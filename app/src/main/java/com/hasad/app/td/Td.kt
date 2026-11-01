package com.hasad.app.td

import android.content.Context
import android.os.Build
import android.util.Log
import com.hasad.app.BuildConfig
import com.hasad.app.data.Prefs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TdException(val code: Int, override val message: String) : Exception(message)

/** حالة تسجيل الدخول المبسّطة للواجهة */
sealed class AuthState {
    data object Loading : AuthState()
    data object NeedKeys : AuthState()
    data object WaitPhone : AuthState()
    data class WaitCode(val phone: String) : AuthState()
    data class WaitPassword(val hint: String) : AuthState()
    data object Ready : AuthState()
    data class Failed(val reason: String) : AuthState()
}

/**
 * غلاف بسيط حول TDLib: يرسل الطلبات كدوال suspend وينشر التحديثات.
 */
object Td {
    private const val TAG = "Td"
    private var client: Client? = null
    private lateinit var app: Context

    private val _auth = MutableStateFlow<AuthState>(AuthState.Loading)
    val auth: StateFlow<AuthState> = _auth

    private val _fileUpdates = MutableSharedFlow<TdApi.File>(extraBufferCapacity = 256)
    val fileUpdates: SharedFlow<TdApi.File> = _fileUpdates

    @Volatile private var nativeLoaded = false

    private fun loadNative(): Boolean {
        if (nativeLoaded) return true
        // نسخة Telegram X الجاهزة تعتمد على OpenSSL و libc++ كمكتبات مستقلة
        for (lib in listOf("c++_shared", "cryptox", "sslx")) {
            try { System.loadLibrary(lib) } catch (t: Throwable) { Log.w(TAG, "optional lib $lib: ${t.message}") }
        }
        return try {
            System.loadLibrary("tdjni"); nativeLoaded = true; true
        } catch (t: Throwable) {
            Log.e(TAG, "tdjni load failed", t)
            _auth.value = AuthState.Failed("تعذّر تحميل مكتبة تيليجرام: ${t.message}")
            false
        }
    }

    fun apiId(): Int = Prefs.apiId(app).takeIf { it != 0 } ?: BuildConfig.TG_API_ID
    fun apiHash(): String = Prefs.apiHash(app).ifBlank { BuildConfig.TG_API_HASH }

    @Synchronized
    fun start(context: Context) {
        app = context.applicationContext
        if (client != null) return
        if (apiId() == 0 || apiHash().isBlank()) { _auth.value = AuthState.NeedKeys; return }
        if (!loadNative()) return
        try { Client.execute(TdApi.SetLogVerbosityLevel(1)) } catch (_: Throwable) {}
        client = Client.create({ onUpdate(it) }, { Log.w(TAG, "update error", it) }, { Log.w(TAG, "error", it) })
    }

    /** يعيد التشغيل بعد إدخال المفاتيح أو بعد الخروج */
    @Synchronized
    fun restart(context: Context) {
        client = null
        start(context)
    }

    private fun onUpdate(obj: TdApi.Object) {
        when (obj) {
            is TdApi.UpdateAuthorizationState -> onAuth(obj.authorizationState)
            is TdApi.UpdateFile -> _fileUpdates.tryEmit(obj.file)
        }
    }

    private fun onAuth(s: TdApi.AuthorizationState) {
        when (s) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val p = TdApi.SetTdlibParameters()
                p.useTestDc = false
                p.databaseDirectory = File(app.filesDir, "td").absolutePath
                p.filesDirectory = File(app.filesDir, "tdfiles").absolutePath
                p.databaseEncryptionKey = ByteArray(0)
                p.useFileDatabase = true
                p.useChatInfoDatabase = true
                p.useMessageDatabase = true
                p.useSecretChats = false
                p.apiId = apiId()
                p.apiHash = apiHash()
                p.systemLanguageCode = "ar"
                p.deviceModel = Build.MANUFACTURER + " " + Build.MODEL
                p.systemVersion = "Android " + Build.VERSION.RELEASE
                p.applicationVersion = BuildConfig.VERSION_NAME
                client?.send(p) { r ->
                    if (r is TdApi.Error) _auth.value = AuthState.Failed(r.message)
                }
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> _auth.value = AuthState.WaitPhone
            is TdApi.AuthorizationStateWaitCode -> _auth.value = AuthState.WaitCode(s.codeInfo?.phoneNumber ?: "")
            is TdApi.AuthorizationStateWaitPassword -> _auth.value = AuthState.WaitPassword(s.passwordHint ?: "")
            is TdApi.AuthorizationStateReady -> _auth.value = AuthState.Ready
            is TdApi.AuthorizationStateLoggingOut, is TdApi.AuthorizationStateClosing -> _auth.value = AuthState.Loading
            is TdApi.AuthorizationStateClosed -> {
                synchronized(this) { client = null }
                start(app)
            }
            else -> _auth.value = AuthState.Failed("هذا النوع من الحسابات يحتاج تسجيل الدخول من تطبيق تيليجرام أولاً")
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : TdApi.Object> send(fn: TdApi.Function<T>): T =
        suspendCancellableCoroutine { cont ->
            val c = client
            if (c == null) {
                cont.resumeWithException(TdException(-1, "تيليجرام غير جاهز"))
                return@suspendCancellableCoroutine
            }
            c.send(fn) { r ->
                if (!cont.isActive) return@send
                if (r is TdApi.Error) cont.resumeWithException(TdException(r.code, r.message))
                else cont.resume(r as T)
            }
        }

    fun isReady() = _auth.value == AuthState.Ready

    suspend fun setPhone(phone: String) {
        val s = TdApi.PhoneNumberAuthenticationSettings()
        s.authenticationTokens = emptyArray()
        val f = TdApi.SetAuthenticationPhoneNumber()
        f.phoneNumber = phone
        f.settings = s
        send(f)
    }

    suspend fun checkCode(code: String) { val f = TdApi.CheckAuthenticationCode(); f.code = code; send(f) }
    suspend fun checkPassword(pw: String) { val f = TdApi.CheckAuthenticationPassword(); f.password = pw; send(f) }
    suspend fun logOut() { send(TdApi.LogOut()) }
}

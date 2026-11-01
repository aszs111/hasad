package com.hasad.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.hasad.app.data.Prefs
import com.hasad.app.td.AuthState
import com.hasad.app.td.Td
import com.hasad.app.ui.AddChannelScreen
import com.hasad.app.ui.C
import com.hasad.app.ui.ChannelScreen
import com.hasad.app.ui.ChannelsScreen
import com.hasad.app.ui.HasadTheme
import com.hasad.app.ui.LibraryScreen
import com.hasad.app.ui.LocalS
import com.hasad.app.ui.LoginScreen
import com.hasad.app.ui.S
import com.hasad.app.ui.SettingsScreen

sealed class Screen {
    data object Channels : Screen()
    data object Library : Screen()
    data object Settings : Screen()
    data class Add(val link: String = "") : Screen()
    data class Channel(val id: Long) : Screen()
}

class MainActivity : ComponentActivity() {

    private val sharedLink = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        handleIntent(intent)
        setContent { Root() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        if (i?.action == Intent.ACTION_SEND) {
            val text = i.getStringExtra(Intent.EXTRA_TEXT) ?: return
            Regex("(https?://)?(t\\.me|telegram\\.me)/\\S+").find(text)?.let { sharedLink.value = it.value }
        }
    }

    @Composable
    private fun Root() {
        var lang by remember { mutableStateOf(Prefs.lang(this)) }
        val s = remember(lang) { S(lang == "ar") }
        val auth by Td.auth.collectAsState()
        HasadTheme {
            CompositionLocalProvider(LocalS provides s, LocalLayoutDirection provides if (s.ar) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                Box(Modifier.fillMaxSize().background(C.bg).safeDrawingPadding()) {
                    if (auth != AuthState.Ready) LoginScreen(auth, onToggleLang = { lang = if (lang == "ar") "en" else "ar"; Prefs.setLang(this@MainActivity, lang) })
                    else Main(onToggleLang = { lang = if (lang == "ar") "en" else "ar"; Prefs.setLang(this@MainActivity, lang) })
                }
            }
        }
    }

    @Composable
    private fun Main(onToggleLang: () -> Unit) {
        val s = LocalS.current
        var stack by remember { mutableStateOf(listOf<Screen>(Screen.Channels)) }
        val shared = sharedLink.value
        androidx.compose.runtime.LaunchedEffect(shared) {
            if (shared != null) { stack = stack + Screen.Add(shared); sharedLink.value = null }
        }
        val cur = stack.last()
        fun go(x: Screen) { stack = stack + x }
        fun tab(x: Screen) { stack = listOf(x) }
        fun back() { if (stack.size > 1) stack = stack.dropLast(1) }
        BackHandler(stack.size > 1) { back() }

        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (cur) {
                    Screen.Channels -> ChannelsScreen(onOpen = { go(Screen.Channel(it)) }, onAdd = { go(Screen.Add()) }, onToggleLang = onToggleLang)
                    Screen.Library -> LibraryScreen()
                    Screen.Settings -> SettingsScreen(onToggleLang = onToggleLang)
                    is Screen.Add -> AddChannelScreen(cur.link, onBack = { back() }, onDone = { id -> stack = listOf(Screen.Channels, Screen.Channel(id)) })
                    is Screen.Channel -> ChannelScreen(cur.id, onBack = { back() })
                }
            }
            if (cur !is Screen.Add) {
                val root = stack.first()
                val colors = NavigationBarItemDefaults.colors(selectedIconColor = C.amber, selectedTextColor = C.amber,
                    indicatorColor = C.amber.copy(alpha = 0.15f), unselectedIconColor = C.sub, unselectedTextColor = C.sub)
                NavigationBar(containerColor = C.surface) {
                    NavigationBarItem(root == Screen.Channels, { tab(Screen.Channels) }, { Icon(Icons.Filled.ViewList, null) }, label = { Text(s.channels) }, colors = colors)
                    NavigationBarItem(root == Screen.Library, { tab(Screen.Library) }, { Icon(Icons.Filled.LocalLibrary, null) }, label = { Text(s.library) }, colors = colors)
                    NavigationBarItem(root == Screen.Settings, { tab(Screen.Settings) }, { Icon(Icons.Filled.Settings, null) }, label = { Text(s.settings) }, colors = colors)
                }
            }
        }
    }
}

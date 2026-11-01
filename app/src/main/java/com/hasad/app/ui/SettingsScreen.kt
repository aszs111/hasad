package com.hasad.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasad.app.BuildConfig
import com.hasad.app.data.Prefs
import com.hasad.app.data.Storage
import com.hasad.app.td.Td
import com.hasad.app.work.Jobs
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

@Composable
fun SettingsScreen(onToggleLang: () -> Unit) {
    val s = LocalS.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var storage by remember { mutableStateOf(Prefs.defaultStorage(ctx)) }
    var wifi by remember { mutableStateOf(Prefs.wifiOnly(ctx)) }
    val me by produceState("") {
        value = runCatching { Td.send(TdApi.GetMe()).let { u -> "${u.firstName} ${u.lastName}".trim() + if (u.phoneNumber.isNullOrBlank()) "" else "  +${u.phoneNumber}" } }.getOrDefault("")
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            storage = uri.toString(); Prefs.setDefaultStorage(ctx, storage)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppHeader(s.settings)
        Column(Modifier.padding(14.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, null, Modifier.size(18.dp), tint = C.sub); Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.account, color = C.sub, fontSize = 11.sp)
                            Text(me.ifBlank { "…" }, color = C.text, fontSize = 14.sp)
                        }
                        TextButton(onClick = { scope.launch { runCatching { Td.logOut() } } }) {
                            Icon(Icons.Filled.Logout, null, Modifier.size(16.dp), tint = C.red); Spacer(Modifier.width(4.dp)); Text(s.logout, color = C.red)
                        }
                    }
                }
            }
            SectionTitle(s.settings)
            Card(Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Language, null, Modifier.size(18.dp), tint = C.sub); Spacer(Modifier.width(10.dp))
                        Text(s.language, color = C.text, modifier = Modifier.weight(1f))
                        TextButton(onClick = onToggleLang) { Text(if (s.ar) "العربية ⇄ English" else "English ⇄ العربية", color = C.amber) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Folder, null, Modifier.size(18.dp), tint = C.sub); Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.defaultFolder, color = C.text)
                            Text(Storage.describe(ctx, storage), color = C.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (storage != null) TextButton(onClick = { storage = null; Prefs.setDefaultStorage(ctx, null) }) { Text(s.reset, color = C.sub) }
                        TextButton(onClick = { picker.launch(null) }) { Text(s.change, color = C.amber) }
                    }
                    ToggleRow(Icons.Filled.Wifi, s.wifiOnly, wifi) { wifi = it; Prefs.setWifiOnly(ctx, it) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Sync, null, Modifier.size(18.dp), tint = C.sub); Spacer(Modifier.width(10.dp))
                        Text(s.syncAll, color = C.text, modifier = Modifier.weight(1f))
                        TextButton(onClick = { Jobs.syncNow(ctx) }) { Text(s.ok, color = C.amber) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Bookshelf(12, 12, Modifier.padding(horizontal = 100.dp), height = 40.dp, maxBooks = 12)
            Spacer(Modifier.height(8.dp))
            Text("${s.appName} ${BuildConfig.VERSION_NAME}", color = C.amber, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(s.about, color = C.sub, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
        }
    }
}

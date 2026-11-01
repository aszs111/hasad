package com.hasad.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasad.app.data.Prefs
import com.hasad.app.td.AuthState
import com.hasad.app.td.Td
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(auth: AuthState, onToggleLang: () -> Unit) {
    val s = LocalS.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var input by remember(auth) { mutableStateOf("") }
    var input2 by remember(auth) { mutableStateOf("") }

    fun act(block: suspend () -> Unit) {
        busy = true; error = null
        scope.launch {
            try { block() } catch (e: Exception) { error = e.message }
            busy = false
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onToggleLang) { Text(if (s.ar) "EN" else "ع", color = C.sub) }
        }
        Spacer(Modifier.height(30.dp))
        Bookshelf(total = 14, done = 14, modifier = Modifier.padding(horizontal = 60.dp), height = 70.dp, maxBooks = 14)
        Spacer(Modifier.height(14.dp))
        Text(s.appName, color = C.amber, fontSize = 40.sp, fontWeight = FontWeight.Black)
        Text(s.tagline, color = C.sub, fontSize = 15.sp)
        Spacer(Modifier.height(36.dp))

        val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.amber, unfocusedBorderColor = C.line, cursorColor = C.amber,
            focusedLabelColor = C.amber)

        when (auth) {
            AuthState.Loading -> CircularProgressIndicator(color = C.amber)
            is AuthState.Failed -> {
                Text(auth.reason, color = C.red, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { Td.restart(ctx) }, colors = ButtonDefaults.buttonColors(C.amber)) { Text(s.retry) }
            }
            AuthState.NeedKeys -> {
                Text(s.keysTitle, color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(s.keysNote, color = C.sub, fontSize = 13.sp, textAlign = TextAlign.Center)
                TextButton(onClick = { openUrl(ctx, "https://my.telegram.org/apps") }) { Text(s.openSite, color = C.amber) }
                OutlinedTextField(input, { input = it.filter { c -> c.isDigit() } }, label = { Text("api_id") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(input2, { input2 = it.trim() }, label = { Text("api_hash") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                Spacer(Modifier.height(16.dp))
                Button(enabled = input.isNotBlank() && input2.length >= 20, onClick = {
                    Prefs.setKeys(ctx, input.toIntOrNull() ?: 0, input2); Td.restart(ctx)
                }, colors = ButtonDefaults.buttonColors(C.amber), modifier = Modifier.fillMaxWidth()) { Text(s.next) }
            }
            else -> {
                Text(s.loginTitle, color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(s.loginNote, color = C.sub, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                val label: String
                val type: KeyboardType
                val action: () -> Unit
                when (auth) {
                    is AuthState.WaitCode -> { label = s.code; type = KeyboardType.Number; action = { act { Td.checkCode(input.trim()) } } }
                    is AuthState.WaitPassword -> {
                        label = s.password + if (auth.hint.isNotBlank()) " (${auth.hint})" else ""
                        type = KeyboardType.Password; action = { act { Td.checkPassword(input) } }
                    }
                    else -> { label = s.phone; type = KeyboardType.Phone; action = { act { Td.setPhone(input.trim().replace(" ", "")) } } }
                }
                OutlinedTextField(
                    input, { input = it }, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = type),
                    visualTransformation = if (auth is AuthState.WaitPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    placeholder = { if (auth == AuthState.WaitPhone) Text("+9665xxxxxxxx", color = C.dim) },
                )
                Spacer(Modifier.height(16.dp))
                Button(enabled = !busy && input.isNotBlank(), onClick = action, colors = ButtonDefaults.buttonColors(C.amber), modifier = Modifier.fillMaxWidth()) {
                    if (busy) CircularProgressIndicator(Modifier.height(18.dp), color = C.bg, strokeWidth = 2.dp) else Text(s.next)
                }
            }
        }
        error?.let { Spacer(Modifier.height(12.dp)); Text(it, color = C.red, textAlign = TextAlign.Center) }
    }
}

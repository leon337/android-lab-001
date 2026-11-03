package br.com.laboratorio.android.live

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

private const val BROKER_URL = "https://android-lab-001-gemini-broker.onrender.com"

@Composable
fun LiveVoicePanel() {
    val context = LocalContext.current
    val client = remember { LiveAudioClient(context.applicationContext) }
    val state by client.state.collectAsStateWithLifecycle()
    val uiScope = rememberCoroutineScope()
    var pairingCode by remember { mutableStateOf("") }
    var pendingPairingCode by remember { mutableStateOf("") }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val code = pendingPairingCode
        pendingPairingCode = ""
        if (granted) {
            uiScope.launch { client.connect(BROKER_URL, code) }
        }
    }

    DisposableEffect(client) {
        onDispose { client.close() }
    }

    HorizontalDivider()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Gemini 3.8 Live · Thinking", style = MaterialTheme.typography.titleMedium)
        Text(
            "Conversa de voz com IA real. O áudio segue diretamente ao Google usando token temporário; a chave permanente nunca entra no APK.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Modelo: gemini-3.8-live-extended-thinking · Raciocínio: LOW",
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            "Servidor de autenticação protegido · HTTPS",
            style = MaterialTheme.typography.bodySmall
        )

        if (state.requiresPairing) {
            OutlinedTextField(
                value = pairingCode,
                onValueChange = { pairingCode = it },
                label = { Text("Código de ativação (uma única vez)") },
                modifier = Modifier.fillMaxWidth().testTag("gemini-pairing-code"),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !state.starting
            )
            Text(
                "Após a ativação, a credencial fica criptografada neste aparelho. O código não será salvo.",
                style = MaterialTheme.typography.labelSmall
            )
        }

        Text(state.status, style = MaterialTheme.typography.bodyMedium)
        if (state.lastMessage.isNotBlank()) {
            Text(state.lastMessage, style = MaterialTheme.typography.bodySmall)
        }

        if (state.connected) {
            OutlinedButton(
                onClick = { client.disconnect() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Encerrar conversa")
            }
        } else if (state.starting) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("Conectando com segurança…")
            }
        } else {
            Button(
                onClick = {
                    val code = if (state.requiresPairing) pairingCode.trim() else ""
                    pairingCode = ""
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        uiScope.launch { client.connect(BROKER_URL, code) }
                    } else {
                        pendingPairingCode = code
                        permission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("gemini-connect"),
                enabled = !state.requiresPairing || pairingCode.trim().length >= 32
            ) {
                Text(if (state.requiresPairing) "Ativar dispositivo e conectar" else "Iniciar conversa ao vivo")
            }
        }

        Text(
            "Protótipo privado: não transmita informações pessoais no nível gratuito sem revisar a política de dados.",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

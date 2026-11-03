package br.com.laboratorio.android.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

@Composable
fun AppUpdatesScreen(
    viewModel: AppUpdateViewModel,
    onBack: () -> Unit,
    onInstall: (File) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected = state.releases.firstOrNull { it.tagName == state.selectedTag }
    val installedCode = state.installedVersion?.versionCode ?: 0L
    val selectedCanInstall = selected != null && selected.versionCode > installedCode
    val preparedFile = state.downloadedApk.takeIf { state.downloadedTag == selected?.tagName }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Voltar") }
            Text(
                "Versões e atualizações",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        HorizontalDivider()

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("Versão instalada", style = MaterialTheme.typography.titleSmall)
                Text(
                    state.installedVersion?.versionName ?: "Não identificada",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Código interno: ${state.installedVersion?.versionCode ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            onClick = { viewModel.checkForUpdates() },
            enabled = !state.isChecking && !state.isDownloading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp),
                    strokeWidth = 2.dp,
                )
                Text("  Consultando versões…")
            } else {
                Text("Verificar versões disponíveis")
            }
        }

        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        state.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        if (state.releases.isNotEmpty()) {
            Text("Escolha uma versão", style = MaterialTheme.typography.titleMedium)
            state.releases.forEach { release ->
                val canInstall = release.versionCode > installedCode
                val selectedRelease = release.tagName == state.selectedTag
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedRelease,
                            enabled = !state.isDownloading,
                            role = Role.RadioButton,
                            onClick = { viewModel.selectRelease(release.tagName) },
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedRelease,
                            onClick = { viewModel.selectRelease(release.tagName) },
                            enabled = !state.isDownloading,
                        )
                        Column(
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                "Gemini Live ${release.versionName}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                release.publishedAt.take(10).ifBlank { release.tagName },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                if (canInstall) "Atualização disponível" else "Versão instalada ou anterior — downgrade bloqueado",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (canInstall) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (release.notes.isNotBlank()) {
                                Text(
                                    release.notes.take(220),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        } else if (!state.isChecking && state.message != null) {
            Text(
                "As versões publicadas aparecerão aqui depois que uma release assinada for publicada no GitHub.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.isDownloading) {
            Text("Baixando e verificando APK…", style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
                progress = { state.downloadProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${state.downloadProgress}%", style = MaterialTheme.typography.labelSmall)
        }

        if (preparedFile != null) {
            Button(
                onClick = { onInstall(preparedFile) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Instalar versão ${selected?.versionName}")
            }
        } else {
            Button(
                onClick = { viewModel.downloadSelectedRelease() },
                enabled = selectedCanInstall && !state.isDownloading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Baixar versão selecionada")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "O aplicativo verifica o pacote, a versão, o certificado de assinatura e o SHA-256 antes de abrir o instalador do Android. A instalação sempre exige confirmação do usuário.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

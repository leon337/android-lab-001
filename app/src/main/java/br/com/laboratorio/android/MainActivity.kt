package br.com.laboratorio.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.laboratorio.android.live.ChatLine
import br.com.laboratorio.android.live.LiveSettings
import br.com.laboratorio.android.live.LiveViewModel
import br.com.laboratorio.android.live.ModelCatalog
import br.com.laboratorio.android.live.Phase

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: LiveViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[LiveViewModel::class.java]

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFB39DDB),
                    secondary = Color(0xFF80CBC4),
                    background = Color(0xFF101116),
                    surface = Color(0xFF191B23),
                    error = Color(0xFFFF8A80),
                ),
            ) {
                GeminiLiveApp(viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && ::viewModel.isInitialized) {
            viewModel.stop()
        }
    }
}

@Composable
private fun GeminiLiveApp(viewModel: LiveViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable {
        mutableStateOf(settings.apiKey.isBlank())
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            if (showSettings) {
                SettingsScreen(
                    initial = settings,
                    canCancel = settings.apiKey.isNotBlank(),
                    onSave = {
                        viewModel.saveSettings(it)
                        showSettings = false
                    },
                    onCancel = { showSettings = false },
                    onClearApiKey = {
                        viewModel.clearApiKey()
                    },
                )
            } else {
                ChatScreen(
                    viewModel = viewModel,
                    onOpenSettings = { showSettings = true },
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    initial: LiveSettings,
    canCancel: Boolean,
    onSave: (LiveSettings) -> Unit,
    onCancel: () -> Unit,
    onClearApiKey: () -> Unit,
) {
    var apiKey by remember(initial.apiKey) { mutableStateOf(initial.apiKey) }
    var showKey by remember { mutableStateOf(false) }
    var model by remember(initial.model) { mutableStateOf(initial.model) }
    var voice by remember(initial.voice) { mutableStateOf(initial.voice) }
    var thinking by remember(initial.thinkingLevel) { mutableStateOf(initial.thinkingLevel) }
    var prompt by remember(initial.systemPrompt) { mutableStateOf(initial.systemPrompt) }
    var speaker by remember(initial.speaker) { mutableStateOf(initial.speaker) }
    var voiceMenu by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
            .testTag("settings-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Gemini Live", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Configuração do assistente de voz",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        Text("Chave de API do Gemini", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it.trim() },
            modifier = Modifier.fillMaxWidth().testTag("api-key-input"),
            singleLine = true,
            label = { Text("API Key") },
            placeholder = { Text("Cole sua chave do Google AI Studio") },
            visualTransformation = if (showKey) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "Ocultar" else "Mostrar")
                }
            },
        )
        Text(
            "Crie ou copie sua chave em aistudio.google.com/apikey. " +
                "Ela é armazenada cifrada pelo Android Keystore e usada para solicitar tokens temporários.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Aviso: nenhum segredo em um aplicativo cliente é inviolável em aparelhos com root ou comprometidos.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )

        HorizontalDivider()
        Text("Modelo de conversação", style = MaterialTheme.typography.titleMedium)

        ModelCatalog.models.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = model == option.id,
                        onClick = { model = option.id },
                        role = Role.RadioButton,
                    )
                    .testTag(
                        if (option.id == ModelCatalog.DEFAULT_MODEL) {
                            "model-live-standard"
                        } else {
                            "model-live-thinking"
                        },
                    )
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = model == option.id, onClick = null)
                Column(Modifier.padding(start = 8.dp)) {
                    Text(option.label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        option.id,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }

        if (ModelCatalog.supportsThinking(model)) {
            Text("Profundidade do raciocínio", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModelCatalog.thinkingLevels.forEach { level ->
                    if (thinking == level) {
                        Button(onClick = { thinking = level }) {
                            Text(level.uppercase())
                        }
                    } else {
                        OutlinedButton(onClick = { thinking = level }) {
                            Text(level.uppercase())
                        }
                    }
                }
            }
        }

        Text("Voz do assistente", style = MaterialTheme.typography.titleSmall)
        Box {
            OutlinedButton(onClick = { voiceMenu = true }) {
                Text(voice)
            }
            DropdownMenu(
                expanded = voiceMenu,
                onDismissRequest = { voiceMenu = false },
            ) {
                ModelCatalog.voices.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            voice = option
                            voiceMenu = false
                        },
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Usar alto-falante")
                Text(
                    "Desative para ouvir pelo fone auricular.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = speaker, onCheckedChange = { speaker = it })
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth().testTag("system-prompt"),
            minLines = 3,
            maxLines = 6,
            label = { Text("Instruções do assistente") },
        )

        Button(
            onClick = {
                onSave(
                    LiveSettings(
                        apiKey = apiKey,
                        model = model,
                        voice = voice,
                        thinkingLevel = thinking,
                        systemPrompt = prompt,
                        speaker = speaker,
                    ),
                )
            },
            enabled = apiKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("settings-save"),
        ) {
            Text("Salvar configurações")
        }

        if (canCancel) {
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar para conversa")
            }
        }

        if (apiKey.isNotBlank()) {
            TextButton(
                onClick = {
                    apiKey = ""
                    onClearApiKey()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Remover chave API")
            }
        }
    }
}

@Composable
private fun ChatScreen(
    viewModel: LiveViewModel,
    onOpenSettings: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val active = ui.phase == Phase.LIVE || ui.phase == Phase.CONNECTING

    DisposableEffect(active) {
        view.keepScreenOn = active
        onDispose { view.keepScreenOn = false }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.start()
        } else {
            viewModel.reportError(
                "Sem permissão de microfone. Libere o acesso em Configurações do Android > Apps > Gemini Live > Permissões.",
            )
        }
    }

    fun startConversation() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            viewModel.start()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(ui.lines.size) {
        if (ui.lines.isNotEmpty()) listState.animateScrollToItem(ui.lines.lastIndex)
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Gemini Live", style = MaterialTheme.typography.headlineSmall)
                Text(
                    ModelCatalog.labelFor(settings.model),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onOpenSettings,
                enabled = !active,
            ) {
                Text("Configurações")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            ui.status,
            style = MaterialTheme.typography.bodyMedium,
            color = if (ui.phase == Phase.LIVE) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        ui.notice?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        ui.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 10.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("chat-screen"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (ui.lines.isEmpty()) {
                item {
                    Text(
                        "Sua conversa aparecerá aqui. Toque em Iniciar conversa e fale naturalmente.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            items(ui.lines) { line ->
                MessageBubble(line)
            }
        }

        Spacer(Modifier.height(12.dp))
        if (active) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { viewModel.stop() },
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Encerrar")
                }
                OutlinedButton(
                    onClick = { viewModel.toggleMute() },
                    enabled = ui.phase == Phase.LIVE,
                    modifier = Modifier.weight(1f).height(54.dp),
                ) {
                    Text(if (ui.muted) "Ativar microfone" else "Silenciar")
                }
            }
        } else {
            Button(
                onClick = { startConversation() },
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("start-live"),
            ) {
                Text("Iniciar conversa")
            }
        }

        TextButton(
            onClick = { viewModel.clearConversation() },
            enabled = ui.lines.isNotEmpty() && !active,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Limpar conversa")
        }
    }
}

@Composable
private fun MessageBubble(line: ChatLine) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (line.fromUser) Alignment.End else Alignment.Start,
    ) {
        Text(
            if (line.fromUser) "Você" else "Gemini",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (line.fromUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 340.dp),
        ) {
            Text(
                line.text.trim(),
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

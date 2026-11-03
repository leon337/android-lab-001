package br.com.laboratorio.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.laboratorio.android.ai.MockAIRepository
import br.com.laboratorio.android.notes.NotesUiState
import br.com.laboratorio.android.notes.NotesViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notesViewModel = ViewModelProvider(
            this,
            NotesViewModel.Factory(MockAIRepository())
        )[NotesViewModel::class.java]

        setContent {
            val estado by notesViewModel.uiState.collectAsStateWithLifecycle()
            MaterialTheme {
                NotesScreen(
                    estado = estado,
                    alterarNota = notesViewModel::alterarNota,
                    gerarResumo = notesViewModel::gerarResumo,
                    aprovarResumo = notesViewModel::aprovarResumo,
                    descartarResumo = notesViewModel::descartarResumo
                )
            }
        }
    }
}

@Composable
private fun NotesScreen(
    estado: NotesUiState,
    alterarNota: (String) -> Unit,
    gerarResumo: () -> Unit,
    aprovarResumo: () -> Unit,
    descartarResumo: () -> Unit
) {
    var interacoes by rememberSaveable { mutableIntStateOf(0) }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Laboratório Android", style = MaterialTheme.typography.headlineMedium)
            Text("Experimento 003 · Anotações com IA",
                style = MaterialTheme.typography.titleMedium)
            Text(
                "SIMULAÇÃO LOCAL — nenhuma IA real conectada",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                "Escreva uma ideia. O simulador devolverá uma sugestão previsível para testar o fluxo de aprovação.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = estado.nota,
                onValueChange = alterarNota,
                label = { Text("Sua anotação") },
                placeholder = { Text("Ex.: Hoje aprendi sobre o Android...") },
                modifier = Modifier.fillMaxWidth().testTag("nota-input"),
                minLines = 3,
                maxLines = 4,
                enabled = !estado.carregando
            )

            Button(
                onClick = gerarResumo,
                modifier = Modifier.fillMaxWidth(),
                enabled = !estado.carregando && estado.nota.isNotBlank()
            ) {
                Text("Gerar resumo (Mock)")
            }

            if (estado.carregando) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp))
                    Text("Processando simulação…")
                }
            }
            estado.erro?.let { mensagem ->
                Text(mensagem, color = MaterialTheme.colorScheme.error)
            }

            estado.resumo?.let { resumo ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Sugestão de resumo", style = MaterialTheme.typography.titleMedium)
                        Text(resumo, modifier = Modifier.testTag("resumo-resultado"))
                        if (estado.aprovado) {
                            Text(
                                "Resumo aprovado",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleSmall
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = aprovarResumo,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Aprovar resumo") }
                                OutlinedButton(
                                    onClick = descartarResumo,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Descartar resumo") }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()
            Text("Teste de regressão · Experimento 001",
                style = MaterialTheme.typography.titleSmall)
            Text("Interações: $interacoes")
            Button(onClick = { interacoes++ }) {
                Text("Testar interação")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

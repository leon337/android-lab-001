package br.com.laboratorio.android.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.laboratorio.android.ai.AIRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val nota: String = "",
    val resumo: String? = null,
    val carregando: Boolean = false,
    val aprovado: Boolean = false,
    val erro: String? = null
)

class NotesViewModel(private val repository: AIRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    fun alterarNota(texto: String) {
        _uiState.update { it.copy(nota = texto, resumo = null, aprovado = false, erro = null) }
    }

    fun gerarResumo() {
        val nota = _uiState.value.nota.trim()
        if (nota.isEmpty()) {
            _uiState.update { it.copy(erro = "Digite uma anotação para começar.") }
            return
        }
        if (_uiState.value.carregando) return
        _uiState.update { it.copy(carregando = true, resumo = null, aprovado = false, erro = null) }
        viewModelScope.launch {
            try {
                val resumo = repository.resumir(nota)
                _uiState.update { it.copy(resumo = resumo, carregando = false) }
            } catch (cancelamento: CancellationException) {
                throw cancelamento
            } catch (erro: Exception) {
                _uiState.update { it.copy(erro = "Não foi possível gerar o resumo.", carregando = false) }
            }
        }
    }

    fun aprovarResumo() {
        _uiState.update { estado ->
            if (estado.resumo == null || estado.carregando) estado else estado.copy(aprovado = true)
        }
    }

    fun descartarResumo() {
        _uiState.update { it.copy(resumo = null, aprovado = false, erro = null) }
    }

    class Factory(private val repository: AIRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NotesViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(repository) as T
        }
    }
}

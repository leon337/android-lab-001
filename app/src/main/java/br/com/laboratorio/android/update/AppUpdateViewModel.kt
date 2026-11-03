package br.com.laboratorio.android.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppUpdateUiState(
    val installedVersion: InstalledAppVersion? = null,
    val isChecking: Boolean = false,
    val releases: List<AvailableRelease> = emptyList(),
    val selectedTag: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val downloadedTag: String? = null,
    val downloadedApk: File? = null,
    val message: String? = null,
    val error: String? = null,
)

class AppUpdateViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppUpdateRepository(app.applicationContext)

    private val _state = MutableStateFlow(
        AppUpdateUiState(installedVersion = repository.installedVersion()),
    )
    val state: StateFlow<AppUpdateUiState> = _state.asStateFlow()

    fun checkForUpdates() {
        if (_state.value.isChecking) return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isChecking = true,
                error = null,
                message = null,
                downloadedApk = null,
                downloadedTag = null,
            )
            try {
                val releases = withContext(Dispatchers.IO) { repository.fetchReleases() }
                val currentCode = _state.value.installedVersion?.versionCode ?: 0L
                val firstInstallable = releases.firstOrNull { it.versionCode > currentCode }
                _state.value = _state.value.copy(
                    isChecking = false,
                    releases = releases,
                    selectedTag = firstInstallable?.tagName ?: releases.firstOrNull()?.tagName,
                    message = if (releases.isEmpty()) {
                        "Nenhuma versão publicada com APK e manifesto válidos."
                    } else {
                        "Catálogo atualizado. Selecione uma versão disponível."
                    },
                )
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    isChecking = false,
                    error = error.message ?: "Não foi possível consultar as versões.",
                )
            }
        }
    }

    fun selectRelease(tagName: String) {
        _state.value = _state.value.copy(
            selectedTag = tagName,
            downloadedApk = null,
            downloadedTag = null,
            downloadProgress = 0,
            error = null,
            message = null,
        )
    }

    fun downloadSelectedRelease() {
        val current = _state.value
        if (current.isDownloading) return
        val release = current.releases.firstOrNull { it.tagName == current.selectedTag }
            ?: run {
                _state.value = current.copy(error = "Selecione uma versão primeiro.")
                return
            }
        val installedCode = current.installedVersion?.versionCode ?: 0L
        if (release.versionCode <= installedCode) {
            _state.value = current.copy(
                error = "Esta versão já está instalada ou é anterior. Downgrade não está habilitado nesta fase.",
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isDownloading = true,
                downloadProgress = 0,
                downloadedApk = null,
                downloadedTag = null,
                error = null,
                message = "Validando manifesto, assinatura e integridade do APK…",
            )
            try {
                val apk = withContext(Dispatchers.IO) {
                    repository.downloadAndVerify(release) { progress ->
                        _state.value = _state.value.copy(downloadProgress = progress)
                    }
                }
                _state.value = _state.value.copy(
                    isDownloading = false,
                    downloadProgress = 100,
                    downloadedApk = apk,
                    downloadedTag = release.tagName,
                    message = "APK validado. A instalação será confirmada pelo Android.",
                )
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    isDownloading = false,
                    downloadedApk = null,
                    downloadedTag = null,
                    error = error.message ?: "Não foi possível preparar esta versão.",
                    message = null,
                )
            }
        }
    }
}

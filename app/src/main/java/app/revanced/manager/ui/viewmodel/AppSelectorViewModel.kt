package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import app.revanced.manager.R
import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.PM
import app.revanced.manager.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files

@OptIn(SavedStateHandleSaveableApi::class)
class AppSelectorViewModel(
    private val app: Application,
    private val pm: PM,
    fs: Filesystem,
    private val patchBundleRepository: PatchBundleRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val inputFile = savedStateHandle.saveable(key = "inputFile") {
        File(
            fs.uiTempDir,
            "input.apk"
        ).also(File::delete)
    }

    val filterTextFlow = MutableStateFlow("")
    val filterText: StateFlow<String> = filterTextFlow

    val apps = pm.appList.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null,
    )

    val filteredApps = filterText
        .combine(apps) { filter, apps ->
            if (apps == null || filter.isBlank()) {
                apps
            } else {
                apps.filter { app ->
                    app.packageName.contains(filter, true) ||
                            loadLabel(app.packageInfo).contains(filter)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 0,
                replayExpirationMillis = 10_000,
            ),
            initialValue = null,
        )

    private val storageSelectionChannel = Channel<SelectedApp.Local>()
    val storageSelectionFlow = storageSelectionChannel.receiveAsFlow()

    val suggestedAppVersions = patchBundleRepository.suggestedVersions.flowOn(Dispatchers.Default)

    var nonSuggestedVersionDialogSubject by mutableStateOf<SelectedApp.Local?>(null)
        private set

    fun setFilterText(filter: String) {
        filterTextFlow.value = filter
    }

    fun loadLabel(app: PackageInfo?) = with(pm) { app?.label() ?: "Not installed" }

    fun dismissNonSuggestedVersionDialog() {
        nonSuggestedVersionDialogSubject = null
    }

    fun handleStorageResult(uri: Uri) = viewModelScope.launch {
        val selectedApp = withContext(Dispatchers.IO) {
            loadSelectedFile(uri)
        }

        if (selectedApp == null) {
            app.toast(app.getString(R.string.failed_to_load_apk))
            return@launch
        }

        if (patchBundleRepository.isVersionAllowed(selectedApp.packageName, selectedApp.version)) {
            storageSelectionChannel.send(selectedApp)
        } else {
            nonSuggestedVersionDialogSubject = selectedApp
        }
    }

    private fun loadSelectedFile(uri: Uri) =
        app.contentResolver.openInputStream(uri)?.use { stream ->
            with(inputFile) {
                delete()
                Files.copy(stream, toPath())

                pm.getPackageInfo(this)?.let { packageInfo ->
                    SelectedApp.Local(
                        packageName = packageInfo.packageName,
                        version = packageInfo.versionName!!,
                        file = this,
                        temporary = true
                    )
                }
            }
        }
}
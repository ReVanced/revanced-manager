package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.DownloaderPluginRepository
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.network.downloader.DownloaderPluginState
import app.revanced.manager.ui.model.SelectedSource
import app.revanced.manager.ui.model.navigation.SelectedAppInfo
import app.revanced.manager.util.PM
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File

class SourceSelectorViewModel(
    val input: SelectedAppInfo.SourceSelector.ViewModelParams
) : ViewModel(), KoinComponent {
    private val app: Application = get()
    private val downloadedAppRepository: DownloadedAppRepository = get()
    private val pluginRepository: DownloaderPluginRepository = get()
    private val installedAppRepository: InstalledAppRepository = get()
    private val pm: PM = get()

    var selectedSource by mutableStateOf(input.selectedSource)
        private set

    fun selectSource(source: SelectedSource) {
        selectedSource = source
    }

    var localApp by mutableStateOf<SourceOption?>(null)
        private set

    val downloadedApps = downloadedAppRepository.get(input.packageName)
        .map { apps ->
            apps.sortedByDescending { app -> app.version }
                .map {
                    SourceOption(
                        source = SelectedSource.Downloaded(
                            path = downloadedAppRepository.getApkFileForApp(it).path,
                            version = it.version
                        ),
                        title = it.version,
                        category = "Downloaded",
                        key = it.version,
                        disableReason = if (input.version != null && it.version != input.version) {
                            DisableReason.VERSION_NOT_MATCHING
                        } else null
                    )
                }
        }

    val plugins = pluginRepository.pluginStates.map { plugins ->
        plugins.toList().sortedByDescending { it.second is DownloaderPluginState.Loaded }
            .map {
                val packageInfo = pm.getPackageInfo(it.first)
                val label = packageInfo?.applicationInfo?.loadLabel(app.packageManager)
                    ?.toString()

                SourceOption(
                    source = SelectedSource.Plugin(it.first),
                    title = label ?: it.first,
                    category = "Plugin",
                    key = it.first,
                    disableReason = when (it.second) {
                        is DownloaderPluginState.Loaded -> null
                        is DownloaderPluginState.Untrusted -> DisableReason.NOT_TRUSTED
                        is DownloaderPluginState.Failed -> DisableReason.FAILED_TO_LOAD
                    }
                )
            }
    }

    fun getPackageInfo(packageName: String) = pm.getPackageInfo(packageName)

    var installedSource by mutableStateOf<SourceOption?>(null)
        private set

    init {
        viewModelScope.launch {
            val packageInfo = pm.getPackageInfo(input.packageName) ?: return@launch

            val installedApp = installedAppRepository.get(input.packageName)

            installedSource = SourceOption(
                source = SelectedSource.Installed,
                title = packageInfo.versionName.toString(),
                category = "Installed",
                key = input.packageName,
                disableReason = when {
                    installedApp != null -> DisableReason.ALREADY_PATCHED
                    input.version != null && packageInfo.versionName != input.version -> DisableReason.VERSION_NOT_MATCHING
                    else -> null
                }
            )
        }
        input.localPath?.let { local ->
            viewModelScope.launch {
                val packageInfo = pm.getPackageInfo(File(local))
                    ?: return@launch

                localApp = SourceOption(
                    source = SelectedSource.Local(local),
                    title = packageInfo.versionName.toString(),
                    category = "Local",
                    key = "local",
                    disableReason = if (input.version != null && packageInfo.versionName != input.version) {
                        DisableReason.VERSION_NOT_MATCHING
                    } else null
                )
            }
        }
    }

    enum class DisableReason(val message: String) {
        VERSION_NOT_MATCHING("Does not match the selected version"),
        ALREADY_PATCHED("Already patched"),
        NOT_TRUSTED("Not trusted"),
        FAILED_TO_LOAD("Failed to load"),
    }

    data class SourceOption(
        val source: SelectedSource,
        val title: String,
        val category: String? = null,
        val key: String,
        val disableReason: DisableReason? = null
    )
}
package app.revanced.manager.ui.viewmodel

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.network.downloader.DownloaderPackageState
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
    private val downloadedAppRepository: DownloadedAppRepository = get()
    private val downloaderRepository: DownloaderRepository = get()
    private val installedAppRepository: InstalledAppRepository = get()
    private val rootInstaller: RootInstaller = get()
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
                        key = it.version,
                        disableReason = if (input.version != null && it.version != input.version) {
                            DisableReason.VERSION_NOT_MATCHING
                        } else null
                    )
                }
        }

    val downloaderSections = downloaderRepository.downloaderPackageStates.map { packageStates ->
        packageStates.toList().sortedByDescending { it.second is DownloaderPackageState.Loaded }
            .mapNotNull { (packageName, state) ->
                (state as? DownloaderPackageState.Loaded)
                    ?.takeIf { it.downloaders.isNotEmpty() }
                    ?.let {
                        DownloaderSection(
                            title = it.name.ifBlank { packageName },
                            key = packageName,
                            options = it.downloaders.map { downloader ->
                                SourceOption(
                                    source = SelectedSource.Downloader(packageName, downloader.className),
                                    title = downloader.name,
                                    key = "${packageName}:${downloader.className}",
                                )
                            }
                        )
                    }
            }
    }

    val unavailableDownloaders = downloaderRepository.downloaderPackageStates.map { packageStates ->
        packageStates.toList()
            .sortedBy { it.first }
            .mapNotNull { (packageName, state) ->
                val disableReason = when (state) {
                    DownloaderPackageState.Untrusted -> DisableReason.NOT_TRUSTED
                    is DownloaderPackageState.Failed -> DisableReason.FAILED_TO_LOAD
                    is DownloaderPackageState.Loaded -> null
                } ?: return@mapNotNull null

                SourceOption(
                    source = SelectedSource.Downloader(packageName),
                    title = with(pm) { pm.getPackageInfo(packageName)?.label() ?: packageName },
                    key = "unavailable_$packageName",
                    disableReason = disableReason
                )
            }
    }

    var installedSource by mutableStateOf<SourceOption?>(null)
        private set

    init {
        viewModelScope.launch {
            val packageInfo = pm.getPackageInfo(input.packageName) ?: return@launch

            val installedApp = installedAppRepository.get(input.packageName)

            installedSource = SourceOption(
                source = SelectedSource.Installed,
                title = packageInfo.versionName.toString(),
                key = input.packageName,
                disableReason = when {
                    installedApp?.installType == InstallType.DEFAULT -> DisableReason.ALREADY_PATCHED
                    installedApp?.installType == InstallType.MOUNT && !rootInstaller.hasRootAccess() ->
                        DisableReason.NO_ROOT
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
                    key = "local",
                    disableReason = if (input.version != null && packageInfo.versionName != input.version) {
                        DisableReason.VERSION_NOT_MATCHING
                    } else null
                )
            }
        }
    }

    enum class DisableReason(@param:StringRes val message: Int) {
        VERSION_NOT_MATCHING(R.string.source_selector_disable_reason_version_not_matching),
        ALREADY_PATCHED(R.string.already_patched),
        NO_ROOT(R.string.app_source_dialog_option_installed_no_root),
        NOT_TRUSTED(R.string.source_selector_disable_reason_not_trusted),
        FAILED_TO_LOAD(R.string.source_selector_disable_reason_failed_to_load),
    }

    data class SourceOption(
        val source: SelectedSource,
        val title: String,
        val key: String,
        val disableReason: DisableReason? = null
    )

    data class DownloaderSection(
        val title: String,
        val key: String,
        val options: List<SourceOption>,
    )
}

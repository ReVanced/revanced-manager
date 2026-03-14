package app.revanced.manager.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.AnnouncementRepository
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.domain.repository.ManagerUpdateRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.network.dto.ReVancedAnnouncement
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.util.PM
import app.revanced.manager.util.uiSafe
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel(
    private val app: Application,
    private val patchBundleRepository: PatchBundleRepository,
    private val downloaderRepository: DownloaderRepository,
    private val announcementRepository: AnnouncementRepository,
    private val managerUpdateRepository: ManagerUpdateRepository,
    private val networkInfo: NetworkInfo,
    val prefs: PreferencesManager,
    private val pm: PM,
) : ViewModel() {
    val availablePatches =
        patchBundleRepository.bundleInfoFlow.map { it.values.sumOf { bundle -> bundle.patches.size } }
    val bundleDownloadError = patchBundleRepository.updateError
    private val contentResolver: ContentResolver = app.contentResolver
    private val powerManager = app.getSystemService<PowerManager>()!!

    val newDownloadersAvailable =
        downloaderRepository.newDownloaderPackageNames.map { it.isNotEmpty() }
    val availableManagerUpdate = managerUpdateRepository.availableVersion

    /**
     * Android 11 kills the app process after granting the "install apps" permission, which is a problem for the patcher screen.
     * This value is true when the conditions that trigger the bug are met.
     *
     * See: https://github.com/ReVanced/revanced-manager/issues/2138
     */
    val android11BugActive get() = Build.VERSION.SDK_INT == Build.VERSION_CODES.R && !pm.canInstallPackages()

    var showBatteryOptimizationsWarning by mutableStateOf(false)
        private set

    var unreadAnnouncement by mutableStateOf<ReVancedAnnouncement?>(null)
        private set

    var availableDownloaderUpdate by mutableStateOf<ReVancedAsset?>(null)
        private set

    var downloaderUpdateState by mutableStateOf(DownloaderUpdateState.IDLE)
        private set

    var downloaderUpdateProgress by mutableStateOf(0f)
        private set

    private val bundleListEventsChannel = Channel<BundleListViewModel.Event>()
    val bundleListEventsFlow = bundleListEventsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            checkForManagerUpdates()
            checkForAnnouncements()
            checkForDownloaderUpdate()
            updateBatteryOptimizationsWarning()
        }
    }

    fun ignoreNewDownloaders() = viewModelScope.launch {
        downloaderRepository.acknowledgeAll()
    }

    private suspend fun checkForManagerUpdates() {
        if (!prefs.managerAutoUpdates.get() || !networkInfo.isConnected()) return

        uiSafe(app, R.string.failed_to_check_updates, "Failed to check for updates") {
            managerUpdateRepository.refreshAvailableVersion()
        }
    }

    private suspend fun checkForAnnouncements() {
        uiSafe(app, R.string.failed_to_check_updates, "Failed to check for announcements") {
            val announcements = withContext(Dispatchers.IO) {
                announcementRepository.getAnnouncements()
            } ?: throw IllegalStateException("Announcements could not be retrieved")

            val readAnnouncements = prefs.readAnnouncements.get()

            unreadAnnouncement = announcements.firstOrNull { announcement ->
                val hasRelevantTag = announcement.tags.any {
                    it == "✨ ReVanced" || it == "💊 Manager"
                }
                val isUnread = announcement.id !in readAnnouncements

                !announcement.isArchived && hasRelevantTag && isUnread
            }
        }
    }

    fun markUnreadAnnouncementRead() {
        viewModelScope.launch {
            unreadAnnouncement?.let {
                prefs.edit {
                    prefs.readAnnouncements += it.id
                }
            }
            unreadAnnouncement = null
        }
    }

    private suspend fun checkForDownloaderUpdate() {
        if (!prefs.downloaderAutoUpdates.get() || !networkInfo.isConnected()) return
        if (downloaderRepository.getInstalledApiDownloader() == null) return

        uiSafe(app, R.string.failed_to_check_updates, "Failed to check for downloader updates") {
            val asset = downloaderRepository.checkApiDownloaderUpdate()
            if (asset != null) {
                availableDownloaderUpdate = asset
            }
        }
    }

    fun dismissDownloaderUpdate() {
        availableDownloaderUpdate = null
        downloaderUpdateState = DownloaderUpdateState.IDLE
    }

    fun downloadAndInstallDownloaderUpdate() {
        val asset = availableDownloaderUpdate ?: return
        viewModelScope.launch {
            downloaderUpdateState = DownloaderUpdateState.DOWNLOADING
            downloaderUpdateProgress = 0f

            when (
                downloaderRepository.installApiDownloaderAsset(
                    asset = asset,
                    onProgress = { downloaded, total ->
                        downloaderUpdateProgress = if (total != null && total > 0) {
                            downloaded.toFloat() / total.toFloat()
                        } else 0f
                    },
                    onInstalling = { installing ->
                        if (installing) downloaderUpdateState = DownloaderUpdateState.INSTALLING
                    }
                )
            ) {
                is DownloaderRepository.ApiDownloaderActionResult.Success -> {
                    downloaderUpdateState = DownloaderUpdateState.INSTALLED
                    availableDownloaderUpdate = null
                }

                DownloaderRepository.ApiDownloaderActionResult.Aborted -> {
                    downloaderUpdateState = DownloaderUpdateState.IDLE
                }

                else -> {
                    downloaderUpdateState = DownloaderUpdateState.FAILED
                }
            }
        }
    }

    fun updateBatteryOptimizationsWarning() {
        showBatteryOptimizationsWarning =
            !powerManager.isIgnoringBatteryOptimizations(app.packageName)
    }

    fun setShowManagerUpdateDialogOnLaunch(value: Boolean) {
        viewModelScope.launch {
            prefs.showManagerUpdateDialogOnLaunch.update(value)
        }
    }

    private fun sendEvent(event: BundleListViewModel.Event) {
        viewModelScope.launch { bundleListEventsChannel.send(event) }
    }

    fun cancelSourceSelection() = sendEvent(BundleListViewModel.Event.CANCEL)
    fun updateSources() = sendEvent(BundleListViewModel.Event.UPDATE_SELECTED)
    fun deleteSources() = sendEvent(BundleListViewModel.Event.DELETE_SELECTED)

    fun deleteSource(uid: Int) = viewModelScope.launch {
        val source = patchBundleRepository.sources.first().firstOrNull { it.uid == uid } ?: return@launch
        patchBundleRepository.remove(source)
    }

    @SuppressLint("Recycle")
    fun createLocalSource(patchBundle: Uri) = viewModelScope.launch {
        patchBundleRepository.createLocal { contentResolver.openInputStream(patchBundle)!! }
    }

    fun createRemoteSource(apiUrl: String, autoUpdate: Boolean) = viewModelScope.launch {
        patchBundleRepository.createRemote(apiUrl, autoUpdate)
    }
}

enum class DownloaderUpdateState {
    IDLE,
    DOWNLOADING,
    INSTALLING,
    INSTALLED,
    FAILED
}
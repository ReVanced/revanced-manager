package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.repository.ChangelogSource
import app.revanced.manager.domain.repository.ChangelogsRepository
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.ui.component.ChangelogUiState
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.await
import ru.solrudev.ackpine.session.parameters.Confirmation

class UpdateViewModel(
    private val changelogsRepository: ChangelogsRepository,
    private val downloadOnScreenEntry: Boolean
) : ViewModel(), KoinComponent {
    private val app: Application by inject()
    private val reVancedAPI: ReVancedAPI by inject()
    private val http: HttpService by inject()
    private val networkInfo: NetworkInfo by inject()
    private val fs: Filesystem by inject()
    private val ackpineInstaller: PackageInstaller = get()

    // TODO: save state to handle process death.
    var downloadedSize by mutableLongStateOf(0L)
        private set
    var totalSize by mutableLongStateOf(0L)
        private set
    val downloadProgress by derivedStateOf {
        if (downloadedSize == 0L || totalSize == 0L) return@derivedStateOf 0f

        downloadedSize.toFloat() / totalSize.toFloat()
    }
    var showInternetCheckDialog by mutableStateOf(false)
    var state by mutableStateOf(State.CAN_DOWNLOAD)
        private set

    var installError by mutableStateOf("")

    var releaseInfo: ReVancedAsset? by mutableStateOf(null)
        private set

    var changelogsState: ChangelogUiState by mutableStateOf(ChangelogUiState.Loading)
    private val changelogsPageSize = 2

    private val location = fs.tempDir.resolve("updater.apk")

    init {
        viewModelScope.launch {
            uiSafe(app, R.string.download_manager_failed, "Failed to download ReVanced Manager") {
                releaseInfo = reVancedAPI.getAppUpdate()
                    ?: throw Exception("No update available")

                val result = changelogsRepository.loadInitial(
                    ChangelogSource.Manager,
                    changelogsPageSize
                )

                changelogsState = ChangelogUiState.Success(
                    changelogs = result.items,
                    hasMore = result.hasMore
                )

                if (downloadOnScreenEntry) {
                    downloadUpdate()
                } else {
                    state = State.CAN_DOWNLOAD
                }
            }

            if (changelogsState is ChangelogUiState.Loading) {
                changelogsState = ChangelogUiState.Error(
                    app.getString(R.string.changelog_download_fail)
                )
            }
        }
    }
    fun loadNextPage() {
        val current = changelogsState as? ChangelogUiState.Success ?: return
        if (current.isLoadingMore || !current.hasMore) return

        changelogsState = current.copy(isLoadingMore = true)

        val result = changelogsRepository.loadNext(changelogsPageSize)

        changelogsState = current.copy(
            changelogs = current.changelogs + result.items,
            isLoadingMore = false,
            hasMore = result.hasMore
        )
    }

    fun downloadUpdate(ignoreInternetCheck: Boolean = false) = viewModelScope.launch {
        uiSafe(app, R.string.failed_to_download_update, "Failed to download update") {
            val release = releaseInfo!!
            withContext(Dispatchers.IO) {
                if (!networkInfo.isSafe(false) && !ignoreInternetCheck) {
                    showInternetCheckDialog = true
                } else {
                    state = State.DOWNLOADING

                    http.download(location) {
                        url(release.downloadUrl)
                        onDownload { bytesSentTotal, contentLength ->
                            withContext(Dispatchers.Main) {
                                downloadedSize = bytesSentTotal
                                contentLength?.let { totalSize = it }
                            }
                        }
                    }
                    installUpdate()
                }
            }
        }
    }

    fun installUpdate() = viewModelScope.launch {
        state = State.INSTALLING
        val result = withContext(Dispatchers.IO) {
            ackpineInstaller.createSession(Uri.fromFile(location)) {
                confirmation = Confirmation.IMMEDIATE
            }.await()
        }

        when (result) {
            is Session.State.Failed<InstallFailure> -> when (val failure = result.failure) {
                is InstallFailure.Aborted -> state = State.CAN_INSTALL
                else -> {
                    val msg = failure.message.orEmpty()
                    app.toast(app.getString(R.string.install_app_fail, msg))
                    installError = msg
                    state = State.FAILED
                }
            }

            Session.State.Succeeded -> {
                app.toast(app.getString(R.string.install_app_success))
                state = State.SUCCESS
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        location.delete()
    }

    enum class State(@param:StringRes val title: Int) {
        CAN_DOWNLOAD(R.string.update_available),
        DOWNLOADING(R.string.downloading_manager_update),
        CAN_INSTALL(R.string.ready_to_install_update),
        INSTALLING(R.string.installing_manager_update),
        FAILED(R.string.install_update_manager_failed),
        SUCCESS(R.string.update_completed)
    }
}

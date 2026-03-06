package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.asRemoteOrNull
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.network.downloader.DownloaderPackageState
import app.revanced.manager.util.PM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.security.MessageDigest

data class OnboardingDownloadersInfo(
    val packageName: String,
    val name: String,
    val version: String,
    val isTrusted: Boolean
)

enum class ApiDownloaderState {
    CHECKING,
    AVAILABLE,
    DOWNLOADING,
    INSTALLING,
    UP_TO_DATE,
    FAILED,
    UNAVAILABLE
}

enum class OnboardingStep {
    Permissions,
    Updates,
    Sources,
    Apps
}

class OnboardingViewModel(
    private val app: Application,
    private val prefs: PreferencesManager,
    private val pm: PM,
    private val downloaderRepository: DownloaderRepository,
    private val patchBundleRepository: PatchBundleRepository,
) : ViewModel() {
    private val powerManager = app.getSystemService<PowerManager>()!!

    val apps = pm.appList.map { apps ->
        apps.filter { (it.patches ?: 0) > 0 }.ifEmpty { null }
    }

    val suggestedVersions = patchBundleRepository.suggestedVersions

    val downloaders = downloaderRepository.downloaderPackageStates.map { states ->
        states.mapNotNull { (packageName, state) ->
            val packageInfo = pm.getPackageInfo(packageName) ?: return@mapNotNull null
            OnboardingDownloadersInfo(
                packageName = packageName,
                name = with(pm) { packageInfo.label() },
                version = packageInfo.versionName.orEmpty(),
                isTrusted = state is DownloaderPackageState.Loaded
            )
        }.sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.Default)

    val apiDownloaderPackageName = downloaderRepository.apiDownloaderPackageName
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val apiDownloaderLabel = apiDownloaderPackageName
        .map { packageName -> packageName?.let { pm.getPackageInfo(it) }?.let { packageInfo -> with(pm) { packageInfo.label() } } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val apiDownloaderSignature = apiDownloaderPackageName
        .map { packageName ->
            packageName?.let {
                try {
                    val signature = pm.getSignature(packageName)
                    val hash = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                    hash.toHexString(format = HexFormat.UpperCase)
                } catch (_: Exception) {
                    null
                }
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var hasDownloaders by mutableStateOf(false)

    var apiDownloaderState by mutableStateOf(ApiDownloaderState.CHECKING)
        private set

    var apiDownloaderProgress by mutableFloatStateOf(0f)
        private set

    var apiDownloaderIsUpdate by mutableStateOf(false)
        private set

    var apiDownloaderIsTrusted by mutableStateOf(false)
        private set

    var canInstallUnknownApps by mutableStateOf(false)
        private set
    var isNotificationsEnabled by mutableStateOf(false)
        private set
    var isBatteryOptimizationExempt by mutableStateOf(false)
        private set

    var currentStep by mutableStateOf(OnboardingStep.Permissions)
        private set

    val allPermissionsGranted
        get() = canInstallUnknownApps && isNotificationsEnabled && isBatteryOptimizationExempt

    private var pendingAsset: app.revanced.manager.network.dto.ReVancedAsset? = null
    private var apiDownloaderInstallJob: Job? = null

    init {
        refreshPermissionStates()
        viewModelScope.launch {
            downloaders.collect { hasDownloaders = it.isNotEmpty() }
        }
        viewModelScope.launch {
            combine(
                downloaderRepository.downloaderPackageStates,
                apiDownloaderPackageName
            ) { states, apiPkg ->
                apiPkg != null && states[apiPkg] is DownloaderPackageState.Loaded
            }.collect { apiDownloaderIsTrusted = it }
        }
        currentStep =
            if (allPermissionsGranted) OnboardingStep.Updates else OnboardingStep.Permissions

        viewModelScope.launch {
            checkApiDownloader()
        }
    }

    private suspend fun checkApiDownloader() {
        apiDownloaderState = ApiDownloaderState.CHECKING

        val asset = downloaderRepository.checkApiDownloaderUpdate()
        if (asset == null) {
            val installed = downloaderRepository.getInstalledApiDownloader()
            apiDownloaderState = if (installed != null) {
                ApiDownloaderState.UP_TO_DATE
            } else {
                val apiAsset = downloaderRepository.getApiDownloaderAsset()
                if (apiAsset == null) ApiDownloaderState.UNAVAILABLE else {
                    pendingAsset = apiAsset
                    ApiDownloaderState.AVAILABLE
                }
            }

            if (currentStep == OnboardingStep.Sources && apiDownloaderState == ApiDownloaderState.AVAILABLE) {
                startApiDownloaderInstall()
            }
            return
        }

        apiDownloaderIsUpdate = downloaderRepository.getInstalledApiDownloader() != null
        pendingAsset = asset
        apiDownloaderState = ApiDownloaderState.AVAILABLE

        if (currentStep == OnboardingStep.Sources) {
            startApiDownloaderInstall()
        }
    }

    private suspend fun installApiDownloader() {
        val asset = pendingAsset ?: return

        apiDownloaderState = ApiDownloaderState.DOWNLOADING
        when (
            downloaderRepository.installApiDownloaderAsset(
                asset = asset,
                onProgress = { downloaded, total ->
                    apiDownloaderProgress = if (total != null && total > 0) {
                        downloaded.toFloat() / total.toFloat()
                    } else 0f
                },
                onInstalling = { installing ->
                    if (installing) apiDownloaderState = ApiDownloaderState.INSTALLING
                }
            )
        ) {
            is DownloaderRepository.ApiDownloaderActionResult.Success -> {
                pendingAsset = null
                kotlinx.coroutines.delay(500)
                apiDownloaderState = ApiDownloaderState.UP_TO_DATE
            }

            DownloaderRepository.ApiDownloaderActionResult.Aborted -> {
                apiDownloaderState = ApiDownloaderState.AVAILABLE
            }

            else -> {
                apiDownloaderState = ApiDownloaderState.FAILED
            }
        }
    }

    fun startApiDownloaderInstall() {
        if (apiDownloaderInstallJob?.isActive == true) return

        apiDownloaderInstallJob = viewModelScope.launch {
            try {
                apiDownloaderProgress = 0f
                installApiDownloader()
            } finally {
                apiDownloaderInstallJob = null
            }
        }
    }

    fun retryApiDownloaderDownload() {
        startApiDownloaderInstall()
    }

    fun refreshPermissionStates() {
        canInstallUnknownApps = pm.canInstallPackages()
        isNotificationsEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                NotificationManagerCompat.from(app).areNotificationsEnabled()
        isBatteryOptimizationExempt = powerManager.isIgnoringBatteryOptimizations(app.packageName)
    }

    fun advance() {
        currentStep = nextStep(currentStep)
        if (currentStep == OnboardingStep.Sources && apiDownloaderState == ApiDownloaderState.AVAILABLE) {
            startApiDownloaderInstall()
        }
    }

    fun retreat() {
        currentStep = previousStep(currentStep)
        if (currentStep == OnboardingStep.Sources && apiDownloaderState == ApiDownloaderState.AVAILABLE) {
            startApiDownloaderInstall()
        }
    }

    fun trustDownloader(packageName: String) = viewModelScope.launch {
        downloaderRepository.trustPackage(packageName)
    }

    fun revokeDownloaderTrust(packageName: String) = viewModelScope.launch {
        downloaderRepository.revokeTrustForPackage(packageName)
    }

    suspend fun applyAutoUpdatePrefs(managerEnabled: Boolean, patchesEnabled: Boolean) {
        prefs.managerAutoUpdates.update(managerEnabled)

        with(patchBundleRepository) {
            sources
                .first()
                .find { it.uid == 0 }
                ?.asRemoteOrNull
                ?.setAutoUpdate(patchesEnabled)

            if (patchesEnabled) updateCheck()
        }
    }

    suspend fun completeOnboarding() {
        prefs.completedOnboarding.update(true)
    }

    private fun nextStep(from: OnboardingStep) = when (from) {
        OnboardingStep.Permissions -> OnboardingStep.Updates
        OnboardingStep.Updates -> if (hasDownloaders || apiDownloaderState != ApiDownloaderState.UNAVAILABLE) OnboardingStep.Sources else OnboardingStep.Apps
        OnboardingStep.Sources -> OnboardingStep.Apps
        OnboardingStep.Apps -> OnboardingStep.Apps
    }

    private fun previousStep(from: OnboardingStep) = when (from) {
        OnboardingStep.Permissions -> OnboardingStep.Permissions
        OnboardingStep.Updates -> OnboardingStep.Permissions
        OnboardingStep.Sources -> OnboardingStep.Updates
        OnboardingStep.Apps -> if (hasDownloaders || apiDownloaderState != ApiDownloaderState.UNAVAILABLE) OnboardingStep.Sources else OnboardingStep.Updates
    }

}

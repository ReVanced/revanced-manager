package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.asRemoteOrNull
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloaderPluginRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.network.downloader.DownloaderPluginState
import app.revanced.manager.util.PM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OnboardingPluginInfo(
    val packageName: String,
    val name: String,
    val version: String,
    val isTrusted: Boolean
)

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
    private val downloaderPluginRepository: DownloaderPluginRepository,
    private val patchBundleRepository: PatchBundleRepository,
) : ViewModel() {
    private val powerManager = app.getSystemService<PowerManager>()!!

    val apps = pm.appList.map { apps ->
        apps.filter { (it.patches ?: 0) > 0 }.ifEmpty { null }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        null
    )

    val suggestedVersions = patchBundleRepository.suggestedVersions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        emptyMap()
    )

    val plugins = downloaderPluginRepository.pluginStates.map { states ->
        states.mapNotNull { (packageName, state) ->
            val packageInfo = pm.getPackageInfo(packageName) ?: return@mapNotNull null
            OnboardingPluginInfo(
                packageName = packageName,
                name = with(pm) { packageInfo.label() },
                version = packageInfo.versionName.orEmpty(),
                isTrusted = state is DownloaderPluginState.Loaded
            )
        }.sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val managerAutoUpdates = prefs.managerAutoUpdates.flow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        prefs.managerAutoUpdates.default
    )

    val patchesAutoUpdates = patchBundleRepository.sources.map { sources ->
        sources.find { it.uid == 0 }?.asRemoteOrNull?.autoUpdate ?: false
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        false
    )

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

    init {
        refreshPermissionStates()
        currentStep = if (allPermissionsGranted) OnboardingStep.Updates else OnboardingStep.Permissions
    }

    fun loadLabel(packageInfo: PackageInfo?) =
        with(pm) { packageInfo?.label() ?: app.getString(R.string.not_installed) }

    fun refreshPermissionStates() {
        canInstallUnknownApps = pm.canInstallPackages()
        isNotificationsEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                NotificationManagerCompat.from(app).areNotificationsEnabled()
        isBatteryOptimizationExempt = powerManager.isIgnoringBatteryOptimizations(app.packageName)
    }

    fun advance() {
        currentStep = nextStep(currentStep)
    }

    fun retreat() {
        currentStep = previousStep(currentStep)
    }

    fun trustPlugin(packageName: String) = viewModelScope.launch {
        downloaderPluginRepository.trustPackage(packageName)
    }

    fun revokePluginTrust(packageName: String) = viewModelScope.launch {
        downloaderPluginRepository.revokeTrustForPackage(packageName)
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
        prefs.firstLaunch.update(false)
    }

    private fun nextStep(from: OnboardingStep) = when (from) {
        OnboardingStep.Permissions -> OnboardingStep.Updates
        OnboardingStep.Updates -> if (plugins.value.isNotEmpty()) OnboardingStep.Sources else OnboardingStep.Apps
        OnboardingStep.Sources -> OnboardingStep.Apps
        OnboardingStep.Apps -> OnboardingStep.Apps
    }

    private fun previousStep(from: OnboardingStep) = when (from) {
        OnboardingStep.Permissions -> OnboardingStep.Permissions
        OnboardingStep.Updates -> OnboardingStep.Permissions
        OnboardingStep.Sources -> OnboardingStep.Updates
        OnboardingStep.Apps -> if (plugins.value.isNotEmpty()) OnboardingStep.Sources else OnboardingStep.Updates
    }

    private companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

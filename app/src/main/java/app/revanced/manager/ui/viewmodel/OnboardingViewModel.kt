package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import app.revanced.manager.domain.sources.Extensions.asRemoteOrNull
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.util.PM
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class OnboardingStep {
    Permissions,
    Updates,
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

        currentStep =
            if (allPermissionsGranted) OnboardingStep.Updates else OnboardingStep.Permissions
    }

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

    suspend fun applyAutoUpdatePrefs(managerEnabled: Boolean, patchesEnabled: Boolean, downloadersEnabled: Boolean) {
        prefs.managerAutoUpdates.update(managerEnabled)

        with(patchBundleRepository) {
            sources
                .first()
                .find { it.uid == 0 }
                ?.asRemoteOrNull
                ?.setAutoUpdate(patchesEnabled)

            if (patchesEnabled) updateCheck()
        }

        with(downloaderRepository) {
            downloaderSources
                .first()[0]
                ?.asRemoteOrNull
                ?.setAutoUpdate(downloadersEnabled)

            if (downloadersEnabled) updateCheck()
        }
    }

    suspend fun completeOnboarding() {
        prefs.completedOnboarding.update(true)
    }

    private fun nextStep(from: OnboardingStep) = when (from) {
        OnboardingStep.Permissions -> OnboardingStep.Updates
        OnboardingStep.Updates -> OnboardingStep.Apps
        OnboardingStep.Apps -> OnboardingStep.Apps
    }

    private fun previousStep(from: OnboardingStep) = when (from) {
        OnboardingStep.Permissions -> OnboardingStep.Permissions
        OnboardingStep.Updates -> OnboardingStep.Permissions
        OnboardingStep.Apps -> OnboardingStep.Updates
    }

}

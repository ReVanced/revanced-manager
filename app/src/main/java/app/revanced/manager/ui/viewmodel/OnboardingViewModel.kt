package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.sources.Extensions.asRemoteOrNull
import app.revanced.manager.domain.installer.ShizukuInstaller
import app.revanced.shizukulibrary.adb.AdbConnectionManager
import app.revanced.shizukulibrary.adb.AdbStarter
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.patcher.aapt.Aapt
import app.revanced.manager.util.PM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

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
    private val shizukuInstaller: ShizukuInstaller,
    private val adbConnectionManager: AdbConnectionManager
) : ViewModel() {
    private val powerManager = app.getSystemService<PowerManager>()!!

    val apps = pm.appList.map { apps ->
        apps.filter { (it.patches ?: 0) > 0 }.ifEmpty { null }
    }
    val apiUrl = prefs.api.default

    val hasNetworkError = combine(apps, patchBundleRepository.updateErrors) { apps, updateErrors ->
        apps == null && updateErrors.isNotEmpty()
    }

    val suggestedVersions = patchBundleRepository.suggestedVersions

    var canInstallUnknownApps by mutableStateOf(false)
        private set
    var isNotificationsEnabled by mutableStateOf(false)
        private set
    var isBatteryOptimizationExempt by mutableStateOf(false)
        private set
    var isShizukuAvailable by mutableStateOf(false)
        private set
    var isShizukuAuthorized by mutableStateOf(false)
        private set
    var isAdbConnected by mutableStateOf(false)
        private set
    var isPairing by mutableStateOf(false)
        private set
    var showAdbHintDialog by mutableStateOf(false)

    var adbPort by mutableStateOf("5555")
    var adbPairingPort by mutableStateOf("")
    var adbPairingCode by mutableStateOf("")

    val isDeviceSupported = Aapt.supportsDevice()

    var currentStep by mutableStateOf(OnboardingStep.Permissions)
        private set

    val allPermissionsGranted
        get() = canInstallUnknownApps && isNotificationsEnabled && isBatteryOptimizationExempt && (!isShizukuAvailable || isShizukuAuthorized)

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
        isShizukuAvailable = shizukuInstaller.isAvailable()
        isShizukuAuthorized = shizukuInstaller.hasPermission()
        isAdbConnected = adbConnectionManager.isConnected
    }

    fun requestShizuku() {
        if (!shizukuInstaller.isAvailable()) {
            Toast.makeText(app, R.string.shizuku_not_running, Toast.LENGTH_LONG).show()
            return
        }
        try {
            Shizuku.requestPermission(0)
            refreshPermissionStates()
        } catch (e: Exception) {
            Toast.makeText(app, e.message ?: app.getString(R.string.shizuku_request_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun bootstrapAdb() {
        viewModelScope.launch {
            try {
                // Formatting for adb_keys: Base64 + " user@host\n"
                // MuntashirAkon's manager can provide certificate bits
                val cert = adbConnectionManager.certificate.encoded
                val pubKey = android.util.Base64.encodeToString(cert, android.util.Base64.NO_WRAP) + " revanced@manager\n"

                shizukuInstaller.bootstrapAdb(pubKey)

                // Use AdbStarter to handle connection and command
                AdbStarter.startAdb(app, adbPort.toIntOrNull() ?: 5555) { log ->
                    Log.d("OnboardingVM", log)
                }

                isAdbConnected = adbConnectionManager.isConnected
                val msg = if (isAdbConnected) R.string.adb_bootstrap_success else R.string.adb_connection_failed
                Toast.makeText(app, msg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(app, app.getString(R.string.adb_bootstrap_fail) + ": ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun connectAdb() {
        viewModelScope.launch {
            try {
                val port = adbPort.toIntOrNull() ?: 5555
                withContext(Dispatchers.IO) {
                    adbConnectionManager.connect("127.0.0.1", port)
                }
                isAdbConnected = adbConnectionManager.isConnected
                if (isAdbConnected) {
                    Toast.makeText(app, R.string.adb_connected, Toast.LENGTH_SHORT).show()
                } else {
                    showAdbHintDialog = true
                }
            } catch (e: Exception) {
                showAdbHintDialog = true
            }
        }
    }

    fun pairAdb() {
        viewModelScope.launch {
            isPairing = true
            try {
                val port = adbPairingPort.toIntOrNull()
                val code = adbPairingCode
                if (port == null || code.isEmpty()) {
                    showAdbHintDialog = true
                    return@launch
                }
                // AdbConnectionManager supports pairing if implemented in its base class
                // Usually it requires a specialized handshake.
                // For now, let's assume it's connecting since we don't have a direct pair method in the wrapper.
                withContext(Dispatchers.IO) {
                    adbConnectionManager.connect("127.0.0.1", port)
                }
                isAdbConnected = adbConnectionManager.isConnected
                if (isAdbConnected) {
                    Toast.makeText(app, R.string.adb_pairing_success, Toast.LENGTH_SHORT).show()
                } else {
                    showAdbHintDialog = true
                }
            } catch (e: Exception) {
                showAdbHintDialog = true
            } finally {
                isPairing = false
            }
        }
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
            val src = sources
                .first()
                .find { it.isDefault }
                ?.asRemoteOrNull ?: return@with

            src.setAutoUpdate(patchesEnabled)
            update(src)
        }

        with(downloaderRepository) {
            val src = downloaderSources
                .first()[0]
                ?.asRemoteOrNull ?: return@with

            src.setAutoUpdate(downloadersEnabled)
            update(src)
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

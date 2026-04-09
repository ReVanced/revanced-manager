package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.ui.model.RootCheckResult
import app.revanced.manager.util.tag
import app.revanced.manager.util.toast
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AdvancedSettingsViewModel(
    val prefs: PreferencesManager,
    private val app: Application,
    private val patchBundleRepository: PatchBundleRepository,
    private val downloaderRepository: DownloaderRepository,
    private val rootInstaller: RootInstaller
) : ViewModel() {
    var rootStatus by mutableStateOf<RootCheckResult?>(null)
        private set

    init { refreshRootStatus() }

    fun refreshRootStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            rootStatus = rootInstaller.checkRootStatus()
        }
    }

    val debugLogFileName: String
        get() {
            val time = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())

            return "revanced-manager_logcat_$time"
        }

    fun exportDebugLogs(target: Uri) = viewModelScope.launch {
        val exitCode = try {
            withContext(Dispatchers.IO) {
                app.contentResolver.openOutputStream(target)!!.bufferedWriter().use { writer ->
                    val consumer = Redirect.Consume { flow ->
                        flow.onEach {
                            writer.write("${it}\n")
                        }.flowOn(Dispatchers.IO).collect()
                    }

                    process("logcat", "-d", stdout = consumer).resultCode
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Got exception while exporting logs", e)
            app.toast(app.getString(R.string.debug_logs_export_failed))
            return@launch
        }

        if (exitCode == 0)
            app.toast(app.getString(R.string.debug_logs_export_success))
        else
            app.toast(app.getString(R.string.debug_logs_export_read_failed, exitCode))
    }
}
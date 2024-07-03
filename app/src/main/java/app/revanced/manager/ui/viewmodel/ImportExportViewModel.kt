package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.manager.KeystoreManager
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.domain.repository.SerializedSelection
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.repository.PatchOptionsRepository
import app.revanced.manager.util.JSON_MIMETYPE
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteExisting
import kotlin.io.path.inputStream

@OptIn(ExperimentalSerializationApi::class)
class ImportExportViewModel(
    private val app: Application,
    private val keystoreManager: KeystoreManager,
    private val selectionRepository: PatchSelectionRepository,
    private val optionsRepository: PatchOptionsRepository,
    patchBundleRepository: PatchBundleRepository
) : ViewModel() {
    private val contentResolver = app.contentResolver
    val patchBundles = patchBundleRepository.sources
    var selectedBundle by mutableStateOf<PatchBundleSource?>(null)
        private set
    var selectionAction by mutableStateOf<SelectionAction?>(null)
        private set
    private var keystoreImportPath by mutableStateOf<Path?>(null)
    val showCredentialsDialog by derivedStateOf { keystoreImportPath != null }

    val packagesWithOptions = optionsRepository.getPackagesWithSavedOptions()

    fun resetOptionsForPackage(packageName: String) = viewModelScope.launch {
        optionsRepository.clearOptionsForPackage(packageName)
        app.toast(app.getString(R.string.patch_options_reset_toast))
    }

    fun clearOptionsForBundle(patchBundle: PatchBundleSource) = viewModelScope.launch {
        optionsRepository.clearOptionsForPatchBundle(patchBundle.uid)
        app.toast(app.getString(R.string.patch_options_reset_toast))
    }

    fun resetOptions() = viewModelScope.launch {
        optionsRepository.reset()
        app.toast(app.getString(R.string.patch_options_reset_toast))
    }

    fun startKeystoreImport(content: Uri) = viewModelScope.launch {
        val path = withContext(Dispatchers.IO) {
            File.createTempFile("signing", "ks", app.cacheDir).toPath().also {
                Files.copy(
                    contentResolver.openInputStream(content)!!,
                    it,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }

        aliases.forEach { alias ->
            knownPasswords.forEach { pass ->
                if (tryKeystoreImport(alias, pass, path)) {
                    return@launch
                }
            }
        }

        keystoreImportPath = path
    }

    fun cancelKeystoreImport() {
        keystoreImportPath?.deleteExisting()
        keystoreImportPath = null
    }

    suspend fun tryKeystoreImport(cn: String, pass: String) =
        tryKeystoreImport(cn, pass, keystoreImportPath!!)

    private suspend fun tryKeystoreImport(cn: String, pass: String, path: Path): Boolean {
        path.inputStream().use { stream ->
            if (keystoreManager.import(cn, pass, stream)) {
                app.toast(app.getString(R.string.import_keystore_success))
                cancelKeystoreImport()
                return true
            }
        }

        return false
    }

    override fun onCleared() {
        super.onCleared()

        cancelKeystoreImport()
    }

    fun canExport() = keystoreManager.hasKeystore()

    fun exportKeystore(target: Uri) = viewModelScope.launch {
        keystoreManager.export(contentResolver.openOutputStream(target)!!)
        app.toast(app.getString(R.string.export_keystore_success))
    }

    fun regenerateKeystore() = viewModelScope.launch {
        keystoreManager.regenerate()
        app.toast(app.getString(R.string.regenerate_keystore_success))
    }

    fun resetSelection() = viewModelScope.launch {
        withContext(Dispatchers.Default) { selectionRepository.reset() }
        app.toast(app.getString(R.string.reset_patch_selection_success))
    }

    fun executeSelectionAction(target: Uri) = viewModelScope.launch {
        val source = selectedBundle!!
        val action = selectionAction!!
        clearSelectionAction()

        action.execute(source.uid, target)
    }

    fun selectBundle(bundle: PatchBundleSource) {
        selectedBundle = bundle
    }

    fun clearSelectionAction() {
        selectionAction = null
        selectedBundle = null
    }

    fun importSelection() = clearSelectionAction().also {
        selectionAction = Import()
    }

    fun exportSelection() = clearSelectionAction().also {
        selectionAction = Export()
    }

    sealed interface SelectionAction {
        suspend fun execute(bundleUid: Int, location: Uri)
        val activityContract: ActivityResultContract<String, Uri?>
        val activityArg: String
    }

    private inner class Import : SelectionAction {
        override val activityContract = ActivityResultContracts.GetContent()
        override val activityArg = JSON_MIMETYPE
        override suspend fun execute(bundleUid: Int, location: Uri) = uiSafe(
            app,
            R.string.import_patch_selection_fail,
            "Failed to restore patch selection"
        ) {
            val selection = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(location)!!.use {
                    Json.decodeFromStream<SerializedSelection>(it)
                }
            }

            selectionRepository.import(bundleUid, selection)
            app.toast(app.getString(R.string.import_patch_selection_success))
        }
    }

    private inner class Export : SelectionAction {
        override val activityContract = ActivityResultContracts.CreateDocument(JSON_MIMETYPE)
        override val activityArg = "selection.json"
        override suspend fun execute(bundleUid: Int, location: Uri) = uiSafe(
            app,
            R.string.export_patch_selection_fail,
            "Failed to backup patch selection"
        ) {
            val selection = selectionRepository.export(bundleUid)

            withContext(Dispatchers.IO) {
                contentResolver.openOutputStream(location, "wt")!!.use {
                    Json.Default.encodeToStream(selection, it)
                }
            }
            app.toast(app.getString(R.string.export_patch_selection_success))
        }
    }

    private companion object {
        val knownPasswords = arrayOf("ReVanced", "s3cur3p@ssw0rd")
        val aliases = arrayOf(KeystoreManager.DEFAULT, "alias", "ReVanced Key")
    }
}
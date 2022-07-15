package app.revanced.manager.ui.screens.mainsubscreens

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.revanced.manager.R
import app.revanced.manager.backend.api.ManagerAPI
import app.revanced.manager.backend.utils.Aapt
import app.revanced.manager.backend.utils.aligning.ZipAligner
import app.revanced.manager.backend.utils.filesystem.ZipFileSystemUtils
import app.revanced.manager.backend.utils.signing.Signer
import app.revanced.manager.settings
import app.revanced.manager.ui.Resource
import app.revanced.manager.ui.components.FloatingActionButton
import app.revanced.manager.ui.screens.destinations.AppSelectorScreenDestination
import app.revanced.manager.ui.screens.destinations.PatchesSelectorScreenDestination
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.data.Data
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.util.patch.implementation.DexPatchBundle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import dalvik.system.DexClassLoader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

private const val tag = "PatcherScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph
@Composable
fun PatcherSubscreen(
    navigator: NavController,
    vm: PatcherViewModel = viewModel(LocalContext.current as ComponentActivity)
) {
    val selectedAppPackage by vm.selectedAppPackage
    val hasAppSelected = selectedAppPackage.isPresent

    Scaffold(floatingActionButton = {
        FloatingActionButton(
            enabled = hasAppSelected && vm.anyPatchSelected(),
            icon = { Icon(Icons.Default.Build, "sd") },
            text = { Text("Patch") },
            onClick = { vm.startPatcher() },
        )
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                onClick = {
                    navigator.navigate(
                        AppSelectorScreenDestination().route
                    )
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.card_application_header),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = selectedAppPackage.orElse(stringResource(R.string.card_application_body)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(0.dp, 8.dp)
                    )
                }
            }
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                enabled = hasAppSelected,
                onClick = {
                    navigator.navigate(PatchesSelectorScreenDestination().route)
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.card_patches_header),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (!hasAppSelected) {
                            "Select an application first."
                        } else if (vm.anyPatchSelected()) {
                            "${vm.selectedAmount()} patches selected."
                        } else {
                            stringResource(R.string.card_patches_body_patches)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(0.dp, 8.dp)
                    )
                }
            }
        }
    }
}

data class PatchClass(
    val patch: Class<out Patch<Data>>,
    val unsupported: Boolean
)

class PatcherViewModel(val app: Application) : AndroidViewModel(app) {
    private val aaptPath = Aapt.binary(app).absolutePath
    private val bundleCacheDir = app.filesDir.resolve("bundle-cache").also { it.mkdirs() }
    private val frameworkPath = app.filesDir.resolve("framework").also { it.mkdirs() }.absolutePath
    private val integrationsCacheDir = app.filesDir.resolve("integrations-cache").also { it.mkdirs() }

    val selectedAppPackage = mutableStateOf(Optional.empty<String>())
    private val selectedPatches = mutableStateListOf<String>()
    val patches = mutableStateOf<Resource<List<Class<out Patch<Data>>>>>(Resource.Loading)

    init {
        loadPatches()
    }

    fun setSelectedAppPackage(appId: String) {
        selectedAppPackage.value.ifPresent { s ->
            if (s != appId) selectedPatches.clear()
        }
        selectedAppPackage.value = Optional.of(appId)
    }

    fun selectPatch(patchId: String, state: Boolean) {
        if (state) selectedPatches.add(patchId)
        else selectedPatches.remove(patchId)
    }

    fun isPatchSelected(patchId: String): Boolean {
        return selectedPatches.contains(patchId)
    }

    fun selectedAmount(): Int {
        return selectedPatches.size
    }

    fun anyPatchSelected(): Boolean {
        return !selectedPatches.isEmpty()
    }

    private fun findPatchesByIds(ids: Iterable<String>): List<Class<out Patch<Data>>> {
        val (patches) = patches.value as? Resource.Success ?: return listOf()
        return patches.filter { patch -> ids.any { it == patch.patchName } }
    }

    private fun getSelectedPackageInfo() =
        if (selectedAppPackage.value.isPresent)
            app.packageManager.getPackageInfo(
                selectedAppPackage.value.get(),
                PackageManager.GET_META_DATA
            )
        else null

    fun getFilteredPatches(): List<PatchClass> {
        return buildList {
            val selected = getSelectedPackageInfo() ?: return@buildList
            val (patches) = patches.value as? Resource.Success ?: return@buildList
            patches.forEach patch@{ patch ->
                var unsupported = false
                patch.compatiblePackages?.forEach { pkg ->
                    // if we detect unsupported once, don't overwrite it
                    if (pkg.name == selected.packageName) {
                        if (!unsupported)
                            unsupported =
                                pkg.versions.isNotEmpty() && !pkg.versions.any { it == selected.versionName }
                        add(PatchClass(patch, unsupported))
                    }
                }
            }
        }
    }

    private suspend fun downloadDefaultPatchBundle(workdir: File): File {
        return try {
            val (_, out) = ManagerAPI.downloadPatches(workdir, this.app.baseContext.settings.data.map { pref -> pref.get(stringPreferencesKey("patches")) }.first().toString())
            out
        } catch (e: Exception) {
            throw Exception("Failed to download default patch bundle", e)
        }
    }

    private suspend fun downloadIntegrations(workdir: File): File {
        return try {
            val (_, out) = ManagerAPI.downloadIntegrations(workdir, this.app.baseContext.settings.data.map { pref -> pref.get(stringPreferencesKey("integrations")) }.first().toString())
            out
        } catch (e: Exception) {
            throw Exception("Failed to download integrations", e)
        }
    }

    private fun loadPatches() = viewModelScope.launch {
        try {
            val file = downloadDefaultPatchBundle(bundleCacheDir)
            loadPatches0(file.absolutePath)
        } catch (e: Exception) {
            Log.e(tag, "An error occurred while loading patches", e)
        }
    }

    private fun loadPatches0(path: String) {
        val patchClasses = DexPatchBundle(
            path, DexClassLoader(
                path,
                app.codeCacheDir.absolutePath,
                null,
                javaClass.classLoader
            )
        ).loadPatches()
        patches.value = Resource.Success(patchClasses)
    }

    private fun createWorkDir(): File {
        return app.filesDir.resolve("tmp-${System.currentTimeMillis()}").also { it.mkdirs() }
    }

    fun startPatcher() {
        val tag = "Patcher"

        viewModelScope.launch {
            Log.d(tag, "Checking prerequisites")
            val info = getSelectedPackageInfo()?.applicationInfo ?: return@launch
            val patches = findPatchesByIds(selectedPatches)
            if (patches.isEmpty()) return@launch
            val integrations = downloadIntegrations(integrationsCacheDir)

            Log.d(tag, "Creating directories")
            val workdir = createWorkDir()
            val inputFile = File(workdir.parentFile!!, "base.apk")
            val patchedFile = File(workdir, "patched.apk")
            val alignedFile = File(workdir, "aligned.apk")
            val outputFile = File(workdir, "out.apk")
            val cacheDirectory = workdir.resolve("cache")
            val buildDirectory = cacheDirectory.resolve("build")

            try {
//                Log.d(tag, "Copying base.apk from ${info.packageName}")
//                withContext(Dispatchers.IO) {
//                    Files.copy(
//                        File(info.publicSourceDir).toPath(),
//                        inputFile.toPath(),
//                        StandardCopyOption.REPLACE_EXISTING
//                    )
//                }

                Log.d(tag, "Creating patcher")
                val patcher = Patcher(
                    PatcherOptions(
                        inputFile,
                        cacheDirectory.absolutePath,
                        patchResources = true,
                        aaptPath = aaptPath,
                        frameworkFolderLocation = frameworkPath
                    )
                )

                Log.d(tag, "Merging integrations")
                patcher.addFiles(listOf(integrations)) {}

                Log.d(tag, "Adding ${patches.size} patch(es)")
                patcher.addPatches(patches)

                Log.d(tag, "Applying patches")
                patcher.applyPatches().forEach { (patch, result) ->
                    if (result.isSuccess) {
                        Log.i(tag, "[success] $patch")
                        return@forEach
                    }
                    Log.e(tag, "[error] $patch:", result.exceptionOrNull()!!)
                }

                Log.d(tag, "Saving file")
                val result = patcher.save()
                ZipFileSystemUtils(result.resourceFile!!, patchedFile).use { fs ->
                    result.dexFiles.forEach { fs.write(it.name, it.dexFileInputStream.readBytes()) }
                    fs.writeInput()
                    fs.uncompress(*result.doNotCompress!!.toTypedArray())
                }

                Log.d(tag, "Aligning apk")
                ZipAligner.align(patchedFile, alignedFile)
                Log.d(tag, "Signing apk")
                Signer("ReVanced", "s3cur3p@ssw0rd").signApk(alignedFile, outputFile)

                // TODO: install apk!
                Log.d(tag, "Installing apk")
            } catch (e: Exception) {
                Log.e(tag, "Error while patching", e)
            }

            Log.d(tag, "Deleting workdir")
            //workdir.deleteRecursively()
        }
    }
}
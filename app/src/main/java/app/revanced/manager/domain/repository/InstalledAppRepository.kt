package app.revanced.manager.domain.repository

import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.apps.installed.AppliedPatch
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.data.room.apps.installed.InstalledPatchBundle
import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.util.PatchSelection
import kotlinx.coroutines.flow.distinctUntilChanged

class InstalledAppRepository(
    db: AppDatabase
) {
    private val dao = db.installedAppDao()

    fun getAll() = dao.getAll().distinctUntilChanged()

    suspend fun get(packageName: String) = dao.get(packageName)

    suspend fun getAppliedPatches(packageName: String): PatchSelection =
        dao.getPatchesSelection(packageName).mapValues { (_, patches) -> patches.toSet() }

    suspend fun getInstalledPatchBundles(packageName: String) =
        dao.getInstalledPatchBundles(packageName)

    suspend fun addOrUpdate(
        currentPackageName: String,
        originalPackageName: String,
        version: String,
        installType: InstallType,
        patchSelection: PatchSelection,
        bundleInfo: Map<Int, PatchBundleInfo.Global> = emptyMap()
    ) {
        dao.upsertApp(
            InstalledApp(
                currentPackageName = currentPackageName,
                originalPackageName = originalPackageName,
                version = version,
                installType = installType
            ),
            patchSelection.flatMap { (uid, patches) ->
                patches.map { patch ->
                    AppliedPatch(
                        packageName = currentPackageName,
                        bundle = uid,
                        patchName = patch
                    )
                }
            },
            patchSelection.keys.mapNotNull { uid ->
                val info = bundleInfo[uid] ?: return@mapNotNull null
                InstalledPatchBundle(
                    packageName = currentPackageName,
                    bundleUid = uid,
                    bundleName = info.name,
                    bundleVersion = info.version
                )
            }
        )
    }

    suspend fun delete(installedApp: InstalledApp) {
        dao.delete(installedApp)
    }
}
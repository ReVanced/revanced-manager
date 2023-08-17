package app.revanced.manager.domain.repository

import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.apps.installed.AppliedPatch
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.util.PatchesSelection
import kotlinx.coroutines.flow.distinctUntilChanged

class InstalledAppRepository(
    db: AppDatabase
) {
    private val dao = db.installedAppDao()

    fun getAll() = dao.getAll().distinctUntilChanged()

    suspend fun get(packageName: String) = dao.get(packageName)

    suspend fun getAppliedPatches(packageName: String): PatchesSelection =
        dao.getPatchesSelection(packageName).mapValues { (_, patches) -> patches.toSet() }

    suspend fun add(
        currentPackageName: String,
        originalPackageName: String,
        version: String,
        installType: InstallType,
        patchesSelection: PatchesSelection
    ) {
        dao.insertApp(
            InstalledApp(
                currentPackageName = currentPackageName,
                originalPackageName = originalPackageName,
                version = version,
                installType = installType
            ),
            patchesSelection.flatMap { (uid, patches) ->
                patches.map { patch ->
                    AppliedPatch(
                        packageName = currentPackageName,
                        bundle = uid,
                        patchName = patch
                    )
                }
            }
        )
    }

    suspend fun delete(installedApp: InstalledApp) {
        dao.delete(installedApp)
    }
}
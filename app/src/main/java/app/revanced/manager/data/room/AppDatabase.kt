package app.revanced.manager.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.revanced.manager.data.room.apps.downloaded.DownloadedAppDao
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.data.room.apps.installed.AppliedPatch
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.data.room.apps.installed.InstalledAppDao
import app.revanced.manager.data.room.selection.PatchSelection
import app.revanced.manager.data.room.selection.SelectedPatch
import app.revanced.manager.data.room.selection.SelectionDao
import app.revanced.manager.data.room.bundles.PatchBundleDao
import app.revanced.manager.data.room.bundles.PatchBundleEntity
import kotlin.random.Random

@Database(entities = [PatchBundleEntity::class, PatchSelection::class, SelectedPatch::class, DownloadedApp::class, InstalledApp::class, AppliedPatch::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patchBundleDao(): PatchBundleDao
    abstract fun selectionDao(): SelectionDao
    abstract fun downloadedAppDao(): DownloadedAppDao
    abstract fun installedAppDao(): InstalledAppDao

    companion object {
        fun generateUid() = Random.Default.nextInt()
    }
}
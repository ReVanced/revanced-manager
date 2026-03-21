package app.revanced.manager.data.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import app.revanced.manager.data.room.apps.downloaded.DownloadedApp
import app.revanced.manager.data.room.apps.downloaded.DownloadedAppDao
import app.revanced.manager.data.room.apps.installed.AppliedPatch
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.data.room.apps.installed.InstalledAppDao
import app.revanced.manager.data.room.apps.installed.InstalledPatchBundle
import app.revanced.manager.data.room.selection.PatchSelection
import app.revanced.manager.data.room.selection.SelectedPatch
import app.revanced.manager.data.room.selection.SelectionDao
import app.revanced.manager.data.room.bundles.PatchBundleDao
import app.revanced.manager.data.room.bundles.PatchBundleEntity
import app.revanced.manager.data.room.downloader.DownloaderDao
import app.revanced.manager.data.room.downloader.DownloaderEntity
import app.revanced.manager.data.room.options.Option
import app.revanced.manager.data.room.options.OptionDao
import app.revanced.manager.data.room.options.OptionGroup
import java.util.UUID
import kotlin.random.Random
import kotlin.uuid.Uuid

@Database(
    entities = [PatchBundleEntity::class, PatchSelection::class, SelectedPatch::class, DownloadedApp::class, InstalledApp::class, AppliedPatch::class, InstalledPatchBundle::class, OptionGroup::class, Option::class, DownloaderEntity::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(
            from = 2,
            to = 3,
            spec = AppDatabase.DeleteTrustedDownloaders::class
        )
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patchBundleDao(): PatchBundleDao
    abstract fun selectionDao(): SelectionDao
    abstract fun downloadedAppDao(): DownloadedAppDao
    abstract fun installedAppDao(): InstalledAppDao
    abstract fun optionDao(): OptionDao
    abstract fun downloaderDao(): DownloaderDao

    @DeleteTable(tableName = "trusted_downloaders")
    class DeleteTrustedDownloaders : AutoMigrationSpec

    companion object {
        fun generateUid() = UUID.randomUUID().mostSignificantBits.toInt()
    }
}
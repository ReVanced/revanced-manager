package app.revanced.manager.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.revanced.manager.data.room.apps.AppDao
import app.revanced.manager.data.room.apps.DownloadedApp
import app.revanced.manager.data.room.selection.PatchSelection
import app.revanced.manager.data.room.selection.SelectedPatch
import app.revanced.manager.data.room.selection.SelectionDao
import app.revanced.manager.data.room.sources.SourceDao
import app.revanced.manager.data.room.sources.SourceEntity
import kotlin.random.Random

@Database(entities = [SourceEntity::class, PatchSelection::class, SelectedPatch::class, DownloadedApp::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun selectionDao(): SelectionDao
    abstract fun appDao(): AppDao

    companion object {
        fun generateUid() = Random.Default.nextInt()
    }
}
package app.revanced.manager.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PatchEntity::class],
    version = 1
)
abstract class PatchDatabase : RoomDatabase() {
    abstract val dao: PatchesDao
}
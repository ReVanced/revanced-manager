package app.revanced.manager.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.revanced.manager.data.room.sources.SourceEntity
import app.revanced.manager.data.room.sources.SourceDao

@Database(entities = [SourceEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
}
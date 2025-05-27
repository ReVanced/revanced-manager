package app.revanced.manager.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.revanced.manager.data.room.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module


val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add columns for RemoteBundleProperties
        database.execSQL("ALTER TABLE patch_bundles ADD COLUMN search_update INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE patch_bundles ADD COLUMN changelog TEXT")
        database.execSQL("ALTER TABLE patch_bundles ADD COLUMN publish_date TEXT")

        // Add columns for RemoteLatestBundleProperties
        database.execSQL("ALTER TABLE patch_bundles ADD COLUMN latest_changelog TEXT")
        database.execSQL("ALTER TABLE patch_bundles ADD COLUMN latest_publish_date TEXT")
        database.execSQL("ALTER TABLE patch_bundles ADD COLUMN latest_version TEXT")
    }
}

val databaseModule = module {
    fun provideAppDatabase(context: Context) =
        Room
            .databaseBuilder(context, AppDatabase::class.java, "manager")
            .addMigrations(MIGRATION_1_2)
            .build()

    single {
        provideAppDatabase(androidContext())
    }
}
package app.revanced.manager.di

import android.content.Context
import androidx.room.Room
import app.revanced.manager.data.room.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    fun provideAppDatabase(context: Context) = Room.databaseBuilder(context, AppDatabase::class.java, "manager").build()

    single {
        provideAppDatabase(androidContext())
    }
}
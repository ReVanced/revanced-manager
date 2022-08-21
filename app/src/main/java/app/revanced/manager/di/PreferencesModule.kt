package app.revanced.manager.di

import android.content.Context
import app.revanced.manager.preferences.PreferencesManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val preferencesModule = module {
    fun providePreferences(
        context: Context
    ) = PreferencesManager(context.getSharedPreferences("preferences", Context.MODE_PRIVATE))

    singleOf(::providePreferences)
}
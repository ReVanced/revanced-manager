package app.revanced.manager.compose.di

import android.content.Context
import app.revanced.manager.compose.domain.manager.PreferencesManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val preferencesModule = module {
    fun providePreferences(
        context: Context
    ) = PreferencesManager(context.getSharedPreferences("preferences", Context.MODE_PRIVATE))

    singleOf(::providePreferences)
}
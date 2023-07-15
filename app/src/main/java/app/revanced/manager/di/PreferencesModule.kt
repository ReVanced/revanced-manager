package app.revanced.manager.di

import app.revanced.manager.domain.manager.PreferencesManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val preferencesModule = module {
    singleOf(::PreferencesManager)
}
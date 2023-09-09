package app.revanced.manager.di

import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.service.RootConnection
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val rootModule = module {
    singleOf(::RootConnection)
    singleOf(::RootInstaller)
}
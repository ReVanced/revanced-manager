package app.revanced.manager.di

import app.revanced.manager.network.service.HttpService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val serviceModule = module {
    singleOf(::HttpService)
}
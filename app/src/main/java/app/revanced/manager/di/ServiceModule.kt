package app.revanced.manager.di

import app.revanced.manager.domain.protocol.FileProtocolHandler
import app.revanced.manager.domain.protocol.HttpProtocolHandler
import app.revanced.manager.domain.protocol.ProtocolHandler
import app.revanced.manager.network.service.HttpService
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val serviceModule = module {
    singleOf(::HttpService)
    singleOf(::HttpProtocolHandler) { bind<ProtocolHandler>() }
    singleOf(::FileProtocolHandler) { bind<ProtocolHandler>() }
}
package app.revanced.manager.di

import app.revanced.manager.domain.protocol.ContentProtocolHandler
import app.revanced.manager.domain.protocol.FilePickRequest
import app.revanced.manager.domain.protocol.FileProtocolHandler
import app.revanced.manager.domain.protocol.HttpProtocolHandler
import app.revanced.manager.domain.protocol.ProtocolHandler
import app.revanced.manager.network.service.HttpService
import kotlinx.coroutines.channels.Channel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val serviceModule = module {
    singleOf(::HttpService)
    single { Channel<FilePickRequest>() }
    singleOf(::HttpProtocolHandler)
    singleOf(::ContentProtocolHandler)
    singleOf(::FileProtocolHandler)
    single<Map<String, ProtocolHandler>> {
        mapOf(
            "http" to get<HttpProtocolHandler>(),
            "https" to get<HttpProtocolHandler>(),
            "content" to get<ContentProtocolHandler>(),
            "file" to get<FileProtocolHandler>(),
        )
    }
}
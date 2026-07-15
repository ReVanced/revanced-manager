package app.revanced.manager.di

import app.revanced.manager.domain.protocol.ContentProtocolHandler
import app.revanced.manager.domain.protocol.FileProtocolHandler
import app.revanced.manager.domain.protocol.HttpProtocolHandler
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.util.FilePicker
import app.revanced.manager.util.UiFilePicker
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val serviceModule = module {
    singleOf(::HttpService)
    single<FilePicker> { UiFilePicker() }
    singleOf(::HttpProtocolHandler)
    single { ContentProtocolHandler(androidContext().contentResolver) }
    singleOf(::FileProtocolHandler)
}

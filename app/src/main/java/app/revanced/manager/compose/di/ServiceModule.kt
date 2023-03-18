package app.revanced.manager.compose.di

import app.revanced.manager.compose.network.service.HttpService
import app.revanced.manager.compose.network.service.ReVancedService
import app.revanced.manager.compose.network.service.ReVancedServiceImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val serviceModule = module {
    fun provideReVancedService(
        client: HttpService,
    ): ReVancedService {
        return ReVancedServiceImpl(
            client = client,
        )
    }

    single { provideReVancedService(get()) }
    singleOf(::HttpService)
}
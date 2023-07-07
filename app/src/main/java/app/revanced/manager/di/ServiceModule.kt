package app.revanced.manager.di

import app.revanced.manager.network.service.GithubService
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.service.ReVancedService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val serviceModule = module {
    fun provideReVancedService(
        client: HttpService,
    ): ReVancedService {
        return ReVancedService(
            client = client,
        )
    }

    single { provideReVancedService(get()) }
    singleOf(::HttpService)
    singleOf(::GithubService)
}
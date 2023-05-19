package app.revanced.manager.compose.di

import app.revanced.manager.compose.domain.repository.ReVancedRepositoryImpl
import app.revanced.manager.compose.network.api.ManagerAPI
import app.revanced.manager.compose.patcher.data.repository.PatchesRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::ReVancedRepositoryImpl)
    singleOf(::ManagerAPI)
    singleOf(::PatchesRepository)
}
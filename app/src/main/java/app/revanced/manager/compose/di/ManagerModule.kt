package app.revanced.manager.compose.di

import app.revanced.manager.compose.domain.repository.SourceRepository
import app.revanced.manager.compose.patcher.SignerService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val managerModule = module {
    singleOf(::SignerService)
}
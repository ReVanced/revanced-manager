package app.revanced.manager.compose.di

import app.revanced.manager.compose.patcher.SignerService
import app.revanced.manager.compose.util.PM
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val managerModule = module {
    singleOf(::SignerService)
    singleOf(::PM)
}
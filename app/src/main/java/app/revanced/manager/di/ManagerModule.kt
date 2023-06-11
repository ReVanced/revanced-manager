package app.revanced.manager.di

import app.revanced.manager.domain.manager.KeystoreManager
import app.revanced.manager.util.PM
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val managerModule = module {
    singleOf(::KeystoreManager)
    singleOf(::PM)
}
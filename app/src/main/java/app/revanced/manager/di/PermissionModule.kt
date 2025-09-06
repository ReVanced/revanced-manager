package app.revanced.manager.di

import app.revanced.manager.util.PermissionHelper
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val permissionModule = module {
    singleOf(::PermissionHelper)
}

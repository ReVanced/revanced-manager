package app.revanced.manager.di

import app.revanced.manager.ui.viewmodel.PermissionStateHolder
import app.revanced.manager.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val permissionModule = module {
    singleOf(::PermissionHelper)

    factory { (permission: String, scope: CoroutineScope) ->
        PermissionStateHolder(
            permission = permission,
            scope = scope
        )
    }
}

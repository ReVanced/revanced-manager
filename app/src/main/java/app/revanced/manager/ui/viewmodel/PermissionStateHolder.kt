package app.revanced.manager.ui.viewmodel

import app.revanced.manager.util.AndroidPermission
import app.revanced.manager.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class PermissionStateHolder(
    @AndroidPermission val permission: String,
    private val scope: CoroutineScope,
) : KoinComponent {
    private val permissionHelper: PermissionHelper = get()

    private val _permissionState = MutableStateFlow<PermissionHelper.PermissionState?>(null)
    val permissionState = _permissionState.asStateFlow()

    fun refreshPermissionState() {
        scope.launch {
            _permissionState.value = permissionHelper.getPermissionState(permission)
        }
    }

    init {
        refreshPermissionState()
    }
}

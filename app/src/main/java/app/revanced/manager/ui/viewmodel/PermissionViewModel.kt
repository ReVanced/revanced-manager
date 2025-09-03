package app.revanced.manager.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import app.revanced.manager.util.permissions.AndroidPermission
import app.revanced.manager.util.permissions.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@OptIn(SavedStateHandleSaveableApi::class)
class PermissionViewModel(
    @AndroidPermission val permission: String
) : ViewModel(), KoinComponent {
    private val permissionHelper: PermissionHelper = get()

    private val _permissionState = MutableStateFlow<PermissionHelper.PermissionState?>(null)

    fun refreshPermissionState(activity: Activity) {
        viewModelScope.launch {
            _permissionState.value = permissionHelper.getPermissionState(activity, permission)
        }
    }

    val shouldShowDialog = _permissionState
        .map { state ->
            state == PermissionHelper.PermissionState.FirstTime ||
                    state == PermissionHelper.PermissionState.DeniedWithRationale
        }
}

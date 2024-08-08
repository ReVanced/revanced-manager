package app.revanced.manager.ui.component

import android.content.pm.PackageInstaller
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import com.github.materiiapps.enumutil.FromValue

private typealias InstallerStatusDialogButtonHandler = ((model: InstallerModel) -> Unit)
private typealias InstallerStatusDialogButton = @Composable (model: InstallerStatusDialogModel) -> Unit

interface InstallerModel {
    fun reinstall()
    fun install()
}

interface InstallerStatusDialogModel : InstallerModel {
    var packageInstallerStatus: Int?
}

@Composable
fun InstallerStatusDialog(model: InstallerStatusDialogModel) {
    val dialogKind = remember {
        DialogKind.fromValue(model.packageInstallerStatus!!) ?: DialogKind.FAILURE
    }

    AlertDialog(
        onDismissRequest = {
            model.packageInstallerStatus = null
        },
        confirmButton = {
            dialogKind.confirmButton(model)
        },
        dismissButton = {
            dialogKind.dismissButton?.invoke(model)
        },
        icon = {
            Icon(dialogKind.icon, null)
        },
        title = {
            Text(
                text = stringResource(dialogKind.title),
                style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(dialogKind.contentStringResId))
            }
        }
    )
}

private fun installerStatusDialogButton(
    @StringRes buttonStringResId: Int,
    buttonHandler: InstallerStatusDialogButtonHandler = { },
): InstallerStatusDialogButton = { model ->
    TextButton(
        onClick = {
            model.packageInstallerStatus = null
            buttonHandler(model)
        }
    ) {
        Text(stringResource(buttonStringResId))
    }
}

@FromValue("flag")
enum class DialogKind(
    val flag: Int,
    val title: Int,
    @StringRes val contentStringResId: Int,
    val icon: ImageVector = Icons.Outlined.ErrorOutline,
    val confirmButton: InstallerStatusDialogButton = installerStatusDialogButton(R.string.ok),
    val dismissButton: InstallerStatusDialogButton? = null,
) {
    FAILURE(
        flag = PackageInstaller.STATUS_FAILURE,
        title = R.string.installation_failed_dialog_title,
        contentStringResId = R.string.installation_failed_description,
        confirmButton = installerStatusDialogButton(R.string.install_app) { model ->
            model.install()
        }
    ),
    FAILURE_ABORTED(
        flag = PackageInstaller.STATUS_FAILURE_ABORTED,
        title = R.string.installation_cancelled_dialog_title,
        contentStringResId = R.string.installation_aborted_description,
        confirmButton = installerStatusDialogButton(R.string.install_app) { model ->
            model.install()
        }
    ),
    FAILURE_BLOCKED(
        flag = PackageInstaller.STATUS_FAILURE_BLOCKED,
        title = R.string.installation_blocked_dialog_title,
        contentStringResId = R.string.installation_blocked_description,
    ),
    FAILURE_CONFLICT(
        flag = PackageInstaller.STATUS_FAILURE_CONFLICT,
        title = R.string.installation_conflict_dialog_title,
        contentStringResId = R.string.installation_conflict_description,
        confirmButton = installerStatusDialogButton(R.string.reinstall) { model ->
            model.reinstall()
        },
        dismissButton = installerStatusDialogButton(R.string.cancel),
    ),
    FAILURE_INCOMPATIBLE(
        flag = PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
        title = R.string.installation_incompatible_dialog_title,
        contentStringResId = R.string.installation_incompatible_description,
    ),
    FAILURE_INVALID(
        flag = PackageInstaller.STATUS_FAILURE_INVALID,
        title = R.string.installation_invalid_dialog_title,
        contentStringResId = R.string.installation_invalid_description,
        confirmButton = installerStatusDialogButton(R.string.reinstall) { model ->
            model.reinstall()
        },
        dismissButton = installerStatusDialogButton(R.string.cancel),
    ),
    FAILURE_STORAGE(
        flag = PackageInstaller.STATUS_FAILURE_STORAGE,
        title = R.string.installation_storage_issue_dialog_title,
        contentStringResId = R.string.installation_storage_issue_description,
    ),

    @RequiresApi(34)
    FAILURE_TIMEOUT(
        flag = PackageInstaller.STATUS_FAILURE_TIMEOUT,
        title = R.string.installation_timeout_dialog_title,
        contentStringResId = R.string.installation_timeout_description,
        confirmButton = installerStatusDialogButton(R.string.install_app) { model ->
            model.install()
        },
    );
    // Needed due to the @FromValue annotation.
    companion object
}

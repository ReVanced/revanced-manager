package app.revanced.manager.ui.component

import android.annotation.SuppressLint
import android.content.pm.PackageInstaller
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.revanced.manager.R
import app.revanced.manager.ui.model.InstallerModel
import com.github.materiiapps.enumutil.FromValue

private typealias InstallerStatusDialogButtonHandler = ((model: InstallerModel) -> Unit)
private typealias InstallerStatusDialogButton = @Composable (model: InstallerModel, dismiss: () -> Unit) -> Unit
const val INTERNAL_STATUS_PATCHER_KILLED = -1000
const val INTERNAL_STATUS_PATCHER_CRASHED = -1001

@Composable
fun InstallerStatusDialog(installerStatus: Int, model: InstallerModel, onDismiss: () -> Unit) {
    val dialogKind = remember {
        DialogKind.fromValue(installerStatus) ?: DialogKind.FAILURE
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            dialogKind.confirmButton(model, onDismiss)
        },
        dismissButton = {
            dialogKind.dismissButton?.invoke(model, onDismiss)
        },
        title = {
            Text(
                text = stringResource(dialogKind.title),
                style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(stringResource(dialogKind.contentStringResId))
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun installerStatusDialogButton(
    @StringRes buttonStringResId: Int,
    buttonHandler: InstallerStatusDialogButtonHandler = { },
): InstallerStatusDialogButton = { model, dismiss ->
    TextButton(
        onClick = {
            dismiss()
            buttonHandler(model)
        },
        shapes = ButtonDefaults.shapes()
    ) {
        Text(stringResource(buttonStringResId))
    }
}

@FromValue("flag")
enum class DialogKind(
    val flag: Int,
    val title: Int,
    @param:StringRes val contentStringResId: Int,
    val confirmButton: InstallerStatusDialogButton = installerStatusDialogButton(R.string.ok),
    val dismissButton: InstallerStatusDialogButton? = null,
) {
    FAILURE(
        flag = PackageInstaller.STATUS_FAILURE,
        title = R.string.installation_failed_dialog_title,
        contentStringResId = R.string.installation_failed_description,
        confirmButton = installerStatusDialogButton(R.string.try_again) { it.install() },
        dismissButton = installerStatusDialogButton(R.string.cancel),
    ),
    FAILURE_BLOCKED(
        flag = PackageInstaller.STATUS_FAILURE_BLOCKED,
        title = R.string.installation_blocked_dialog_title,
        contentStringResId = R.string.installation_blocked_description,
        dismissButton = installerStatusDialogButton(R.string.cancel),
    ),
    FAILURE_CONFLICT(
        flag = PackageInstaller.STATUS_FAILURE_CONFLICT,
        title = R.string.installation_conflict_dialog_title,
        contentStringResId = R.string.installation_conflict_description,
        confirmButton = installerStatusDialogButton(R.string.reinstall) { it.reinstall() },
        dismissButton = installerStatusDialogButton(R.string.cancel),
    ),
    FAILURE_INCOMPATIBLE(
        flag = PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
        title = R.string.installation_incompatible_dialog_title,
        contentStringResId = R.string.installation_incompatible_description,
        dismissButton = installerStatusDialogButton(R.string.ok),
    ),
    FAILURE_INVALID(
        flag = PackageInstaller.STATUS_FAILURE_INVALID,
        title = R.string.installation_invalid_dialog_title,
        contentStringResId = R.string.installation_invalid_description,
        confirmButton = installerStatusDialogButton(R.string.reinstall) { it.reinstall() },
        dismissButton = installerStatusDialogButton(R.string.cancel),
    ),
    FAILURE_STORAGE(
        flag = PackageInstaller.STATUS_FAILURE_STORAGE,
        title = R.string.installation_storage_issue_dialog_title,
        contentStringResId = R.string.installation_storage_issue_description,
        dismissButton = installerStatusDialogButton(R.string.ok),
    ),
    FAILURE_TIMEOUT(
        flag = @SuppressLint("InlinedApi") PackageInstaller.STATUS_FAILURE_TIMEOUT,
        title = R.string.installation_timeout_dialog_title,
        contentStringResId = R.string.installation_timeout_description,
        confirmButton = installerStatusDialogButton(R.string.try_again) { it.install() },
        dismissButton = installerStatusDialogButton(R.string.cancel),
    ),
    PATCHER_KILLED(
        flag = INTERNAL_STATUS_PATCHER_KILLED,
        title = R.string.installation_patcher_killed_dialog_title,
        contentStringResId = R.string.installation_patcher_killed_description,
        confirmButton = installerStatusDialogButton(R.string.try_again) { it.retry() },
        dismissButton = installerStatusDialogButton(R.string.cancel),
    ),
    PATCHER_CRASHED(
        flag = INTERNAL_STATUS_PATCHER_CRASHED,
        title = R.string.installation_patcher_crashed_dialog_title,
        contentStringResId = R.string.installation_patcher_crashed_description,
        confirmButton = installerStatusDialogButton(R.string.try_again) { it.retry() },
        dismissButton = installerStatusDialogButton(R.string.cancel),
    );

    // Needed due to the @FromValue annotation.
    companion object
}

package app.revanced.manager.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.domain.manager.PreferencesManager
import org.koin.compose.koinInject

@Composable
fun NonSuggestedVersionDialog(suggestedVersion: String, onCancel: () -> Unit, onContinue: () -> Unit) {
    val prefs: PreferencesManager = koinInject()

    DangerousActionDialogBase(
        onCancel = onCancel,
        onConfirm = onContinue,
        enableConfirmCountdown = prefs.enableSuggestedVersionSafeguardCountdown,
        title = R.string.non_suggested_version_warning_title,
        body = stringResource(R.string.non_suggested_version_warning_description, suggestedVersion),
    )
}
package app.revanced.manager.ui.component.bundle

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R

@Composable
fun BundleTextContent(
    name: String,
    onNameChange: (String) -> Unit = {},
    isLocal: Boolean,
    remoteUrl: String,
    onRemoteUrlChange: (String) -> Unit = {},
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        value = name,
        onValueChange = onNameChange,
        label = {
            Text(stringResource(R.string.bundle_input_name))
        }
    )
    if (!isLocal) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            value = remoteUrl,
            onValueChange = onRemoteUrlChange,
            label = {
                Text(stringResource(R.string.bundle_input_source_url))
            }
        )
    }
}
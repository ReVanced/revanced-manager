package app.revanced.manager.ui.component.selectedapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.util.transparentListItemColors

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VersionSelectorDialog(
    selectedVersion: String?,
    availableVersions: List<String>,
    allowAnyVersion: Boolean,
    onDismissRequest: () -> Unit,
    onSelect: (String?) -> Unit
) {
    FullscreenDialog(onDismissRequest) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = stringResource(R.string.version),
                    onBackClick = onDismissRequest
                )
            }
        ) { paddingValues ->
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                if (allowAnyVersion) {
                    item(key = "any") {
                        ListItem(
                            modifier = Modifier.clickable { onSelect(null) },
                            headlineContent = { Text(stringResource(R.string.selected_app_meta_any_version)) },
                            supportingContent = if (selectedVersion == null) {
                                { Text(stringResource(R.string.this_version)) }
                            } else {
                                null
                            },
                            colors = transparentListItemColors
                        )
                    }
                }

                items(
                    items = availableVersions,
                    key = { version -> "version_$version" }
                ) { version ->
                    ListItem(
                        modifier = Modifier.clickable { onSelect(version) },
                        headlineContent = { Text(version) },
                        supportingContent = if (selectedVersion == version) {
                            { Text(stringResource(R.string.this_version)) }
                        } else {
                            null
                        },
                        colors = transparentListItemColors
                    )
                }
            }
        }
    }
}

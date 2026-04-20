package app.revanced.manager.ui.component.patches

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.patcher.patch.Option
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.TooltipIconButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OptionsDialog(
    patch: PatchInfo,
    values: Map<String, Any?>?,
    reset: () -> Unit,
    set: (String, Any?) -> Unit,
    onDismissRequest: () -> Unit,
    selectionWarningEnabled: Boolean,
    readOnly: Boolean
) = FullscreenDialog(onDismissRequest = onDismissRequest) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = patch.name,
                onBackClick = onDismissRequest,
                actions = {
                    if (!readOnly) {
                        TooltipIconButton(
                            onClick = reset,
                            tooltip = stringResource(R.string.reset)
                        ) {
                            Icon(Icons.Filled.Restore, stringResource(R.string.reset))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumnWithScrollbar(
            modifier = Modifier.padding(paddingValues)
        ) {
            if (patch.options == null) return@LazyColumnWithScrollbar

            items(patch.options, key = { it.name }) { option ->
                val name = option.name
                val value =
                    if (values == null || !values.contains(name)) option.default else values[name]

                @Suppress("UNCHECKED_CAST")
                OptionItem(
                    option = option as Option<Any>,
                    value = value,
                    setValue = {
                        set(name, it)
                    },
                    selectionWarningEnabled = selectionWarningEnabled,
                    readOnly = readOnly
                )
            }
        }
    }
}

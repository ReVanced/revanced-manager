package app.revanced.manager.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchView(
    query: String,
    onQueryChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    placeholder: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = SearchBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dividerColor = MaterialTheme.colorScheme.outline,
        inputFieldColors = SearchBarDefaults.inputFieldColors()
    )
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {
                    keyboardController?.hide()
                },
                expanded = true,
                onExpandedChange = onActiveChange,
                placeholder = placeholder,
                leadingIcon = {
                    IconButton(onClick = { onActiveChange(false) }, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.back)
                        )
                    }
                },
                trailingIcon = {
                    Row {
                        trailingContent?.invoke()

                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }, shapes = IconButtonDefaults.shapes()) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.clear)
                                )
                            }
                        }
                    }
                }
            )
        },
        expanded = true,
        onExpandedChange = onActiveChange,
        modifier = Modifier.focusRequester(focusRequester),
        colors = colors,
        content = content
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
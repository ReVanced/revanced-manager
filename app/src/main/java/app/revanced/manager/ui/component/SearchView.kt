package app.revanced.manager.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchView(
    query: String,
    onQueryChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    placeholder: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = {
            keyboardController?.hide()
        },
        active = true,
        onActiveChange = onActiveChange,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester),
        placeholder = placeholder,
        leadingIcon = {
            IconButton({ onActiveChange(false) }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(R.string.back)
                )
            }
        },
        content = content
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
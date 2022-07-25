package app.revanced.manager.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun AppBar(
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    navigationIcon: @Composable () -> Unit = {}
) {
    SmallTopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions
    )
}

@Preview(name = "Top App Bar Preview")
@Composable
fun AppBarPreview() {
    AppBar(
        title = { Text("ReVanced Manager") },
    )
}
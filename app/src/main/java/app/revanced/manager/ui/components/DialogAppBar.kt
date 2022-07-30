package app.revanced.manager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun DialogAppBar(title: String, onClick: () -> Unit) {
    SmallTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onClick) {
                androidx.compose.material3.Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Return"
                )
            }
        }
    )
}

@Preview(name = "Top App Bar Preview")
@Composable
fun DialogAppBarPreview() {
    //DialogAppBar("Select an app!")
}
package app.revanced.manager.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import app.revanced.manager.R


@Composable
fun Navigator() {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Dashboard", "Patcher")
    val icons = listOf("Dashboard", "Patcher")

    NavigationBar() {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(R.drawable.ic_baseline_dashboard_24, contentDescription = null) },
                label = { Text(item) },
                alwaysShowLabel = false,
                selected = selectedItem == index,
                onClick = { selectedItem = index }
            )
        }
    }
}

@Preview
@Composable
fun NavigatorPreview() {
    Navigator()
}
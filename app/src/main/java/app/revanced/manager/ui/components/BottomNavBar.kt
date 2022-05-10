package app.revanced.manager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import app.revanced.manager.R

@Composable
fun BottomNavBar() {
    var selectedItem by remember { mutableStateOf("") }
    val items = mapOf("Dashboard" to R.drawable.ic_baseline_dashboard_24, "Patcher" to R.drawable.ic_baseline_build_24)

    NavigationBar {
        for ((name_, drawable_) in items.entries) {
            val name = name_
            val drawable = drawable_
            NavigationBarItem(
                icon = { Icon(drawable, contentDescription = null) },
                label = { Text(name) },
                alwaysShowLabel = false,
                selected = selectedItem == name,
                onClick = { selectedItem = name }
            )
        }
    }
}

@Preview
@Composable
fun BottomNavBarPreview() {
    BottomNavBar()
}
package app.revanced.manager.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.revanced.manager.ui.components.placeholders.Icon

@Composable
fun BottomNavBar(screenName: String, items: Map<String, Int>, onNavigateClick: (screenName: String) -> Unit) {
    NavigationBar {
        for ((name, drawable) in items.entries) {
            NavigationBarItem(
                icon = { Icon(drawable, contentDescription = null) },
                label = { Text(name) },
                alwaysShowLabel = false,
                selected = screenName == name,
                onClick = { onNavigateClick(name) }
            )
        }
    }
}

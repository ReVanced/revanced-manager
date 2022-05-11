package app.revanced.manager.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BottomNavBar(screenName: String, items: Map<String, Int>, onNavigateClick: (screenName: String) -> Unit) {
    NavigationBar {
        for ((name_, drawable_) in items.entries) {
            val name = name_
            val drawable = drawable_
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

@Preview
@Composable
fun BottomNavBarPreview() {
    //BottomNavBar()
}
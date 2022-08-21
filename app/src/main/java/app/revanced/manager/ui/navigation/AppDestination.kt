package app.revanced.manager.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import app.revanced.manager.R
import com.xinto.taxi.Destination
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
sealed interface AppDestination : Destination {
    @Parcelize
    object Dashboard : AppDestination
}

@Parcelize
enum class DashboardDestination(
    val icon: @RawValue ImageVector,
    @StringRes val label: Int
) : Destination {
    DASHBOARD(Icons.Default.Dashboard, R.string.dashboard),
    PATCHER(Icons.Default.Build, R.string.patcher),
    SETTINGS(Icons.Default.Settings, R.string.settings),
}
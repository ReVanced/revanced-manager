package app.revanced.manager.ui.screens

import androidx.annotation.StringRes
import app.revanced.manager.R
import app.revanced.manager.ui.screens.destinations.DashboardSubscreenDestination
import app.revanced.manager.ui.screens.destinations.MoreSubscreenDestination
import app.revanced.manager.ui.screens.destinations.PatcherSubscreenDestination
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

enum class MainScreenDestinations(
    val direction: DirectionDestinationSpec,
    val icon: Int,
    @StringRes val label: Int
) {
    Dashboard(
        DashboardSubscreenDestination,
        R.drawable.ic_baseline_dashboard_24,
        R.string.navigation_dashboard
    ),
    Patcher(
        PatcherSubscreenDestination,
        R.drawable.ic_baseline_build_24,
        R.string.navigation_patcher
    ),
    More(
        MoreSubscreenDestination,
        R.drawable.ic_baseline_more_horiz_24,
        R.string.navigation_more
    ),
}
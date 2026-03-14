package app.revanced.manager.ui.screen.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.onboarding.OnboardingAppList
import app.revanced.manager.util.AppInfo

@Composable
fun AppsStepContent(
    modifier: Modifier = Modifier,
    apps: List<AppInfo>?,
    suggestedVersions: Map<String, String?>,
    onAppClick: (String) -> Unit
) {
    apps?.let {
        OnboardingAppList(
            modifier = modifier,
            apps = apps,
            suggestedVersions = suggestedVersions,
            onAppClick = onAppClick
        )
    } ?: Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator()
    }
}
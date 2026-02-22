package app.revanced.manager.ui.screen.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.onboarding.OnboardingAppList
import app.revanced.manager.util.AppInfo

@Composable
fun AppsStepContent(
    apps: List<AppInfo>?,
    suggestedVersions: Map<String, String?>,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    showSubtitle: Boolean = true
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (showSubtitle) {
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (apps == null) {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        } else {
            OnboardingAppList(
                apps = apps,
                suggestedVersions = suggestedVersions,
                onAppClick = onAppClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

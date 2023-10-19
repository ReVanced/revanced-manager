package app.revanced.manager.ui.component

import android.content.pm.PackageInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppInfo(appInfo: PackageInfo?, placeholderLabel: String? = null, extraContent: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(
            appInfo,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 5.dp)
        )

        AppLabel(
            appInfo,
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            defaultText = placeholderLabel
        )

        extraContent()
    }
}
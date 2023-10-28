package app.revanced.manager.ui.screen.settings.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.Markdown
import app.revanced.manager.ui.viewmodel.ChangelogsViewModel
import app.revanced.manager.util.formatNumber
import app.revanced.manager.util.relativeTime
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogsScreen(
    onBackClick: () -> Unit,
    vm: ChangelogsViewModel = getViewModel()
) {
    val changelogs = vm.changelogs
    val lastChangelog = changelogs.lastOrNull()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.changelog),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (changelogs.isEmpty()) Arrangement.Center else Arrangement.Top
        ) {
            if (changelogs.isNotEmpty()) {
                items(
                    changelogs,
                    key = { it.version }
                ) { changelog ->
                    Column(modifier = Modifier.padding(16.dp)) {
                        Changelog(
                            markdown = changelog.body,
                            version = changelog.version,
                            downloadCount = changelog.downloadCount.formatNumber(),
                            publishDate = changelog.publishDate.relativeTime()
                        )
                        if (changelog != lastChangelog) {
                            Divider(
                                modifier = Modifier.padding(top = 32.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "No changelogs found",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun Changelog(
    markdown: String,
    version: String,
    downloadCount: String,
    publishDate: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Campaign,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(32.dp)
            )
            Text(
                version.removePrefix("v"),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight(800)),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Tag(
                Icons.Outlined.Sell,
                version
            )
            Tag(
                Icons.Outlined.FileDownload,
                downloadCount
            )
            Tag(
                Icons.Outlined.CalendarToday,
                publishDate
            )
        }
    }
    Markdown(
        markdown,
    )
}

@Composable
private fun Tag(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
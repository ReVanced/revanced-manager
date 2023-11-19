package app.revanced.manager.ui.screen.settings.update


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.settings.Changelog
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
            verticalArrangement = if (changelogs.isNullOrEmpty()) Arrangement.Center else Arrangement.Top
        ) {
            if (changelogs == null) {
                item {
                    LoadingIndicator()
                }
            } else if (changelogs.isEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.no_changelogs_found),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            } else {
                val lastChangelog = changelogs.last()
                items(
                    changelogs,
                    key = { it.version }
                ) { changelog ->
                    ChangelogItem(changelog, lastChangelog)
                }
            }
        }
    }
}

@Composable
fun ChangelogItem(
    changelog: ChangelogsViewModel.Changelog,
    lastChangelog: ChangelogsViewModel.Changelog
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Changelog(
            markdown = changelog.body.replace("`", ""),
            version = changelog.version,
            downloadCount = changelog.downloadCount.formatNumber(),
            publishDate = changelog.publishDate.relativeTime(LocalContext.current)
        )
        if (changelog != lastChangelog) {
            Divider(
                modifier = Modifier.padding(top = 32.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
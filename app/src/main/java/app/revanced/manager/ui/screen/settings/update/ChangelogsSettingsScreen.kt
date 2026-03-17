package app.revanced.manager.ui.screen.settings.update

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.settings.Changelog
import app.revanced.manager.ui.viewmodel.ChangelogUiState
import app.revanced.manager.ui.viewmodel.ChangelogsViewModel
import app.revanced.manager.util.relativeTime
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogsSettingsScreen(
    onBackClick: () -> Unit,
    vm: ChangelogsViewModel = koinViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            AppTopBar(
                title = {},
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = vm.state) {
                is ChangelogUiState.Loading -> LoadingIndicator()

                is ChangelogUiState.Error -> Text(
                    text = state.message,
                    style = MaterialTheme.typography.titleLarge
                )

                is ChangelogUiState.Success -> {
                    if (state.changelogs.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_changelogs_found),
                            style = MaterialTheme.typography.titleLarge
                        )
                    } else {
                        LazyColumnWithScrollbar(modifier = Modifier.fillMaxSize()) {
                            items(
                                items = state.changelogs,
                                key = { it.version }
                            ) { changelog ->
                                ChangelogItem(
                                    changelog = changelog,
                                    showDivider = changelog != state.changelogs.last()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogItem(
    changelog: ReVancedAsset,
    showDivider: Boolean
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Changelog(
            description = changelog.description,
            version = changelog.version,
            publishDate = changelog.createdAt.relativeTime(LocalContext.current)
        )
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 32.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
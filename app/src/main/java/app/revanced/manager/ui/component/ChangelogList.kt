package app.revanced.manager.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedAssetHistory
import app.revanced.manager.util.relativeTime

sealed interface ChangelogUiState {
    data object Loading : ChangelogUiState
    data class Error(val error: String) : ChangelogUiState
    data class Success(
        val changelogs: List<ReVancedAssetHistory>,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false,
    ) : ChangelogUiState
}

@Composable
fun ChangelogList(
    state: ChangelogUiState,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            val canScroll = listState.canScrollForward || listState.canScrollBackward

            (lastVisible >= total - 2 || !canScroll) && total > 0
        }
    }

    LaunchedEffect(shouldLoadMore, state) {
        if (shouldLoadMore) onLoadMore()
    }

    Box(
        modifier = modifier.then(Modifier.fillMaxSize()),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is ChangelogUiState.Loading -> LoadingIndicator()

            is ChangelogUiState.Error -> Text(
                text = state.error,
                style = MaterialTheme.typography.titleLarge
            )

            is ChangelogUiState.Success -> {
                if (state.changelogs.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_changelogs_found),
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {
                    LazyColumnWithScrollbar(
                        modifier = Modifier.fillMaxSize(),
                        state = listState
                    ) {
                        items(
                            items = state.changelogs,
                            key = { it.version }
                        ) { changelog ->
                            ChangelogItem(
                                changelog = changelog,
                                showDivider = changelog != state.changelogs.last()
                            )
                        }

                        if (state.isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
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
    changelog: ReVancedAssetHistory,
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

@Composable
fun Changelog(
    description: String,
    version: String,
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
                Icons.Outlined.CalendarToday,
                publishDate
            )
        }
        Markdown(
            description.removeVersionHeaderIfMatches(version),
        )
    }
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

fun String.removeVersionHeaderIfMatches(version: String): String {
    val firstNewlineIndex = indexOf('\n')
    if (firstNewlineIndex == -1) return this

    val firstLine = substring(0, firstNewlineIndex).trim()
    val versionWithoutPrefix = version.removePrefix("v")

    if (!firstLine.contains(versionWithoutPrefix)) return this

    return substring(firstNewlineIndex + 1).trimStart()
}
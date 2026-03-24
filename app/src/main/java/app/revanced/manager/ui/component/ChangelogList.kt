package app.revanced.manager.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedAssetHistory
import app.revanced.manager.util.relativeTime

@Composable
fun ChangelogList(
    changelogs: LazyPagingItems<ReVancedAssetHistory>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            changelogs.loadState.refresh is LoadState.Loading -> LoadingIndicator()

            changelogs.loadState.refresh is LoadState.Error -> {
                val error = changelogs.loadState.refresh as LoadState.Error
                Text(
                    text = error.error.message ?: stringResource(R.string.changelog_download_fail),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            changelogs.itemCount == 0 -> Text(
                text = stringResource(R.string.no_changelogs_found),
                style = MaterialTheme.typography.titleLarge
            )

            else -> {
                val listState = rememberLazyListState()

                LazyColumnWithScrollbar(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = contentPadding
                ) {
                    items(
                        count = changelogs.itemCount,
                        key = { changelogs.peek(it)?.version ?: it }
                    ) { index ->
                        changelogs[index]?.let { changelog ->
                            ChangelogItem(
                                changelog = changelog,
                                showDivider = index < changelogs.itemCount - 1
                            )
                        }
                    }

                    if (changelogs.loadState.append is LoadState.Loading) {
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
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Campaign,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    version.removePrefix("v"),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight(800)
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier)

                Text(
                    "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )

                Text(
                    publishDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Markdown(
            description.removeVersionHeaderIfMatches(version),
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
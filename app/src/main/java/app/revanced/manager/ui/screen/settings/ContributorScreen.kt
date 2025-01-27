package app.revanced.manager.ui.screen.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedContributor
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.viewmodel.ContributorViewModel
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributorScreen(
    onBackClick: () -> Unit,
    viewModel: ContributorViewModel = koinViewModel()
) {
    val repositories = viewModel.repositories
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.contributors),
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        LazyColumnWithScrollbar(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = if (repositories.isNullOrEmpty()) Arrangement.Center else Arrangement.spacedBy(
                24.dp
            )
        ) {
            repositories?.let { repositories ->
                if (repositories.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(id = R.string.no_contributors_found),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                } else {
                    items(
                        items = repositories,
                        key = { it.name }
                    ) {
                        ContributorsCard(
                            title = it.name,
                            contributors = it.contributors
                        )
                    }
                }
            } ?: item { LoadingIndicator() }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContributorsCard(
    title: String,
    contributors: List<ReVancedContributor>,
    itemsPerPage: Int = 12,
    numberOfRows: Int = 2
) {
    val itemsPerRow = (itemsPerPage / numberOfRows)

    // Create a list of contributors grouped by itemsPerPage
    val contributorsByPage = remember(itemsPerPage, contributors) {
        contributors.chunked(itemsPerPage)
    }
    val pagerState = rememberPagerState { contributorsByPage.size }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "(${(pagerState.currentPage + 1)}/${pagerState.pageCount})",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = true,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                BoxWithConstraints {
                    val spaceBetween = 16.dp
                    val maxWidth = this.maxWidth
                    val itemSize = (maxWidth - (itemsPerRow - 1) * spaceBetween) / itemsPerRow
                    val itemSpacing = (maxWidth - itemSize * 6) / (itemsPerRow - 1)
                    FlowRow(
                        maxItemsInEachRow = itemsPerRow,
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        contributorsByPage[page].forEach {
                            if (itemSize > 100.dp) {
                                Row(
                                    modifier = Modifier.width(itemSize - 1.dp), // we delete 1.dp to account for not-so divisible numbers
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AsyncImage(
                                        model = it.avatarUrl,
                                        contentDescription = it.avatarUrl,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size((itemSize / 3).coerceAtMost(40.dp))
                                            .clip(CircleShape)
                                    )
                                    Text(
                                        text = it.username,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier.width(itemSize - 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = it.avatarUrl,
                                        contentDescription = it.avatarUrl,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(size = (itemSize - 1.dp).coerceAtMost(50.dp)) // we delete 1.dp to account for not-so divisible numbers
                                            .clip(CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
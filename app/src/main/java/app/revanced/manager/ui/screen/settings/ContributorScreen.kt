package app.revanced.manager.ui.screen.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedContributor
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.viewmodel.ContributorViewModel
import coil.compose.AsyncImage
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributorScreen(
    onBackClick: () -> Unit,
    viewModel: ContributorViewModel = getViewModel()
) {
    val repositories = viewModel.repositories
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.contributors),
                onBackClick = onBackClick
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (repositories.isEmpty()) {
                LoadingIndicator()
            }
            repositories.forEach {
                ExpandableListCard(
                    title = it.name,
                    contributors = it.contributors
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ExpandableListCard(
    title: String,
    contributors: List<ReVancedContributor>,
    itemsPerPage: Int = 12,
    numberOfRows: Int = 2
) {
    val itemsPerRow = (itemsPerPage / numberOfRows)

    // Create a list of contributors grouped by itemsPerPage
    val contributorsByPage = contributors.chunked(itemsPerPage)
    val pagerState = rememberPagerState { contributorsByPage.size }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(),
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
                    modifier = Modifier.weight(1f),
                    text = processHeadlineText(title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "(${(pagerState.currentPage + 1)}/${pagerState.pageCount})",
                    style = MaterialTheme.typography.labelSmall
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
                                    modifier = Modifier.width(itemSize - 1.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AsyncImage(
                                        model = it.avatarUrl,
                                        contentDescription = it.avatarUrl,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(itemSize / 3)
                                            .clip(CircleShape)
                                    )
                                    Text(
                                        modifier = Modifier.basicMarquee(100),
                                        text = it.username,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = it.avatarUrl,
                                    contentDescription = it.avatarUrl,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(itemSize - 1.dp)
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

fun processHeadlineText(repositoryName: String): String {
    return "ReVanced " + repositoryName.replace("revanced/revanced-", "")
        .replace("-", " ")
        .split(" ")
        .map { if (it.length > 3) it else it.uppercase() }
        .joinToString(" ")
        .replaceFirstChar { it.uppercase() }
}
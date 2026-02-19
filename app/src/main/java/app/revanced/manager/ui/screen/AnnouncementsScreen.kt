package app.revanced.manager.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedAnnouncement
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.viewmodel.AnnouncementsViewModel
import app.revanced.manager.util.relativeTime
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    onBackClick: () -> Unit,
    onAnnouncementClick: (ReVancedAnnouncement) -> Unit,
    vm: AnnouncementsViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showFilterSheet by remember { mutableStateOf(false) }

    if (showFilterSheet) {
        FilterBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            tags = vm.tags ?: emptyList(),
            selectedTags = vm.selectedTags
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.announcements)) },
                onBackClick = onBackClick,
                actions = {
                    if (vm.tags != null) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                imageVector = Icons.Outlined.FilterAlt,
                                contentDescription = stringResource(R.string.announcements_filter_tag)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = if (vm.announcements.isNullOrEmpty()) Arrangement.Center else Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            vm.announcements?.let { repositories ->
                if (repositories.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(id = R.string.no_announcements_found),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                } else {
                    itemsIndexed(
                        items = repositories,
                        key = { _, announcement ->
                            announcement.id
                        }
                    ) { i, announcement ->
                        if (i != 0) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        AnnouncementCard(
                            modifier = Modifier
                                .fillMaxWidth(),
                            onClick = {
                                vm.markUnreadAnnouncementRead(announcement.id)
                                onAnnouncementClick(announcement)
                            },
                            title = announcement.title,
                            date = announcement.createdAt.relativeTime(LocalContext.current),
                            author = announcement.author,
                            content = announcement.content,
                            unread = announcement.id !in vm.readAnnouncements
                        )
                    }
                }
            } ?: item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    LoadingIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    onDismissRequest: () -> Unit,
    tags: List<String>,
    selectedTags: SnapshotStateList<String>
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.announcements_filter_tag),
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    val selected = selectedTags.contains(tag)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) {
                                selectedTags.remove(tag)
                            } else {
                                selectedTags.add(tag)
                            }
                        },
                        label = { Text(tag) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnouncementCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    title: String,
    date: String,
    author: String,
    content: String,
    unread: Boolean
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(if (unread) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unread) FontWeight.ExtraBold else null
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$date • $author",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (unread) FontWeight.ExtraBold else null
                    )
                    if (unread) {
                        Badge(modifier = Modifier.size(6.dp))
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
        // TODO add announcement summary
//        val textColor = MaterialTheme.colorScheme.onSurface
//        val linkColor = MaterialTheme.colorScheme.primary
//        AndroidView(
//            factory = {
//                WebView(it).apply {
//                    setBackgroundColor(0)
//                    isVerticalScrollBarEnabled = false
//                    isHorizontalScrollBarEnabled = false
//                    isLongClickable = false
//                    setOnLongClickListener { true }
//                    isHapticFeedbackEnabled = false
//
//                    // Disable WebView's internal scrolling
//                    @SuppressLint("ClickableViewAccessibility")
//                    setOnTouchListener { _, event ->
//                        event.action == MotionEvent.ACTION_MOVE
//                    }
//                }
//            },
//            update = {
//                @Language("HTML")
//                val body = """
//                  <html>
//                    <head>
//                      <meta name="viewport" content="width=device-width, initial-scale=1" />
//                      <style>
//                        * {
//                          font-size: 12px;
//                          font-weight: normal;
//                        }
//                        body {
//                          margin: 0;
//                          padding: 0;
//                          color: ${textColor.toCss()};
//                          overflow: hidden;
//                          display: -webkit-box;
//                          -webkit-box-orient: vertical;
//                          -webkit-line-clamp: 3;
//                          text-overflow: ellipsis;
//                        }
//                        a {
//                          color: ${linkColor.toCss()};
//                        }
//                      </style>
//                    </head>
//                    <body>
//                      $content
//                    </body>
//                  </html>
//                """.trimIndent()
//
//                it.loadData(body, "text/html", "UTF-8")
//            },
//            onReset = {},
//            onRelease = { it.destroy() }
//        )
    }
}

//private fun Color.toCss(): String {
//    return "rgba(${red * 255f}, ${green * 255f}, ${blue * 255f}, $alpha)"
//}
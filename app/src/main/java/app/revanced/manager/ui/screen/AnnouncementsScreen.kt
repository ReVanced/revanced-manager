package app.revanced.manager.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedAnnouncement
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.AnnouncementsViewModel
import app.revanced.manager.util.relativeTime
import app.revanced.manager.util.withHapticFeedback
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnnouncementsScreen(
    onBackClick: () -> Unit,
    onAnnouncementClick: (ReVancedAnnouncement) -> Unit,
    vm: AnnouncementsViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    val tags by vm.tags.collectAsStateWithLifecycle(null)
    val selectedTags by vm.selectedTags.getAsState()
    val announcementSections by vm.announcementSections.collectAsStateWithLifecycle(null)

    if (showFilterSheet) {
        FilterBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            tags = tags.orEmpty(),
            selectedTags = selectedTags,
            onReset = vm::resetTagSelection,
            changeSelection = vm::changeTagSelection
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.announcements)) },
                onBackClick = onBackClick,
                actions = {
                    if (tags != null) {
                        TooltipIconButton(
                            onClick = { showFilterSheet = true },
                            tooltip = stringResource(R.string.announcements_filter_tag)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FilterAlt,
                                contentDescription = stringResource(R.string.announcements_filter_tag)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        val readAnnouncements by vm.readAnnouncements.getAsState()
        LazyColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = if (announcementSections?.isEmpty != false) {
                Arrangement.Center
            } else {
                Arrangement.spacedBy(8.dp)
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            announcementSections?.let { sections ->
                if (sections.isEmpty) {
                    item {
                        Text(
                            text = stringResource(id = R.string.no_announcements_found),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                } else {
                    val activeAnnouncements = sections.activeAnnouncements
                    val archivedAnnouncements = sections.archivedAnnouncements

                    if (activeAnnouncements.isNotEmpty()) {
                        item {
                            ListSection {
                                activeAnnouncements.forEach { announcement ->
                                    AnnouncementListItem(
                                        onClick = {
                                            vm.markAnnouncementRead(announcement.id)
                                            onAnnouncementClick(announcement)
                                        },
                                        title = announcement.title,
                                        date = announcement.createdAt.toLocalDateTime(TimeZone.UTC)
                                            .relativeTime(LocalContext.current),
                                        author = announcement.author,
                                        tags = announcement.tags,
                                        unread = announcement.id !in readAnnouncements,
                                        archived = false
                                    )
                                }
                            }
                        }
                    }

                    if (archivedAnnouncements.isNotEmpty()) {
                        item {
                            ArchivedAnnouncementsHeader(
                                expanded = archivedExpanded,
                                onToggle = { archivedExpanded = !archivedExpanded },
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                    }

                    if (archivedAnnouncements.isNotEmpty() && archivedExpanded) {
                        item {
                            ListSection {
                                archivedAnnouncements.forEach { announcement ->
                                    AnnouncementListItem(
                                        onClick = {
                                            vm.markAnnouncementRead(announcement.id)
                                            onAnnouncementClick(announcement)
                                        },
                                        title = announcement.title,
                                        date = announcement.createdAt.toLocalDateTime(TimeZone.UTC)
                                            .relativeTime(LocalContext.current),
                                        author = announcement.author,
                                        tags = announcement.tags,
                                        unread = announcement.id !in readAnnouncements,
                                        archived = true
                                    )
                                }
                            }
                        }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FilterBottomSheet(
    onDismissRequest: () -> Unit,
    tags: Set<String>,
    selectedTags: Set<String>,
    onReset: () -> Unit,
    changeSelection: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.announcements_filter_tag),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    FilterChip(
                        selected = tag in selectedTags,
                        onClick = {
                            changeSelection(tag)
                        }.withHapticFeedback(HapticFeedbackConstantsCompat.CONFIRM),
                        label = { Text(tag) }
                    )
                }
            }

            TextButton(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 16.dp),
                onClick = onReset,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.reset))
            }
        }
    }
}

@Composable
private fun ArchivedAnnouncementsHeader(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else 180f,
        label = "archivedChevronRotation"
    )
    Row(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .padding(end = 8.dp)
        )
        Text(
            text = stringResource(R.string.announcements_show_archived),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = if (expanded) stringResource(R.string.collapse_content) else stringResource(
                R.string.expand_content
            ),
            modifier = Modifier.rotate(rotation)
        )
    }
}

@Composable
private fun AnnouncementListItem(
    onClick: () -> Unit,
    title: String,
    date: String,
    author: String,
    tags: List<String>,
    unread: Boolean,
    archived: Boolean
) {
    SettingsListItem(
        onClick = onClick,
        headlineContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1500,
                        initialDelayMillis = 2500,
                        spacing = MarqueeSpacing.fractionOfContainer(1f / 5f),
                        velocity = 55.dp,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (unread) FontWeight.Bold else null,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val dot = "\u2022"          // •
                    val emSpace = "\u2002"      // en space, roughly half character width
                    val separator = "$emSpace$dot$emSpace"
                    Text("$date$separator$author",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                    if (unread) {
                        Badge(modifier = Modifier.size(6.dp))
                    }
                }
                AnnouncementTag(
                    tags = tags,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
    )
}

//private fun Color.toCss(): String {
//    return "rgba(${red * 255f}, ${green * 255f}, ${blue * 255f}, $alpha)"
//}

@Composable
fun AnnouncementTag(
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    if (tags.isEmpty()) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tags.forEach { tag ->
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
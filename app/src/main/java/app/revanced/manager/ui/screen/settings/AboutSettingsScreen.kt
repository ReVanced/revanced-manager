package app.revanced.manager.ui.screen.settings

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.revanced.manager.BuildConfig
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedSocial
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.model.navigation.Settings
import app.revanced.manager.ui.viewmodel.AboutViewModel
import app.revanced.manager.ui.viewmodel.AboutViewModel.Companion.DEVELOPER_OPTIONS_TAPS
import app.revanced.manager.ui.viewmodel.AboutViewModel.Companion.getSocialIcon
import app.revanced.manager.util.openUrl
import app.revanced.manager.util.toast
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutSettingsScreen(
    onBackClick: () -> Unit,
    navigate: (Settings.Destination) -> Unit,
    viewModel: AboutViewModel = koinViewModel()
) {
    val context = LocalContext.current
    // painterResource() is broken on release builds for some reason.
    val icon = rememberDrawablePainter(drawable = remember {
        AppCompatResources.getDrawable(context, R.drawable.ic_logo_ring)
    })

    val (preferredSocials, socials) = remember(viewModel.socials) {
        viewModel.socials.partition(ReVancedSocial::preferred)
    }

    val preferredSocialButtons = remember(preferredSocials, viewModel.donate, viewModel.contact) {
        preferredSocials.map {
            Triple(
                getSocialIcon(it.name),
                it.name,
                third = {
                    context.openUrl(it.url)
                }
            )
        } + listOfNotNull(
            viewModel.donate?.let {
                Triple(
                    Icons.Outlined.FavoriteBorder,
                    context.getString(R.string.donate),
                    third = {
                        context.openUrl(it)
                    }
                )
            },
            viewModel.contact?.let {
                Triple(
                    Icons.Outlined.MailOutline,
                    context.getString(R.string.contact),
                    third = {
                        context.openUrl("mailto:$it")
                    }
                )
            }
        )
    }

    val socialButtons = remember(socials) {
        socials.map {
            Triple(
                getSocialIcon(it.name),
                it.name,
                third = {
                    context.openUrl(it.url)
                }
            )
        }
    }

    val listItems = listOfNotNull(
        Triple(
            stringResource(R.string.submit_feedback),
            stringResource(R.string.submit_feedback_description),
            third = {
                context.openUrl("https://github.com/ReVanced/revanced-manager/issues/new/choose")
            }),
        Triple(
            stringResource(R.string.contributors),
            stringResource(R.string.contributors_description),
            third = nav@{
                if (!viewModel.isConnected) {
                    context.toast(context.getString(R.string.no_network_toast))
                    return@nav
                }

                navigate(Settings.Contributors)
            }
        ),
        Triple(
            stringResource(R.string.opensource_licenses),
            stringResource(R.string.opensource_licenses_description),
            third = { navigate(Settings.Licenses) }
        )
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    val showDeveloperSettings by viewModel.showDeveloperSettings.getAsState()
    var developerTaps by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(developerTaps) {
        if (developerTaps == 0) return@LaunchedEffect
        if (showDeveloperSettings) {
            snackbarHostState.showSnackbar(context.getString(R.string.developer_options_already_enabled))
            developerTaps = 0
            return@LaunchedEffect
        }

        val remaining = DEVELOPER_OPTIONS_TAPS - developerTaps
        if (remaining > 0) {
            snackbarHostState.showSnackbar(
                context.getString(
                    R.string.developer_options_taps,
                    remaining
                ),
                duration = SnackbarDuration.Long
            )
        } else if (remaining == 0) {
            viewModel.showDeveloperSettings.update(true)
            snackbarHostState.showSnackbar(context.getString(R.string.developer_options_enabled))
        }

        // Reset the counter
        developerTaps = 0
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.about),
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clickable { developerTaps += 1 },
                painter = icon,
                contentDescription = stringResource(R.string.app_name)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics {
                        // Icon already has this information for the purpose of being clickable.
                        hideFromAccessibility()
                    }
                )
                Text(
                    text = stringResource(R.string.version) + " " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                preferredSocialButtons.forEach { (icon, text, onClick) ->
                    FilledTonalButton(
                        onClick = onClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                socialButtons.forEach { (icon, text, onClick) ->
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Icon(
                            icon,
                            contentDescription = text,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            OutlinedCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_revanced_manager),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.revanced_manager_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column {
                listItems.forEach { (title, description, onClick) ->
                    SettingsListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClick() },
                        headlineContent = title,
                        supportingContent = description
                    )
                }
            }
        }
    }
}

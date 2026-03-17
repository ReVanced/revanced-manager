package app.revanced.manager.ui.screen.settings.update

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.ui.component.BottomContentBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.ui.component.settings.BooleanItem
import app.revanced.manager.ui.component.settings.SafeguardBooleanItem
import app.revanced.manager.ui.viewmodel.UpdatesSettingsViewModel
import app.revanced.manager.util.toast
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdatesSettingsScreen(
    onBackClick: () -> Unit,
    onChangelogClick: () -> Unit,
    onUpdateClick: () -> Unit,
    vm: UpdatesSettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    var checkingForUpdate by remember { mutableStateOf(false) }
    val availableUpdate by vm.availableManagerUpdate.collectAsStateWithLifecycle()
    val scrollState = androidx.compose.foundation.rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        canScroll = {
            scrollState.canScrollBackward || scrollState.canScrollForward
        }
    )
    val appIcon = rememberDrawablePainter(
        drawable = remember(context) {
            AppCompatResources.getDrawable(context, R.drawable.ic_logo_ring)
        }
    )

    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.updates)) },
                navigationIcon = {
                    TooltipIconButton(
                        onClick = onBackClick,
                        tooltip = stringResource(R.string.back)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            BottomContentBar(modifier = Modifier.navigationBarsPadding()) {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !checkingForUpdate,
                    onClick = {
                        coroutineScope.launch {
                            if (availableUpdate != null) {
                                onUpdateClick()
                                return@launch
                            }

                            if (!vm.isConnected) {
                                context.toast(resources.getString(R.string.no_network_toast))
                                return@launch
                            }
                            checkingForUpdate = true
                            try {
                                val version = vm.checkForUpdates()
                                if (!version.isNullOrEmpty()) onUpdateClick()
                            } finally {
                                checkingForUpdate = false
                            }
                        }
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    if (checkingForUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Update,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            when {
                                checkingForUpdate -> R.string.update_check
                                availableUpdate != null -> R.string.view_update
                                else -> R.string.manual_update_check
                            }
                        )
                    )
                }
            }
        },
        modifier = Modifier.then(
            scrollBehavior.let { Modifier.nestedScroll(it.nestedScrollConnection) }
        ),
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = scrollState
        ) {
            ListSection(
                title = stringResource(R.string.manager),
                leadingContent = { Icon(Icons.Outlined.WorkOutline, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = appIcon,
                                contentDescription = stringResource(R.string.app_name),
                                modifier = Modifier
                                    .size(42.dp)
                                    .padding(start = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                if (!vm.isConnected) {
                                    context.toast(resources.getString(R.string.no_network_toast))
                                } else {
                                    onChangelogClick()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth(),
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(text = stringResource(R.string.changelog))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ListSection {
                val managerAutoUpdates by vm.managerAutoUpdates.getAsState()

                BooleanItem(
                    preference = vm.managerAutoUpdates,
                    headline = R.string.update_checking_manager,
                    description = R.string.update_checking_manager_description
                )

                AnimatedVisibility(visible = managerAutoUpdates) {
                    BooleanItem(
                        preference = vm.showManagerUpdateDialogOnLaunch,
                        headline = R.string.show_manager_update_dialog_on_launch,
                        description = R.string.show_manager_update_dialog_on_launch_description
                    )
                }

                SafeguardBooleanItem(
                    preference = vm.useManagerPrereleases,
                    headline = R.string.manager_prereleases,
                    description = R.string.manager_prereleases_description,
                    dialogTitle = R.string.prerelease_title,
                    confirmationText = R.string.prereleases_warning,
                    onValueChange = { value ->
                        coroutineScope.launch {
                            vm.useManagerPrereleases.update(value)
                            vm.clearAvailableManagerUpdate()
                        }
                    }
                )
            }
        }
    }
}
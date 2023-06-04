package app.revanced.manager.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.patcher.worker.StepGroup
import app.revanced.manager.patcher.worker.StepStatus
import app.revanced.manager.ui.component.AppScaffold
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.viewmodel.InstallerViewModel
import app.revanced.manager.util.APK_MIMETYPE
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallerScreen(
    onBackClick: () -> Unit,
    vm: InstallerViewModel
) {
    val exportApkLauncher = rememberLauncherForActivityResult(CreateDocument(APK_MIMETYPE), vm::export)

    AppScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.installer),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.HelpOutline, stringResource(R.string.help))
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.MoreVert, stringResource(R.string.more))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            vm.stepGroups.forEach {
                InstallGroup(it)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Button(
                    onClick = { exportApkLauncher.launch("${vm.packageName}.apk") },
                    enabled = vm.canInstall
                ) {
                    Text(stringResource(R.string.export_app))
                }

                Button(
                    onClick = vm::installApk,
                    enabled = vm.canInstall
                ) {
                    Text(stringResource(R.string.install_app))
                }
            }
        }
    }
}

// Credits: https://github.com/Aliucord/AliucordManager/blob/main/app/src/main/kotlin/com/aliucord/manager/ui/component/installer/InstallGroup.kt

@Composable
fun InstallGroup(group: StepGroup) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .run {
                if (expanded) {
                    background(MaterialTheme.colorScheme.secondaryContainer)
                } else this
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
                .run { if (expanded) {
                    background(MaterialTheme.colorScheme.secondaryContainer)
                } else
                    background(MaterialTheme.colorScheme.surface)
                }
        ) {
            StepIcon(group.status, 24.dp)

            Text(text = stringResource(group.name), style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = { expanded = !expanded }) {
                if (expanded) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "collapse"
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "expand"
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background.copy(0.6f))
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(start = 4.dp)
            ) {
                group.steps.forEach {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        StepIcon(it.status, size = 18.dp)

                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, true),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepIcon(status: StepStatus, size: Dp) {
    val strokeWidth = Dp(floor(size.value / 10) + 1)

    when (status) {
        StepStatus.COMPLETED -> Icon(
            Icons.Filled.CheckCircle,
            contentDescription = "success",
            tint = MaterialTheme.colorScheme.surfaceTint,
            modifier = Modifier.size(size)
        )

        StepStatus.FAILURE -> Icon(
            Icons.Filled.Cancel,
            contentDescription = "failed",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(size)
        )

        StepStatus.WAITING -> CircularProgressIndicator(
            strokeWidth = strokeWidth,
            modifier = Modifier
                .size(size)
                .semantics {
                    contentDescription = "waiting"
                }
        )
    }
}

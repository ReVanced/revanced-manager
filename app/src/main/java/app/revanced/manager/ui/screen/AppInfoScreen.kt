package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.component.AppLabel
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.SegmentedButton
import app.revanced.manager.ui.viewmodel.AppInfoViewModel
import app.revanced.manager.util.PatchesSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(
    onPatchClick: (packageName: String, patchesSelection: PatchesSelection) -> Unit,
    onBackClick: () -> Unit,
    viewModel: AppInfoViewModel
) {
    SideEffect {
        viewModel.onBackClick = onBackClick
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_info),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppIcon(
                    viewModel.appInfo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 5.dp)
                )

                AppLabel(
                    viewModel.appInfo,
                    style = MaterialTheme.typography.titleLarge,
                    defaultText = null
                )

                Text(viewModel.installedApp.version, style = MaterialTheme.typography.bodySmall)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                SegmentedButton(
                    icon = Icons.Outlined.OpenInNew,
                    text = stringResource(R.string.open_app),
                    onClick = viewModel::launch
                )

                SegmentedButton(
                    icon = Icons.Outlined.Delete,
                    text = stringResource(R.string.uninstall),
                    onClick = viewModel::uninstall
                )

                SegmentedButton(
                    icon = Icons.Outlined.Update,
                    text = stringResource(R.string.repatch),
                    onClick = {
                        viewModel.appliedPatches?.let {
                            onPatchClick(viewModel.installedApp.originalPackageName, it)
                        }
                    }
                )
            }

            Column(
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                ListItem(
                    modifier = Modifier.clickable {  },
                    headlineContent = { Text(stringResource(R.string.applied_patches)) },
                    supportingContent = {
                        Text(
                            (viewModel.appliedPatches?.values?.sumOf { it.size } ?: 0).let {
                                pluralStringResource(
                                    id = R.plurals.applied_patches,
                                    it,
                                    it
                                )
                            }
                        )
                    },
                    trailingContent = { Icon(Icons.Filled.ArrowRight, contentDescription = stringResource(R.string.view_applied_patches)) }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.package_name)) },
                    supportingContent = { Text(viewModel.installedApp.currentPackageName) }
                )

                if (viewModel.installedApp.originalPackageName != viewModel.installedApp.currentPackageName) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.original_package_name)) },
                        supportingContent = { Text(viewModel.installedApp.originalPackageName) }
                    )
                }

                ListItem(
                    headlineContent = { Text(stringResource(R.string.install_type)) },
                    supportingContent = { Text(stringResource(viewModel.installedApp.installType.stringResource)) }
                )
            }

        }
    }
}
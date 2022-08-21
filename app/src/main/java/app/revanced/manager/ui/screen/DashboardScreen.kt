package app.revanced.manager.ui.screen

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.sp
import app.revanced.manager.R
import app.revanced.manager.ui.component.ApplicationItem
import app.revanced.manager.ui.component.HeadlineWithCard
import app.revanced.manager.ui.viewmodel.DashboardViewModel
import org.koin.androidx.compose.getViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = getViewModel()) {
    val context = LocalContext.current
    val padHoriz = 16.dp
    val padVert = 10.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .verticalScroll(state = rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HeadlineWithCard(R.string.updates) {
            Row(
                modifier = Modifier
                    .padding(horizontal = padHoriz, vertical = padVert)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    CommitDate(
                        label = R.string.patcher,
                        date = viewModel.patcherCommitDate
                    )
                    CommitDate(
                        label = R.string.manager,
                        date = viewModel.managerCommitDate
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        enabled = false, // needs update
                        onClick = {
                            Toast.makeText(context, "Already up-to-date!", Toast.LENGTH_SHORT)
                                .show()
                        },
                    ) { Text(stringResource(R.string.update_manager)) }
                    Text(
                        text = "No updates available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        HeadlineWithCard(R.string.patched_apps) {
            Row(
                modifier = Modifier
                    .padding(horizontal = padHoriz, vertical = padVert)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    val amount = 2 // TODO
                    Text(
                        text = "${stringResource(R.string.updates_available)}: $amount",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Button(
                    enabled = true, // needs update
                    onClick = {
                        Toast.makeText(context, "Already up-to-date!", Toast.LENGTH_SHORT).show()
                    }
                ) { Text(stringResource(R.string.update_all)) }
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = padHoriz)
                    .padding(bottom = padVert)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ApplicationItem(
                    name = "ReVanced",
                    released = "Released 2 days ago",
                    icon = { Icon(Icons.Default.Dashboard, "ReVanced") }
                ) {
                    ChangelogText(
                        """
                            fix: aaaaaa
                            fix: aaaaaa
                            fix: aaaaaa
                            fix: aaaaaa
                            fix: aaaaaa
                        """.trimIndent()
                    )
                }
                ApplicationItem(
                    name = "ReReddit",
                    released = "Released 1 month ago",
                    icon = { Icon(Icons.Default.Build, "ReReddit") }
                ) {
                    ChangelogText(
                        """
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                            fix: bbbbbb
                        """.trimIndent()
                    )
                }
            }
        }
    }
}

@Composable
fun CommitDate(@StringRes label: Int, date: String) {
    Row {
        Text(
            text = "${stringResource(label)}: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ChangelogText(text: String) {
    Column {
        Text(
            text = "Changelog",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
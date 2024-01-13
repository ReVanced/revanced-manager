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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.BuildConfig
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedSocial
import app.revanced.manager.ui.viewmodel.AboutViewModel
import app.revanced.manager.ui.viewmodel.AboutViewModel.Companion.getSocialIcon
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.util.isDebuggable
import app.revanced.manager.util.openUrl
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutSettingsScreen(
    onBackClick: () -> Unit,
    onContributorsClick: () -> Unit,
    onLicensesClick: () -> Unit,
    viewModel: AboutViewModel = getViewModel()
) {
    val context = LocalContext.current
    // painterResource() is broken on release builds for some reason.
    val icon = rememberDrawablePainter(drawable = remember {
        AppCompatResources.getDrawable(context, R.drawable.ic_logo_ring)
    })

    val (preferred, notPreferred) = viewModel.socials.partition(ReVancedSocial::preferred)

    val filledButtons = remember(preferred) {
        preferred.map {
            Triple(getSocialIcon(it.name), it.name, third = {
                context.openUrl(it.url)
            })
        }
    }.toMutableList()

    viewModel.donate.value?.let {
        filledButtons.add(
            Triple(
                Icons.Outlined.FavoriteBorder,
                stringResource(R.string.donate),
                third = {
                    context.openUrl(viewModel.donate.value!!)
                }
            )
        )
    }

    viewModel.contact.value?.let {
        filledButtons.add(
            Triple(
                Icons.Outlined.MailOutline,
                stringResource(R.string.contact),
                third = {
                    context.openUrl(viewModel.contact.value!!)
                }
            )
        )
    }

    val socialButtons = remember(notPreferred) {
        notPreferred.map {
            Pair(getSocialIcon(it.name)) {
                context.openUrl(it.url)
            }
        }
    }

    val listItems = listOfNotNull(
        Triple(stringResource(R.string.submit_feedback),
            stringResource(R.string.submit_feedback_description),
            third = {
                context.openUrl("https://github.com/ReVanced/revanced-manager/issues/new/choose")
            }),
        Triple(
            stringResource(R.string.contributors),
            stringResource(R.string.contributors_description),
            third = onContributorsClick
        ),
        Triple(stringResource(R.string.developer_options),
            stringResource(R.string.developer_options_description),
            third = { /*TODO*/ }).takeIf { context.isDebuggable },
        Triple(
            stringResource(R.string.opensource_licenses),
            stringResource(R.string.opensource_licenses_description),
            third = onLicensesClick
        )
    )

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.about),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                modifier = Modifier.padding(top = 16.dp),
                painter = icon,
                contentDescription = null
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.version) + " " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                filledButtons.forEach { (icon, text, onClick) ->
                    FilledTonalButton(
                        onClick = onClick,
                        modifier = Modifier.defaultMinSize(minWidth = 130.dp),
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
                FlowRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    socialButtons.forEach { (icon, onClick) ->
                        IconButton(
                            onClick = onClick,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
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

package app.revanced.manager.ui.screen.settings

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.BuildConfig
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedSocial
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.viewmodel.AboutViewModel
import app.revanced.manager.util.isDebuggable
import app.revanced.manager.util.openUrl
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Brands
import compose.icons.fontawesomeicons.brands.Discord
import compose.icons.fontawesomeicons.brands.Github
import compose.icons.fontawesomeicons.brands.Reddit
import compose.icons.fontawesomeicons.brands.Telegram
import compose.icons.fontawesomeicons.brands.Twitter
import compose.icons.fontawesomeicons.brands.Youtube
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

    val socialIcons = mapOf(
        "Contact" to Icons.Outlined.MailOutline,
        "Discord" to FontAwesomeIcons.Brands.Discord, 
        "Donate" to Icons.Outlined.FavoriteBorder,
        "GitHub" to FontAwesomeIcons.Brands.Github,
        "Reddit" to FontAwesomeIcons.Brands.Reddit,
        "Telegram" to FontAwesomeIcons.Brands.Telegram,
        "Twitter" to FontAwesomeIcons.Brands.Twitter,
        "X" to FontAwesomeIcons.Brands.Twitter,
        "YouTube" to FontAwesomeIcons.Brands.Youtube,
    )

    val outlinedButton = viewModel.socials.filter(ReVancedSocial::preferred).map {
        Triple(socialIcons[it.name] ?: Icons.Outlined.Language, it.name, third = {
            context.openUrl(it.url)
        })
    }

    val socialButtons = viewModel.socials.filterNot(ReVancedSocial::preferred).map {
        Pair(socialIcons[it.name] ?: Icons.Outlined.Language) {
            context.openUrl(it.url)
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
        ).takeIf { context.isDebuggable },
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Image(painter = icon, contentDescription = null)
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.version) + " " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")",
                    style = MaterialTheme.typography.bodyMedium
                )

                FlowRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                ) {
                    outlinedButton.forEach { (icon, text, onClick) ->
                        Button(
                            onClick = onClick,
                            modifier = Modifier.padding(end = 8.dp),

                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            border = ButtonDefaults.outlinedButtonBorder
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .padding(end = 8.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                FlowRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    socialButtons.forEach { (icon, onClick) ->
                        IconButton(
                            onClick = onClick,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.about_revanced_manager),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.revanced_manager_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            }

            listItems.forEach { (title, description, onClick) ->
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { onClick() },
                    headlineContent = { Text(title, style = MaterialTheme.typography.titleLarge) },
                    supportingContent = {
                        Text(
                            description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                )
            }
        }
    }
}

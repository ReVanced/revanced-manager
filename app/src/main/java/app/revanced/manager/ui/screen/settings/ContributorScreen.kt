package app.revanced.manager.ui.screen.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedContributor
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ArrowButton
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
            if(repositories.isEmpty()) {
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandableListCard(
    title: String,
    contributors: List<ReVancedContributor>
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(30.dp),
        elevation = CardDefaults.outlinedCardElevation(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.outlinedCardColors(),
    ) {
        Column() {
            Row() {
                ListItem(
                    headlineContent = {
                        Text(
                            text = processHeadlineText(title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    trailingContent = {
                        if (contributors.isNotEmpty()) {
                            ArrowButton(
                                expanded = expanded,
                                onClick = { expanded = !expanded }
                            )
                        }
                    },
                )
            }
            if (expanded) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(8.dp),
                ) {
                    contributors.forEach {
                        AsyncImage(
                            model = it.avatarUrl,
                            contentDescription = it.avatarUrl,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(45.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }
    }
}
fun processHeadlineText(repositoryName: String): String {
    return "Revanced " + repositoryName.replace("revanced/revanced-", "")
        .replace("-", " ")
        .split(" ")
        .map { if (it.length > 3) it else it.uppercase() }
        .joinToString(" ")
        .replaceFirstChar { it.uppercase() }
}
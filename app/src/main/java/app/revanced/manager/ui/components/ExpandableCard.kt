package app.revanced.manager.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.revanced.manager.ui.models.ContributorsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import app.revanced.manager.R
import coil.compose.AsyncImage


private const val tag = "Expandable Card"


@Composable
@ExperimentalMaterial3Api
fun ExpandableCard(
    title: String,
    vm: ContributorsViewModel = viewModel()
) {
    var expandedState by remember { mutableStateOf(false) }
    val rotateState by animateFloatAsState(targetValue = if (expandedState) 180f else 0f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(state = rememberScrollState())
            .padding(8.dp)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = LinearOutSlowInEasing
                )
            ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$title",
                    modifier = Modifier.weight(6f),
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    modifier = Modifier
                        .alpha(ContentAlpha.medium)
                        .weight(1f)
                        .rotate(rotateState),
                    onClick = {
                        expandedState = !expandedState
                    }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown"
                    )
                }
            }
            if (expandedState) {
                if(vm.contributorsList.isNotEmpty()) {
                    var currentUriHandler = LocalUriHandler.current

//                    for(contributor in vm.contributorsList) {
//                         Row(
//                             Modifier
//                                 .fillMaxWidth()) {
//                             AsyncImage(
//                                 model = contributor.avatar_url,
//                                 contentDescription = stringResource(id = R.string.contributor_image),
//                                 Modifier
//                                     .size(40.dp)
//                                     .clip(CircleShape)
//                                     .clickable {
//                                         currentUriHandler.openUri(contributor.url)
//                                     }
//                             )
//                             Text(text = contributor.login)
//
//                         }
//                    }

                  Box(Modifier.height(100.dp)){
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(48.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(vm.contributorsList) {
                            contributor -> AsyncImage(
                                 model = contributor.avatar_url,
                                 contentDescription = stringResource(id = R.string.contributor_image),
                                 Modifier
                                     .size(40.dp)
                                     .clip(CircleShape)
                                     .clickable {
                                         currentUriHandler.openUri(contributor.url)
                                     }
                             )
                        }
                    }
                  }
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
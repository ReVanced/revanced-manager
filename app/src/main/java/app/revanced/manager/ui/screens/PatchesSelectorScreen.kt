package app.revanced.manager.ui.screens

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.revanced.manager.R
import app.revanced.manager.ui.Resource
import app.revanced.manager.ui.components.LoadingIndicator
import app.revanced.manager.ui.screens.mainsubscreens.PatchClass
import app.revanced.manager.ui.screens.mainsubscreens.PatcherViewModel
import app.revanced.patcher.extensions.PatchExtensions.description
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.extensions.PatchExtensions.version
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("QueryPermissionsNeeded")
@Destination
@RootNavGraph
@Composable
fun PatchesSelectorScreen(
    navigator: NavController, // TODO: add back button
    pvm: PatcherViewModel = viewModel(LocalContext.current as ComponentActivity)
) {
    val patches = rememberSaveable { pvm.getFilteredPatches() }
    val patchesState by pvm.patches

    when (patchesState) {
        is Resource.Success -> {
            Scaffold { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    LazyColumn {
                        items(count = patches.size) {
                            val patch = patches[it]
                            val name = patch.patch.patchName
                            PatchSelectable(patch, pvm.isPatchSelected(name)) {
                                pvm.selectPatch(name, !pvm.isPatchSelected(name))
                            }
                        }
                    }
                }
            }
        }
        else -> LoadingIndicator(R.string.loading_fetching_patches)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchSelectable(patchClass: PatchClass, isSelected: Boolean, onSelected: () -> Unit) {
    val patch = patchClass.patch
    val name = patch.patchName

    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        onClick = { onSelected() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // TODO: get these 2 little shits to align properly!
                    // also figure out a way to limit the length of the name, so it doesn't overflow
                    Text(
                        name + name + name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    SecondaryText(
                        patch.version ?: "unknown",
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    if (patchClass.unsupported) {
                        SecondaryText(
                            "Unsupported!", // get some yellow warning icon here
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelected() }
                    )
                }
            }
            patch.description?.let { desc ->
                SecondaryText(
                    desc,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SecondaryText(text: String, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        ProvideTextStyle(MaterialTheme.typography.body2) {
            Text(
                text,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                modifier = modifier,
                fontWeight = FontWeight.Light
            )
        }
    }
}
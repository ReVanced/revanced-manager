package app.revanced.manager.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.ui.component.bundle.BundleItem
import app.revanced.manager.ui.viewmodel.BundlesViewModel
import org.koin.androidx.compose.getViewModel

@Composable
fun BundlesScreen(
    vm: BundlesViewModel = getViewModel(),
) {
    val sources by vm.sources.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        sources.forEach {
            BundleItem(
                bundle = it,
                onDelete = {
                    vm.delete(it)
                },
                onUpdate = {
                    vm.update(it)
                }
            )
        }
    }
}
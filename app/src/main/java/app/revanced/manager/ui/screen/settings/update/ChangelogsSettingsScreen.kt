package app.revanced.manager.ui.screen.settings.update

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import app.revanced.manager.domain.repository.ChangelogSource
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ChangelogList
import app.revanced.manager.ui.viewmodel.ChangelogsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogsSettingsScreen(
    source: ChangelogSource,
    onBackClick: () -> Unit,
    vm: ChangelogsViewModel = koinViewModel { parametersOf(source) }
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            AppTopBar(
                title = {},
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        ChangelogList(modifier = Modifier.padding(paddingValues), state = vm.state, onLoadMore =vm::loadNextPage)
    }
}


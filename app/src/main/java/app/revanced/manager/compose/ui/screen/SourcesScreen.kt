package app.revanced.manager.compose.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import app.revanced.manager.compose.R

@Composable
fun SourcesScreen() {
    Box(Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.no_sources_set),
            fontSize = 24.sp,
            modifier = Modifier
                .align(alignment = Alignment.Center)
        )
    }
}
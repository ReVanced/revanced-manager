package app.revanced.manager.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    text: String? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        text?.let { Text(text) }

        progress?.let {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.padding(vertical = 16.dp).then(modifier)
            )
        } ?:
            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 16.dp).then(modifier)
            )
    }
}
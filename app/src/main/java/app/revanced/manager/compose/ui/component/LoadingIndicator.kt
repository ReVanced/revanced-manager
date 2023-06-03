package app.revanced.manager.compose.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun LoadingIndicator(progress: Float? = null, text: Int? = null) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (text != null)
            Text(stringResource(text))
        if (progress == null) {
            CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
        } else {
            CircularProgressIndicator(progress = progress, modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}
package app.revanced.manager.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R

@Composable
fun LogoHeader() {
    Box {
        Icon(
            painterResource(id = R.drawable.ic_revanced),
            contentDescription = "Header Icon",
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(32.dp)
                .size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }

    Divider(Modifier.alpha(.5f))
}
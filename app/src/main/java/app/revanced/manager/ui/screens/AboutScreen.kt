package app.revanced.manager.ui.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.IconButton
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import app.revanced.manager.R
import app.revanced.manager.Global.Companion.socialLinks
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import app.revanced.manager.ui.components.ExpandableCard
import app.revanced.manager.ui.components.PreferenceRow

private const val tag = "AboutScreen"


@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph
@Composable
fun AboutScreen(
    //navigator: NavController,
) {
    Column(Modifier.padding(8.dp)) {
//        Box() {
//            Text(
//                text = "ReVanced Manager",
//                style = MaterialTheme.typography.headlineMedium,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .align(Alignment.TopCenter)
//                    .padding(20.dp,0.dp,0.dp,12.dp),
//                textAlign = TextAlign.Center
//            )
//        }
        Image(
            painterResource(R.drawable.revancedtext),
            contentDescription = "ReVanced Logo",
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )
        Divider(Modifier.alpha(.5f))

        ExpandableCard("Patcher Credits")

        var currentUriHandler = LocalUriHandler.current

        PreferenceRow(
            title = stringResource(R.string.whats_new),
            onClick = { currentUriHandler.openUri("https://revanced.app") },
        )

        PreferenceRow(
            title = stringResource(R.string.help_translate),
            onClick = { currentUriHandler.openUri("https://revanced.app") },
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            for ((social_ic, uri) in socialLinks.entries) {
                IconButton(onClick = { currentUriHandler.openUri(uri) }) {
                    Icon(painter = painterResource(social_ic), contentDescription = "Links")
                }
            }
        }
    }
}


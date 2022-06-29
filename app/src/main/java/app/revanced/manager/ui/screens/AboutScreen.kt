package app.revanced.manager.ui.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
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

private const val tag = "AboutScreen"

val socialLinks = mapOf(
    "https://revanced.app/" to R.drawable.ic_web,
    "https://revanced.app/discord" to R.drawable.ic_discord_24,
    "https://revanced.app/github" to R.drawable.ic_github_24,
    "https://twitter.com/@revancedapp" to R.drawable.ic_twitter,
    "https://youtube.com/channel/UCLktAUh5Gza9zAJBStwxNdw" to R.drawable.ic_youtube,
    "https://reddit.com/r/revancedapp" to R.drawable.ic_reddit,
    )

@OptIn(ExperimentalMaterialApi::class)
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

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            for ((_name, drawble_) in socialLinks.entries) {
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(painter = painterResource(drawble_), contentDescription = "Links")
                }
            }
        }
    }
    


}
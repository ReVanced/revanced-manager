package app.revanced.manager.ui.screens


import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.IconButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.revanced.manager.Global
import app.revanced.manager.backend.api.GitHubAPI
import app.revanced.manager.ui.components.ExpandableCard
import app.revanced.manager.ui.components.PreferenceRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val tag = "AboutScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph
@Composable
fun AboutScreen(
//    navigator: NavController,
    vm: AboutViewModel = viewModel()
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
            onClick = { currentUriHandler.openUri("https://revanced.app") }
        )

        Text(
            text = vm.contributorName,
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

class AboutViewModel : ViewModel() {
    private val tag = "AboutViewModel"

    private var _fetchContributorName : GitHubAPI.Contributors.Contributor? by mutableStateOf(null)
        val contributorName: String
            get() = _fetchContributorName?.login ?: "Null"

    private var _fetchContributorAvatar : GitHubAPI.Contributors.Contributor? by mutableStateOf(null)
    val contributorAvatar: String
        get() = _fetchContributorAvatar?.login ?: "Null"

    private var _fetchContributorProfile : GitHubAPI.Contributors.Contributor? by mutableStateOf(null)
    val contributorProfile: String
        get() = _fetchContributorProfile?.login ?: "Null"

    init {
        fetchContributors()
    }
    private fun fetchContributors() {
        viewModelScope.launch {
            try {
                _fetchContributorName = GitHubAPI.Contributors.contributors("revanced", "revanced-patches")
            } catch (e: Exception) {
                Log.e(tag, "failed to fetch contributor names", e)
            }
            try {
                _fetchContributorName = GitHubAPI.Contributors.contributors("revanced", "revanced-patches")
            } catch (e: Exception) {
                Log.e(tag, "failed to fetch latest contributor names", e)
            }
        }
    }
}
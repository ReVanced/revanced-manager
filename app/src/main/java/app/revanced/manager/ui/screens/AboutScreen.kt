package app.revanced.manager.ui.screens


import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.IconButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.revanced.manager.Global.Companion.websiteUrl
import app.revanced.manager.backend.api.GitHubAPI
import app.revanced.manager.ui.components.ExpandableCard
import app.revanced.manager.ui.components.PreferenceRow
import kotlinx.coroutines.launch

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
        Box() {
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

        ExpandableCard("Patcher Credits")

        var currentUriHandler = LocalUriHandler.current

        PreferenceRow(
            title = stringResource(R.string.whats_new),
            onClick = { currentUriHandler.openUri(websiteUrl) },
        )

        PreferenceRow(
            title = stringResource(R.string.help_translate),
            onClick = { currentUriHandler.openUri(websiteUrl) }
        )

//        Text(
//            text = vm.contributorName,
//        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            for ((social_ic, uri) in socialLinks.entries) {
                IconButton(onClick = { currentUriHandler.openUri(uri) }) {
                    Icon(painter = painterResource(social_ic), contentDescription = "Links", tint = MaterialTheme.colorScheme.primary)
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
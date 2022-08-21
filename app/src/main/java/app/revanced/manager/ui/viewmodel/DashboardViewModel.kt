package app.revanced.manager.ui.viewmodel

import android.text.format.DateUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import app.revanced.manager.repository.GitHubRepository
import app.revanced.manager.util.ghManager
import app.revanced.manager.util.ghPatcher
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(private val repository: GitHubRepository) : ViewModel() {
    var patcherCommitDate by mutableStateOf("")
        private set
    var managerCommitDate by mutableStateOf("")
        private set

    init {
        runBlocking {
            patcherCommitDate = commitDateOf(ghPatcher)
            managerCommitDate = commitDateOf(ghManager)
        }
    }

    private suspend fun commitDateOf(repo: String, ref: String = "HEAD"): String {
        val commit = repository.getLatestCommit(repo, ref).commit
        return DateUtils.getRelativeTimeSpanString(
            formatter.parse(commit.committer.date)!!.time,
            Calendar.getInstance().timeInMillis,
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }

    private companion object {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    }
}
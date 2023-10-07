package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.launch
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

class ManagerUpdateChangelogViewModel(
    private val api: ReVancedAPI,
    private val app: Application,
) : ViewModel() {
    private val markdownFlavour = GFMFlavourDescriptor()
    private val markdownParser = MarkdownParser(flavour = markdownFlavour)

    var changelog by mutableStateOf(
        Changelog(
            "...",
            app.getString(R.string.changelog_loading),
        )
    )
        private set
    val changelogHtml by derivedStateOf {
        val markdown = changelog.body
        val parsedTree = markdownParser.buildMarkdownTreeFromString(markdown)
        HtmlGenerator(markdown, parsedTree, markdownFlavour).generateHtml()
    }

    init {
        viewModelScope.launch {
            uiSafe(app, R.string.changelog_download_fail, "Failed to download changelog") {
                changelog = api.getRelease("revanced-manager").getOrThrow().let {
                    Changelog(it.metadata.tag, it.metadata.body)
                }
            }
        }
    }

    data class Changelog(
        val version: String,
        val body: String,
    )
}

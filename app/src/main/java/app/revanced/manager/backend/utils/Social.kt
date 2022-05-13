package app.revanced.manager.backend.utils

import androidx.compose.ui.platform.UriHandler
import app.revanced.manager.Global.Companion.discordUrl
import app.revanced.manager.Global.Companion.githubUrl

fun openDiscord(uriHandle: UriHandler) {
    uriHandle.openUri(discordUrl)
}

fun openGitHub(uriHandle: UriHandler) {
    uriHandle.openUri(githubUrl)
}

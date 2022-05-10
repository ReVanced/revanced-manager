package app.revanced.manager.backend.utils

import androidx.compose.ui.platform.UriHandler
import app.revanced.manager.Global.Companion.REVANCED_DISCORD
import app.revanced.manager.Global.Companion.REVANCED_GITHUB

fun OpenDiscord(uriHandle: UriHandler) {
    uriHandle.openUri(REVANCED_DISCORD)
}

fun OpenGitHub(uriHandle: UriHandler) {
    uriHandle.openUri(REVANCED_GITHUB)
}

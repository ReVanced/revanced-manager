package app.revanced.manager.backend.utils

import androidx.compose.ui.platform.UriHandler
import app.revanced.manager.Global.Companion.REVANCED_DISCORD
import app.revanced.manager.Global.Companion.REVANCED_GITHUB

fun openDiscord(uriHandle: UriHandler) {
    uriHandle.openUri(REVANCED_DISCORD)
}

fun openGitHub(uriHandle: UriHandler) {
    uriHandle.openUri(REVANCED_GITHUB)
}

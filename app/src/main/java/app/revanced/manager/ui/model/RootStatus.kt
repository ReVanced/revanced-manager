package app.revanced.manager.ui.model

import app.revanced.manager.R

enum class RootCheckResult(val displayName: Int) {
    GRANTED(R.string.generic_active),
    DENIED(R.string.generic_inactive),
    UNAVAILABLE(R.string.generic_not_available)
}
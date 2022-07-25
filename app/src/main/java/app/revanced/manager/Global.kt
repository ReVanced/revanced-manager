package app.revanced.manager

class Global {
    companion object {
        const val websiteUrl = "https://revanced.app"

        val socialLinks = mapOf(
            R.drawable.ic_web to "$websiteUrl",
            R.drawable.ic_discord to "$websiteUrl/discord",
            R.drawable.ic_github to "$websiteUrl/github",
            R.drawable.ic_twitter to "https://twitter.com/revancedapp",
            R.drawable.ic_reddit to "https://reddit.com/r/revancedapp",
            R.drawable.ic_telegram to "https://t.me/app_revanced",
            R.drawable.ic_youtube to "https://youtube.com/channel/UCLktAUh5Gza9zAJBStwxNdw",

            )

        private const val ghOrg = "revanced"
        const val ghPatches = "$ghOrg/revanced-patches"
        const val ghPatcher = "$ghOrg/revanced-patcher"
        const val ghManager = "$ghOrg/revanced-manager"
        const val ghIntegrations = "$ghOrg/revanced-integrations"

        var showBar = true
    }
}
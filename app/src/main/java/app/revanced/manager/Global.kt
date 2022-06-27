package app.revanced.manager

class Global {
    companion object {
        private const val websiteUrl = "https://revanced.app"
        const val githubUrl = "$websiteUrl/github"
        const val discordUrl = "$websiteUrl/discord"

        private const val ghOrg = "revanced"
        const val ghPatches = "$ghOrg/revanced-patches"
        const val ghPatcher = "$ghOrg/revanced-patcher"
        const val ghManager = "$ghOrg/revanced-manager"
        const val ghIntegrations = "$ghOrg/revanced-integrations"

        var showBar = true
    }
}
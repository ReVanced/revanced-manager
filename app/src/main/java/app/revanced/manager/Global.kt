package app.revanced.manager

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

class Global {
    companion object {
        private const val websiteUrl = "https://revanced.app"
        const val githubUrl = "$websiteUrl/github"
        const val discordUrl = "$websiteUrl/discord"

        val client = HttpClient(Android) {
            BrowserUserAgent()
            install(ContentNegotiation) {
                json()
            }
        }
    }
}
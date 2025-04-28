package app.revanced.manager.ui.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedDonationLink
import app.revanced.manager.network.dto.ReVancedSocial
import app.revanced.manager.network.utils.getOrNull
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Brands
import compose.icons.fontawesomeicons.brands.Discord
import compose.icons.fontawesomeicons.brands.Github
import compose.icons.fontawesomeicons.brands.Reddit
import compose.icons.fontawesomeicons.brands.Telegram
import compose.icons.fontawesomeicons.brands.XTwitter
import compose.icons.fontawesomeicons.brands.Youtube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AboutViewModel(
    private val reVancedAPI: ReVancedAPI,
    private val network: NetworkInfo,
) : ViewModel() {
    var socials by mutableStateOf(emptyList<ReVancedSocial>())
        private set
    var contact by mutableStateOf<String?>(null)
        private set
    var donate by mutableStateOf<String?>(null)
        private set
    val isConnected: Boolean
        get() = network.isConnected()

    init {
        viewModelScope.launch {
            if (!isConnected) {
                return@launch
            }
            withContext(Dispatchers.IO) {
                reVancedAPI.getInfo("https://api.revanced.app").getOrNull()
            }?.let {
                socials = it.socials
                contact = it.contact.email
                donate = it.donations.links.find(ReVancedDonationLink::preferred)?.url
            }
        }
    }

    companion object {
        private val socialIcons = mapOf(
            "Discord" to FontAwesomeIcons.Brands.Discord,
            "GitHub" to FontAwesomeIcons.Brands.Github,
            "Reddit" to FontAwesomeIcons.Brands.Reddit,
            "Telegram" to FontAwesomeIcons.Brands.Telegram,
            "Twitter" to FontAwesomeIcons.Brands.XTwitter,
            "X" to FontAwesomeIcons.Brands.XTwitter,
            "YouTube" to FontAwesomeIcons.Brands.Youtube,
        )

        fun getSocialIcon(name: String) = socialIcons[name] ?: Icons.Outlined.Language
    }
}
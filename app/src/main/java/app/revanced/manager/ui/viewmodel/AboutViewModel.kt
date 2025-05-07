package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedDonationLink
import app.revanced.manager.network.dto.ReVancedSocial
import app.revanced.manager.network.utils.getOrNull
import app.revanced.manager.util.toast
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Brands
import compose.icons.fontawesomeicons.brands.Discord
import compose.icons.fontawesomeicons.brands.Github
import compose.icons.fontawesomeicons.brands.Reddit
import compose.icons.fontawesomeicons.brands.Telegram
import compose.icons.fontawesomeicons.brands.XTwitter
import compose.icons.fontawesomeicons.brands.Youtube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AboutViewModel(
    private val app: Application,
    private val reVancedAPI: ReVancedAPI,
    private val network: NetworkInfo,
    private val prefs: PreferencesManager,
) : ViewModel() {
    var socials by mutableStateOf(emptyList<ReVancedSocial>())
        private set
    var contact by mutableStateOf<String?>(null)
        private set
    var donate by mutableStateOf<String?>(null)
        private set
    val isConnected: Boolean
        get() = network.isConnected()

    private val _developerTaps = MutableStateFlow(0)
    @OptIn(ExperimentalCoroutinesApi::class)
    val developerTaps = _developerTaps
        .flatMapLatest { taps ->
            if (taps == 0) return@flatMapLatest flowOf(0)

            // Reset after 5 seconds if the user hasn't tapped.
            flow {
                emit(taps)
                delay(5000L)
                _developerTaps.emit(0)
            }
        }
        .filter { it > 0 }
        .onEach { taps ->
            if (taps == DEVELOPER_OPTIONS_TAPS) {
                prefs.showDeveloperOptions.update(true)
                _developerTaps.emit(0)
            }
        }
        // Don't show a notification when subscribing.
        .drop(1)

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

    fun onIconTap() = viewModelScope.launch {
        if (prefs.showDeveloperOptions.get()) {
            app.toast("You are already a developer")
            return@launch
        }

        _developerTaps.value += 1
    }

    companion object {
        const val DEVELOPER_OPTIONS_TAPS = 5

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
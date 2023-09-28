package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedSocial
import app.revanced.manager.network.utils.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AboutViewModel(private val reVancedAPI: ReVancedAPI) : ViewModel() {
    val socials = mutableStateListOf<ReVancedSocial>()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { reVancedAPI.getSocials().getOrNull() }?.let(
                socials::addAll
            )
        }
    }
}
package app.revanced.manager.data.platform

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService

class NetworkInfo(app: Application) {
    private val connectivityManager = app.getSystemService<ConnectivityManager>()!!

    private fun getCapabilities() = connectivityManager.activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
    fun isConnected() = connectivityManager.activeNetwork != null
    fun isUnmetered() = getCapabilities()?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ?: true

    /**
     * Returns true if it is safe to download large files.
     */
    fun isSafe() = isConnected() && isUnmetered()
}
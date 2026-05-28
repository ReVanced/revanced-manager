package app.revanced.manager.data.platform

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import app.revanced.manager.util.tag

class NetworkInfo(private val app: Application) {
    private val connectivityManager = app.getSystemService<ConnectivityManager>()!!

    private fun hasNetworkStatePermission() = ContextCompat
        .checkSelfPermission(app, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED

    private fun getCapabilities(): NetworkCapabilities? {
        if (!hasNetworkStatePermission()) {
            Log.w(tag, "ACCESS_NETWORK_STATE not granted; assuming unmetered connection")
            return null
        }
        return try {
            connectivityManager.activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        } catch (e: SecurityException) {
            Log.w(tag, "Denied access to network state; assuming unmetered connection", e)
            null
        } catch (e: RuntimeException) {
            Log.w(tag, "Failed to query network state; assuming unmetered connection", e)
            null
        } catch (e: Exception) {
            Log.w(tag, "Unexpected error while querying network state; assuming unmetered connection", e)
            null
        }
    }

    fun isUnmetered(): Boolean {
        val capabilities = getCapabilities() ?: return true
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) return true

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)
    }
}
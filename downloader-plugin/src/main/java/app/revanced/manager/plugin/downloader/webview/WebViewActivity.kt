package app.revanced.manager.plugin.downloader.webview

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.revanced.manager.plugin.downloader.R

// TODO: use ComponentActivity instead.
class WebViewActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_webview)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val cookieManager = CookieManager.getInstance()
        findViewById<WebView>(R.id.content).apply {
            cookieManager.setAcceptCookie(true)
            // TODO: murder cookies if this is the first time setting it up.
            settings.apply {
                cacheMode = WebSettings.LOAD_NO_CACHE
                databaseEnabled = false
                allowContentAccess = true
                domStorageEnabled = false
                javaScriptEnabled = true
            }
        }
    }
}
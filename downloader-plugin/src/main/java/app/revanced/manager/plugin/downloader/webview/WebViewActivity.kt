package app.revanced.manager.plugin.downloader.webview

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import app.revanced.manager.plugin.downloader.PluginHostApi
import app.revanced.manager.plugin.downloader.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@OptIn(PluginHostApi::class)
@PluginHostApi
class WebViewActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vm by viewModels<WebViewModel>()
        enableEdgeToEdge()
        setContentView(R.layout.activity_webview)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val webView = findViewById<WebView>(R.id.webview)
        onBackPressedDispatcher.addCallback {
            if (webView.canGoBack()) webView.goBack()
            else cancelActivity()
        }

        val params = intent.getParcelableExtra<Parameters>(KEY)!!
        actionBar?.apply {
            title = params.title
            setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel)
            setDisplayHomeAsUpEnabled(true)
        }

        val events = IWebViewEvents.Stub.asInterface(params.events)!!
        vm.setup(events)

        webView.apply {
            settings.apply {
                cacheMode = WebSettings.LOAD_NO_CACHE
                allowContentAccess = false
                domStorageEnabled = true
                javaScriptEnabled = true
            }

            webViewClient = vm.webViewClient
            setDownloadListener { url, userAgent, _, mimetype, _ ->
                vm.onDownload(url, mimetype, userAgent)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.commands.collect {
                    when (it) {
                        is WebViewModel.Command.Finish -> {
                            setResult(RESULT_OK)
                            finish()
                        }

                        is WebViewModel.Command.Load -> webView.loadUrl(it.url)
                    }
                }
            }
        }
    }

    private fun cancelActivity() {
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == android.R.id.home) {
        cancelActivity()

        true
    } else super.onOptionsItemSelected(item)

    @Parcelize
    internal class Parameters(
        val title: String, val events: IBinder
    ) : Parcelable

    internal companion object {
        const val KEY = "params"
    }
}

@OptIn(PluginHostApi::class)
internal class WebViewModel : ViewModel() {
    init {
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            setAcceptCookie(true)
        }
    }

    private val commandChannel = Channel<Command>()
    val commands = commandChannel.receiveAsFlow()

    private var eventBinder: IWebViewEvents? = null
    private val ctrlBinder = object : IWebView.Stub() {
        override fun load(url: String?) {
            viewModelScope.launch {
                commandChannel.send(Command.Load(url!!))
            }
        }

        override fun finish() {
            viewModelScope.launch {
                commandChannel.send(Command.Finish)
            }
        }
    }

    val webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            eventBinder!!.pageLoad(url)
        }
    }

    fun onDownload(url: String, mimeType: String, userAgent: String) {
        eventBinder!!.download(url, mimeType, userAgent)
    }

    fun setup(binder: IWebViewEvents) {
        if (eventBinder != null) return
        eventBinder = binder
        binder.ready(ctrlBinder)
    }

    sealed interface Command {
        data class Load(val url: String) : Command
        data object Finish : Command
    }
}
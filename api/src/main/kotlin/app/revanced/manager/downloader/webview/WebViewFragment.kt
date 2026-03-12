package app.revanced.manager.downloader.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import app.revanced.manager.downloader.DownloaderHostApi
import app.revanced.manager.downloader.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@OptIn(DownloaderHostApi::class)
@DownloaderHostApi
class WebViewFragment : Fragment(R.layout.webview_fragment) {
    private val vm by viewModels<WebViewModel>()
    lateinit var webView: WebView
    private val args by lazy {
        arguments?.getParcelable<Parameters>(KEY)!!
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().apply {
            enableEdgeToEdge()
            onBackPressedDispatcher.addCallback {
                if (webView.canGoBack()) webView.goBack()
                else cancelActivity()
            }
            actionBar?.apply {
                title = args.title
                setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel)
                setDisplayHomeAsUpEnabled(true)
            }

            addMenuProvider(
                object : MenuProvider {
                    override fun onCreateMenu(
                        menu: Menu,
                        menuInflater: MenuInflater
                    ) {
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem) =
                        if (menuItem.itemId == android.R.id.home) {
                            cancelActivity()

                            true
                        } else false
                },
                this
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        println("Deez nuts")

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        webView = view.findViewById<WebView>(R.id.webview)

        val events = IWebViewEvents.Stub.asInterface(args.events)!!
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

        val activity = requireActivity()
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.commands.collect {
                    when (it) {
                        is WebViewModel.Command.Finish -> {
                            activity.setResult(Activity.RESULT_OK)
                            activity.finish()
                        }

                        is WebViewModel.Command.Load -> webView.loadUrl(it.url)
                    }
                }
            }
        }
    }

    private fun cancelActivity() {
        val activity = requireActivity()
        activity.setResult(Activity.RESULT_CANCELED)
        activity.finish()
    }

    @Parcelize
    internal class Parameters(
        val title: String, val events: IBinder
    ) : Parcelable

    internal companion object {
        const val KEY = "params"
    }
}

@OptIn(DownloaderHostApi::class)
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
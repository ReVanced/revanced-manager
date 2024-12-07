package app.revanced.manager.plugin.downloader.webview

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import app.revanced.manager.plugin.downloader.DownloaderScope
import app.revanced.manager.plugin.downloader.GetScope
import app.revanced.manager.plugin.downloader.Scope
import app.revanced.manager.plugin.downloader.downloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.net.HttpURLConnection
import java.net.URI
import kotlin.properties.Delegates

typealias InitialUrl = String
typealias PageLoadCallback<T> = suspend WebViewCallbackScope<T>.(url: String) -> Unit
typealias DownloadCallback<T> = suspend WebViewCallbackScope<T>.(url: String, mimeType: String, userAgent: String) -> Unit

@Parcelize
/**
 * A data class for storing a download
 */
data class DownloadUrl(val url: String, val userAgent: String?) : Parcelable {
    /**
     * Converts this into a [app.revanced.manager.plugin.downloader.DownloadResult].
     */
    fun toResult() = with(URI.create(url).toURL().openConnection() as HttpURLConnection) {
        useCaches = false
        allowUserInteraction = false
        userAgent?.let { setRequestProperty("User-Agent", it) }

        connectTimeout = 10_000
        connect()

        inputStream to getHeaderField("Content-Length").toLong()
    }
}

interface WebViewCallbackScope<T> : Scope {
    /**
     * Finishes the activity and returns the [result].
     */
    suspend fun finish(result: T)

    /**
     * Tells the WebView to load the specified [url].
     */
    suspend fun load(url: String)
}

class WebViewScope<T> internal constructor(
    coroutineScope: CoroutineScope,
    private val scopeImpl: Scope,
    setResult: (T) -> Unit
) : Scope by scopeImpl {
    private var onPageLoadCallback: PageLoadCallback<T> = {}
    private var onDownloadCallback: DownloadCallback<T> = { _, _, _ -> }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = Dispatchers.Default.limitedParallelism(1)
    private lateinit var webView: IWebView
    internal lateinit var initialUrl: String

    internal val binder = object : IWebViewEvents.Stub() {
        override fun ready(iface: IWebView?) {
            coroutineScope.launch(dispatcher) {
                webView = iface!!.also {
                    it.load(initialUrl)
                }
            }
        }

        override fun pageLoad(url: String?) {
            coroutineScope.launch(dispatcher) { onPageLoadCallback(callbackScope, url!!) }
        }

        override fun download(url: String?, mimetype: String?, userAgent: String?) {
            coroutineScope.launch(dispatcher) {
                onDownloadCallback(
                    callbackScope,
                    url!!,
                    mimetype!!,
                    userAgent!!
                )
            }
        }
    }

    private val callbackScope = object : WebViewCallbackScope<T>, Scope by scopeImpl {
        override suspend fun finish(result: T) {
            setResult(result)
            // Tell the WebViewActivity to finish
            webView.let { withContext(Dispatchers.IO) { it.finish() } }
        }

        override suspend fun load(url: String) {
            webView.let { withContext(Dispatchers.IO) { it.load(url) } }
        }

    }

    /**
     * Called when the WebView attempts to navigate to a downloadable file.
     */
    fun download(block: DownloadCallback<T>) {
        onDownloadCallback = block
    }

    /**
     * Called when the WebView finishes loading a page.
     */
    fun pageLoad(block: PageLoadCallback<T>) {
        onPageLoadCallback = block
    }
}

@JvmInline
private value class Container<U>(val value: U)

/**
 * Run a [android.webkit.WebView] Activity controlled by the provided code block.
 * The activity will keep running until it is cancelled or an event handler calls [WebViewCallbackScope.finish].
 *
 * @param title The string displayed in the action bar
 * @param block Defines event handlers and returns an initial URL
 */
suspend fun <T> GetScope.runWebView(
    title: String,
    block: suspend WebViewScope<T>.() -> InitialUrl
) = supervisorScope {
    var result by Delegates.notNull<Container<T>>()

    val scope = WebViewScope<T>(this@supervisorScope, this@runWebView) { result = Container(it) }
    scope.initialUrl = scope.block()

    // Start the webview activity and wait until it finishes
    requestStartActivity(Intent().apply {
        putExtras(Bundle().apply {
            putBinder(WebViewActivity.BINDER_KEY, scope.binder)
            putString(WebViewActivity.TITLE_KEY, title)
        })
        setClassName(
            hostPackageName,
            WebViewActivity::class.qualifiedName!!
        )
    })

    // Return the result and cancel any leftover coroutines.
    coroutineContext.cancelChildren()
    result.value
}

/**
 * Implement a downloader using [runWebView] and [DownloadUrl]. This function will automatically define a handler for download events unlike [runWebView].
 * Returning null inside the [block] is equivalent to returning null inside [DownloaderScope.get].
 *
 * @see runWebView
 */
fun webViewDownloader(block: suspend WebViewScope<DownloadUrl>.(packageName: String, version: String?) -> InitialUrl?) =
    downloader<DownloadUrl> {
        val label = context.applicationInfo.loadLabel(
            context.packageManager
        ).toString()

        get { packageName, version ->
            class ReturnNull : Exception()

            try {
                runWebView(label) {
                    download { url, _, userAgent -> finish(DownloadUrl(url, userAgent)) }

                    block(this@runWebView, packageName, version) ?: throw ReturnNull()
                } to version
            } catch (_: ReturnNull) {
                null
            }
        }

        download {
            it.toResult()
        }
    }
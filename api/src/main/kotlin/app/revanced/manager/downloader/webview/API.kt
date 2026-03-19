package app.revanced.manager.downloader.webview

import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import app.revanced.manager.downloader.DownloadUrl
import app.revanced.manager.downloader.DownloaderScope
import app.revanced.manager.downloader.GetScope
import app.revanced.manager.downloader.Scope
import app.revanced.manager.downloader.Downloader
import app.revanced.manager.downloader.DownloaderHostApi
import app.revanced.manager.downloader.requestStartFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

typealias InitialUrl = String
typealias PageLoadCallback<T> = suspend WebViewCallbackScope<T>.(url: String) -> Unit
typealias DownloadCallback<T> = suspend WebViewCallbackScope<T>.(url: String, mimeType: String, userAgent: String) -> Unit

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

@OptIn(DownloaderHostApi::class)
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
     * Called when the WebView attempts to download a file to disk.
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
 * The [block] defines the event handlers and returns the initial URL.
 *
 * @param title The string displayed in the action bar.
 * @param block The control block.
 */
@OptIn(DownloaderHostApi::class)
suspend fun <T> GetScope.runWebView(
    title: String,
    block: suspend WebViewScope<T>.() -> InitialUrl
) = supervisorScope {
    var result by Delegates.notNull<Container<T>>()

    val scope = WebViewScope<T>(this@supervisorScope, this@runWebView) { result = Container(it) }
    scope.initialUrl = scope.block()

    // Start the webview and wait until it finishes.
    requestStartFragment<WebViewFragment>(Bundle().apply {
        putParcelable(
            WebViewFragment.KEY,
            WebViewFragment.Parameters(title, scope.binder)
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
fun WebViewDownloader(@StringRes name: Int, block: suspend WebViewScope<DownloadUrl>.(packageName: String, version: String?) -> InitialUrl?) =
    Downloader(name) {
        val label = resources.getString(name)

        get { packageName, version ->
            class ReturnNull : Exception()

            try {
                runWebView(label) {
                    download { url, _, userAgent ->
                        finish(
                            DownloadUrl(
                                url,
                                mapOf("User-Agent" to userAgent)
                            )
                        )
                    }

                    block(this@runWebView, packageName, version) ?: throw ReturnNull()
                } to version
            } catch (_: ReturnNull) {
                null
            }
        }

        download {
            it.toDownloadResult()
        }
    }
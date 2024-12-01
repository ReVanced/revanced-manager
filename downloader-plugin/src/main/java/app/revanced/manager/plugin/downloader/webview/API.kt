package app.revanced.manager.plugin.downloader.webview

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import app.revanced.manager.plugin.downloader.DownloaderScope
import app.revanced.manager.plugin.downloader.GetResult
import app.revanced.manager.plugin.downloader.GetScope
import app.revanced.manager.plugin.downloader.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.net.HttpURLConnection
import java.net.URI
import kotlin.properties.Delegates

internal typealias PageLoadCallback<T> = suspend WebViewCallbackScope<T>.(url: String) -> Unit
internal typealias DownloadCallback<T> = suspend WebViewCallbackScope<T>.(url: String, mimeType: String, userAgent: String) -> Unit
internal typealias ReadyCallback<T> = suspend WebViewCallbackScope<T>.() -> Unit

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
    private var onReadyCallback: ReadyCallback<T> =
        { throw Exception("Ready callback not set") }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = Dispatchers.Default.limitedParallelism(1)
    private var current: IWebView? = null
    private val webView: IWebView
        inline get() = current ?: throw Exception("WebView interface unavailable")

    internal val binder = object : IWebViewEvents.Stub() {
        override fun ready(iface: IWebView?) {
            coroutineScope.launch(dispatcher) {
                val wasNull = current == null
                current = iface
                if (wasNull) onReadyCallback(callbackScope)
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

    /**
     * Called when the WebView is ready. This should always call [WebViewCallbackScope.load].
     */
    fun ready(block: ReadyCallback<T>) {
        onReadyCallback = block
    }
}

@JvmInline
private value class Container<U>(val value: U)

private suspend fun <T> GetScope.runWebView(title: String, block: WebViewScope<T>.() -> Unit) =
    coroutineScope {
        var result by Delegates.notNull<Container<T>>()

        val scope = WebViewScope<T>(this@coroutineScope, this@runWebView) { result = Container(it) }
        scope.block()

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

        result.value
    }

/**
 * Implements [DownloaderScope.get] using an [android.webkit.WebView]. Event handlers are defined in the provided [block].
 * The activity will keep running until it is cancelled or an event handler calls [WebViewCallbackScope.finish].
 *
 * @param title The title that will be shown in the WebView activity. The default value is the plugin application label.
 */
fun <T : Parcelable> DownloaderScope<T>.webView(
    title: String = context.applicationInfo.loadLabel(
        context.packageManager
    ).toString(),
    block: WebViewScope<GetResult<T>?>.(packageName: String, version: String?) -> Unit
) = get { pkgName, version ->
    runWebView(title) {
        block(pkgName, version)
    }
}
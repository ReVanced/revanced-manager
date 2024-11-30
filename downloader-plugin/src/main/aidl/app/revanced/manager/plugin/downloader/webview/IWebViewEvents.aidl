// IWebViewEvents.aidl
package app.revanced.manager.plugin.downloader.webview;

import app.revanced.manager.plugin.downloader.webview.IWebView;

oneway interface IWebViewEvents {
    void ready(IWebView iface);
    void pageLoad(String url);
    void download(String url, String mimetype, String userAgent);
}
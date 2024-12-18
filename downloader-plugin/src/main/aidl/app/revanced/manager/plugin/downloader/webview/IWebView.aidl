// IWebView.aidl
package app.revanced.manager.plugin.downloader.webview;

oneway interface IWebView {
    void load(String url);
    void finish();
}
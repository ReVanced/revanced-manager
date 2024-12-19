// IWebView.aidl
package app.revanced.manager.plugin.downloader.webview;

@JavaPassthrough(annotation="@app.revanced.manager.plugin.downloader.PluginHostApi")
oneway interface IWebView {
    void load(String url);
    void finish();
}
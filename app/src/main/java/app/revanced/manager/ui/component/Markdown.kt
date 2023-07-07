package app.revanced.manager.ui.component

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.revanced.manager.util.hexCode
import app.revanced.manager.util.openUrl
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewStateWithHTMLData

@Composable
@SuppressLint("ClickableViewAccessibility")
fun Markdown(
    text: String,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val state = rememberWebViewStateWithHTMLData(data = generateMdHtml(source = text))
    val client = remember {
        object : AccompanistWebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (request != null) ctx.openUrl(request.url.toString())
                return true
            }
        }
    }

    WebView(
        state,
        modifier = Modifier
            .background(Color.Transparent)
            .then(modifier),
        client = client,
        onCreated = {
            it.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            it.isVerticalScrollBarEnabled = false
            it.isHorizontalScrollBarEnabled = false
            it.setOnTouchListener { _, event -> event.action == MotionEvent.ACTION_MOVE }
            it.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    )
}

@Composable
fun generateMdHtml(
    source: String,
    wrap: Boolean = false,
    headingColor: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    linkColor: Color = MaterialTheme.colorScheme.primary
) = remember(source, wrap, headingColor, textColor, linkColor) {
    """<html>
                    <head>
                        <meta charset="utf-8" />
                        <title>Markdown</title>
                        <meta name="viewport" content="width=device-width; initial-scale=1.0; maximum-scale=1.0; user-scalable=0;"/>
                        <style>
                            body {
                                color: #${textColor.hexCode};
                            }
                            a {
                                color: #${linkColor.hexCode}!important;
                            }
                            a.anchor {
                                display: none;
                            }
                            .highlight pre, pre {
                                word-wrap: ${if (wrap) "break-word" else "normal"};
                                white-space: ${if (wrap) "pre-wrap" else "pre"};
                            } 
                            h2 {
                                color: #${headingColor.hexCode};
                                font-size: 18px;
                                font-weight: 500;
                                line-height: 24px;
                                letter-spacing: 0.15px;
                            }
                            ul {
                                margin-left: 0px;
                                padding-left: 18px;
                            }
                            li {
                                margin-left: 2px;
                            }
                            ::marker {
                              font-size: 16px;
                              margin-right: 8px;
                              color: #${textColor.hexCode};
                            }
                        </style> 
                    </head> 
                    <body> 
                        $source 
                    </body>
                </html>"""
}
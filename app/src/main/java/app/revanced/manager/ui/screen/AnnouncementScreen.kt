package app.revanced.manager.ui.screen

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.children
import app.revanced.manager.R
import app.revanced.manager.network.dto.ReVancedAnnouncement
import app.revanced.manager.util.relativeTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.intellij.lang.annotations.Language

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnnouncementScreen(
    onBackClick: () -> Unit,
    announcement: ReVancedAnnouncement
) {
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        canScroll = {
            scrollState.canScrollBackward || scrollState.canScrollForward
        }
    )
    val textColor = MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TwoRowsTopAppBar(
                title = { expanded ->
                    Text(
                        text = announcement.title,
                        style = if (expanded) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium
                    )
                },
                subtitle = {
                    val createDate = announcement.createdAt.toLocalDateTime(TimeZone.UTC).relativeTime(LocalContext.current)
                    Text("$createDate · ${announcement.author}")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(scrollState)
                .padding(paddingValues)
        ) {
            AnnouncementTag(
                tags = announcement.tags,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                factory = {
                    val webView = WebView(it).apply {
                        setBackgroundColor(0)
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        isLongClickable = false
                        setOnLongClickListener { true }
                        isHapticFeedbackEnabled = false

                        // Disable WebView's internal scrolling
                        @SuppressLint("ClickableViewAccessibility")
                        setOnTouchListener { _, event ->
                            event.action == MotionEvent.ACTION_MOVE
                        }
                    }
                    FrameLayout(it).apply {
                        addView(webView)
                    }
                },
                update = {
                    val webView = it.children.first() as WebView
                    @Language("HTML")
                    val style = """
                    <html>
                      <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1" />
                        <style>
                          body {
                            color: ${textColor.toCss()};
                          }
                          a {
                            color: ${linkColor.toCss()};
                          }
                        </style>
                      </head>
                      <body>
                        ${announcement.content}
                      </body>
                    </html>
                """.trimIndent()
                    webView.loadData(style, "text/html", "UTF-8")
                },
                onRelease = {
                    val webView = it.children.first() as WebView
                    webView.destroy()
                }
            )
        }
    }
}

private fun Color.toCss(): String {
    return "rgba(${red * 255f}, ${green * 255f}, ${blue * 255f}, $alpha)"
}
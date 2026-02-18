package app.revanced.manager.ui.screen

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.children
import app.revanced.manager.R
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.viewmodel.AnnouncementViewModel
import app.revanced.manager.util.relativeTime
import org.intellij.lang.annotations.Language
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnnouncementScreen(
    onBackClick: () -> Unit,
    vm: AnnouncementViewModel = koinViewModel()
) {
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        canScroll = {
            scrollState.canScrollBackward || scrollState.canScrollForward
        }
    )
    Scaffold(
        topBar = {
            TwoRowsTopAppBar(
                title = { expanded ->
                    vm.announcement?.let {
                        Text(
                            text = it.title,
                            style = if (expanded) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium
                        )
                    }
                },
                subtitle = {
                    vm.announcement?.let {
                        val createDate = it.createdAt.relativeTime(LocalContext.current)
                        Text("$createDate · ${it.author}")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
        vm.announcement?.let { announcement ->
            val textColor = MaterialTheme.colorScheme.onSurface
            val linkColor = MaterialTheme.colorScheme.primary
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 10.dp),
                factory = {
                    val webView = WebView(it).apply {
                        setBackgroundColor(0)
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        isLongClickable = false
                        setOnLongClickListener { true }
                        isHapticFeedbackEnabled = false
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

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
                }
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
    }
}

private fun Color.toCss(): String {
    return "rgba(${red * 255f}, ${green * 255f}, ${blue * 255f}, $alpha)"
}
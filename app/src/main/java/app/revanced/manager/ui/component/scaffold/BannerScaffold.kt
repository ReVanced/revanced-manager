package app.revanced.manager.ui.component.scaffold

import android.os.Build
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import app.revanced.manager.R
import app.revanced.manager.ui.component.TooltipIconButton
import kotlin.math.roundToInt

@Stable
class BannerScope internal constructor(
    val collapseFraction: Float,
    val isLandscape: Boolean,
    val contentColor: Color,
)

@Stable
class BannerScrollState(initialCollapsedFraction: Float = 0f) {
    private var collapseOffsetPx by mutableFloatStateOf(0f)
    private var collapseRangePx by mutableFloatStateOf(0f)

    var collapsedFraction by mutableFloatStateOf(initialCollapsedFraction.coerceIn(0f, 1f))
        private set

    internal var offsetLimitPx: Float
        get() = collapseRangePx
        set(value) {
            collapseRangePx = value.coerceAtLeast(0f)
            if (collapseRangePx == 0f) {
                collapseOffsetPx = 0f
                collapsedFraction = 0f
                return
            }

            collapseOffsetPx = (collapsedFraction * collapseRangePx).coerceIn(0f, collapseRangePx)
            collapsedFraction = collapseOffsetPx / collapseRangePx
        }

    internal fun consumeScrollDelta(deltaPx: Float): Float {
        if (collapseRangePx <= 0f || deltaPx == 0f) return 0f

        val previousOffset = collapseOffsetPx
        collapseOffsetPx = (collapseOffsetPx - deltaPx).coerceIn(0f, collapseRangePx)
        collapsedFraction = collapseOffsetPx / collapseRangePx

        return -(collapseOffsetPx - previousOffset)
    }
}

@Stable
class BannerScrollBehavior internal constructor(
    val state: BannerScrollState,
    private val canCollapse: () -> Boolean,
    private val dispatchSheetRawDelta: ((Float) -> Float)?,
) {
    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val consumed = if (available.y < 0f) consumeBannerDelta(available.y) else 0f
            return Offset(0f, consumed)
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            val consumedByBanner = if (available.y > 0f) consumeBannerDelta(available.y) else 0f
            return Offset(0f, consumedByBanner)
        }
    }

    internal fun consumeDirectDragDelta(deltaPx: Float): Float {
        if (deltaPx == 0f) return 0f

        return if (deltaPx < 0f) {
            val bannerConsumed = consumeBannerDelta(deltaPx)
            val remaining = deltaPx - bannerConsumed
            bannerConsumed + consumeSheetDelta(remaining)
        } else {
            val sheetConsumed = consumeSheetDelta(deltaPx)
            val remaining = deltaPx - sheetConsumed
            sheetConsumed + consumeBannerDelta(remaining)
        }
    }

    private fun consumeBannerDelta(deltaPx: Float): Float {
        if (deltaPx < 0f && !canCollapse()) return 0f
        return state.consumeScrollDelta(deltaPx)
    }

    private fun consumeSheetDelta(gestureDeltaPx: Float): Float {
        if (gestureDeltaPx == 0f) return 0f
        val consumedRaw = dispatchSheetRawDelta?.invoke(-gestureDeltaPx) ?: 0f
        return -consumedRaw
    }
}

private val BannerScrollStateSaver: Saver<BannerScrollState, Float> = Saver(
    save = { it.collapsedFraction },
    restore = { BannerScrollState(initialCollapsedFraction = it) },
)

@Composable
private fun rememberBannerScrollState(
    initialCollapsedFraction: Float = 0f,
): BannerScrollState = rememberSaveable(saver = BannerScrollStateSaver) {
    BannerScrollState(initialCollapsedFraction)
}

@Composable
fun rememberBannerScrollBehavior(
    state: BannerScrollState = rememberBannerScrollState(),
    canCollapse: () -> Boolean = { true },
): BannerScrollBehavior = remember(state, canCollapse) {
    BannerScrollBehavior(state = state, canCollapse = canCollapse, dispatchSheetRawDelta = null)
}

@Composable
fun rememberBannerScrollBehavior(
    sheetLazyListState: LazyListState,
    state: BannerScrollState = rememberBannerScrollState(),
    canCollapse: () -> Boolean = { true },
): BannerScrollBehavior {
    return remember(state, sheetLazyListState, canCollapse) {
        BannerScrollBehavior(
            state = state,
            canCollapse = {
                canCollapse() &&
                    (sheetLazyListState.canScrollForward || sheetLazyListState.canScrollBackward)
            },
            dispatchSheetRawDelta = sheetLazyListState::dispatchRawDelta,
        )
    }
}

object BannerScaffoldDefaults {
    val SheetCornerRadius: Dp = 28.dp

    @Composable
    fun colors(
        sheetBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
        bannerContentColor: Color = MaterialTheme.colorScheme.onSurface,
    ) = BannerScaffoldColors(
        sheetBackgroundColor = sheetBackgroundColor,
        bannerContentColor = bannerContentColor,
        sheetContentColor = contentColorFor(sheetBackgroundColor),
    )
}

@ConsistentCopyVisibility
@Immutable
data class BannerScaffoldColors internal constructor(
    val sheetBackgroundColor: Color,
    val bannerContentColor: Color,
    val sheetContentColor: Color,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BannerScaffold(
    modifier: Modifier = Modifier,
    title: String = "",
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    colors: BannerScaffoldColors = BannerScaffoldDefaults.colors(),
    scrollBehavior: BannerScrollBehavior? = null,
    bannerBackground: @Composable BannerScope.() -> Unit = {},
    bannerContent: @Composable BannerScope.() -> Unit,
    sheetContent: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val isLandscape = maxWidth > maxHeight
        val axisFraction = if (isLandscape) 0.5f else 0.35f
        val collapsedAxisFraction = if (isLandscape) axisFraction else 0.15f

        val topBarHeight = if (isLandscape) 0.dp else 64.dp
        val topBarPx = with(density) { topBarHeight.roundToPx() }

        val totalMainAxisPx = with(density) {
            if (isLandscape) maxWidth.roundToPx() else maxHeight.roundToPx()
        }
        val availableMainAxisPx = (totalMainAxisPx - if (isLandscape) 0 else topBarPx).coerceAtLeast(0)
        val expandedBannerPx = (availableMainAxisPx * axisFraction).roundToInt()
        val collapsedBannerPx = (availableMainAxisPx * collapsedAxisFraction)
            .roundToInt()
            .coerceAtMost(expandedBannerPx)

        val collapseFraction = if (isLandscape) 0f else scrollBehavior?.state?.collapsedFraction ?: 0f

        SideEffect {
            scrollBehavior?.state?.offsetLimitPx =
                if (isLandscape) 0f else (expandedBannerPx - collapsedBannerPx).toFloat()
        }

        val bannerPx = if (isLandscape) expandedBannerPx else {
            lerp(expandedBannerPx.toFloat(), collapsedBannerPx.toFloat(), collapseFraction).roundToInt()
        }

        val bannerSize = with(density) {
            bannerPx.coerceIn(0, availableMainAxisPx).toDp()
        }

        val cornerRadius = BannerScaffoldDefaults.SheetCornerRadius
        val cornerPx = with(density) { cornerRadius.roundToPx() }
        val backgroundExtent = with(density) {
            val maxPx = if (isLandscape) maxWidth.roundToPx() else maxHeight.roundToPx()
            val extentPx = bannerPx + cornerPx + if (isLandscape) 0 else topBarPx
            extentPx.coerceAtMost(maxPx).toDp()
        }

        val bannerScope = remember(collapseFraction, isLandscape, colors.bannerContentColor) {
            BannerScope(collapseFraction, isLandscape, colors.bannerContentColor)
        }

        val scaffoldScrollModifier = if (!isLandscape && scrollBehavior != null) {
            Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .scrollable(
                    state = rememberScrollableState { scrollBehavior.consumeDirectDragDelta(it) },
                    orientation = Orientation.Vertical,
                )
        } else {
            Modifier
        }

        Box(Modifier.fillMaxSize().then(scaffoldScrollModifier)) {
            val bannerBackgroundModifier = Modifier.align(Alignment.TopStart).then(
                if (isLandscape) {
                    Modifier.width(backgroundExtent).fillMaxHeight()
                } else {
                    Modifier.fillMaxWidth().height(backgroundExtent)
                }
            )

            CompositionLocalProvider(LocalContentColor provides colors.bannerContentColor) {
                Box(modifier = bannerBackgroundModifier) {
                    bannerScope.bannerBackground()
                }
            }

            val bannerContentModifier = Modifier.align(Alignment.TopStart).then(
                if (isLandscape) {
                    Modifier.width(bannerSize).fillMaxHeight()
                } else {
                    Modifier.padding(top = topBarHeight).fillMaxWidth().height(bannerSize)
                }
            )

            CompositionLocalProvider(LocalContentColor provides colors.bannerContentColor) {
                Box(modifier = bannerContentModifier.clipToBounds()) {
                    bannerScope.bannerContent()
                }
            }

            val sheetShape = if (isLandscape) {
                RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius)
            } else {
                RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
            }

            val sheetBackgroundModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier
                    .clip(sheetShape)
                    .background(colors.sheetBackgroundColor)
            } else {
                Modifier.background(colors.sheetBackgroundColor, sheetShape)
            }

            val sheetContainerModifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (isLandscape) bannerSize else 0.dp,
                    top = if (isLandscape) 0.dp else topBarHeight + bannerSize,
                )

            CompositionLocalProvider(
                LocalContentColor provides colors.sheetContentColor,
                LocalOverscrollFactory provides null,
            ) {
                Box(
                    modifier = sheetContainerModifier
                        .then(sheetBackgroundModifier)
                        .then(
                            if (isLandscape) {
                                Modifier.windowInsetsPadding(
                                    WindowInsets.systemBars.only(
                                        WindowInsetsSides.End + WindowInsetsSides.Vertical,
                                    )
                                )
                            } else {
                                Modifier
                            }
                        )
                ) {
                    val insetPadding = if (isLandscape) {
                        PaddingValues(start = cornerRadius)
                    } else {
                        PaddingValues(top = cornerRadius)
                    }
                    sheetContent(insetPadding)
                }
            }

            TopAppBar(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .then(if (isLandscape) Modifier.width(bannerSize) else Modifier.fillMaxWidth()),
                title = { Text(title) },
                navigationIcon = {
                    if (onBackClick != null) {
                        TooltipIconButton(
                            onClick = onBackClick,
                            tooltip = stringResource(R.string.back)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = colors.bannerContentColor,
                    navigationIconContentColor = colors.bannerContentColor,
                    actionIconContentColor = colors.bannerContentColor,
                ),
                windowInsets = WindowInsets.systemBars.only(
                    if (isLandscape) {
                        WindowInsetsSides.Top + WindowInsetsSides.Start
                    } else {
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    }
                ),
            )
        }
    }
}

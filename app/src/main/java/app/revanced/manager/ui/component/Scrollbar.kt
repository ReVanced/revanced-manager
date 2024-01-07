package app.revanced.manager.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gigamole.composescrollbars.Scrollbars
import com.gigamole.composescrollbars.ScrollbarsState
import com.gigamole.composescrollbars.config.ScrollbarsConfig
import com.gigamole.composescrollbars.config.ScrollbarsOrientation
import com.gigamole.composescrollbars.config.layercontenttype.ScrollbarsLayerContentType
import com.gigamole.composescrollbars.config.layersType.ScrollbarsLayersType
import com.gigamole.composescrollbars.config.layersType.thicknessType.ScrollbarsThicknessType
import com.gigamole.composescrollbars.config.visibilitytype.ScrollbarsVisibilityType
import com.gigamole.composescrollbars.scrolltype.ScrollbarsScrollType
import com.gigamole.composescrollbars.scrolltype.knobtype.ScrollbarsDynamicKnobType
import com.gigamole.composescrollbars.scrolltype.knobtype.ScrollbarsStaticKnobType

@Composable
fun Scrollbar(scrollState: ScrollState, modifier: Modifier = Modifier) {
    Scrollbar(
        ScrollbarsScrollType.Scroll(
            knobType = ScrollbarsStaticKnobType.Auto(),
            state = scrollState
        ),
        modifier
    )
}

@Composable
fun Scrollbar(lazyListState: LazyListState, modifier: Modifier = Modifier) {
    Scrollbar(
        ScrollbarsScrollType.Lazy.List.Dynamic(
            knobType = ScrollbarsDynamicKnobType.Auto(),
            state = lazyListState
        ),
        modifier
    )
}

@Composable
private fun Scrollbar(scrollType: ScrollbarsScrollType, modifier: Modifier = Modifier) {
    Scrollbars(
        state = ScrollbarsState(
            ScrollbarsConfig(
                orientation = ScrollbarsOrientation.Vertical,
                paddingValues = PaddingValues(0.dp),
                layersType = ScrollbarsLayersType.Wrap(ScrollbarsThicknessType.Exact(4.dp)),
                knobLayerContentType = ScrollbarsLayerContentType.Default.Colored.Idle(
                    idleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                ),
                visibilityType = ScrollbarsVisibilityType.Dynamic.Fade(
                    isVisibleOnTouchDown = true,
                    isStaticWhenScrollPossible = false
                )
            ),
            scrollType
        ),
        modifier = modifier
    )
}
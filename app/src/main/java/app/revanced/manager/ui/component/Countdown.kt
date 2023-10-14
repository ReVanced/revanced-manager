package app.revanced.manager.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
fun Countdown(start: Int, content: @Composable (Int) -> Unit) {
    var timer by rememberSaveable(start) {
        mutableStateOf(start)
    }
    LaunchedEffect(timer) {
        if (timer == 0) {
            return@LaunchedEffect
        }

        delay(1000L)
        timer -= 1
    }

    content(timer)
}
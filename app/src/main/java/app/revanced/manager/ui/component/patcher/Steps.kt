package app.revanced.manager.ui.component.patcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.ArrowButton
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.model.ProgressKey
import app.revanced.manager.ui.model.State
import app.revanced.manager.ui.model.Step
import app.revanced.manager.ui.model.StepCategory
import app.revanced.manager.ui.model.StepProgressProvider
import java.util.Locale
import kotlin.math.floor

// Credits: https://github.com/Aliucord/AliucordManager/blob/main/app/src/main/kotlin/com/aliucord/manager/ui/component/installer/InstallGroup.kt
@Composable
fun Steps(
    category: StepCategory,
    steps: List<Step>,
    stepCount: Pair<Int, Int>? = null,
    stepProgressProvider: StepProgressProvider,
    isExpanded: Boolean = false,
    onExpand: () -> Unit
) {
    val state = remember(steps) {
        when {
            steps.all { it.state == State.COMPLETED } -> State.COMPLETED
            steps.any { it.state == State.FAILED } -> State.FAILED
            steps.any { it.state == State.RUNNING } -> State.RUNNING
            else -> State.WAITING
        }
    }

    LaunchedEffect(state) {
        if (state == State.RUNNING)
            onExpand()
    }

    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .clickable(true, onClick = onExpand)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
                StepIcon(state = state, size = 24.dp)

                Text(stringResource(category.displayName))

                Spacer(modifier = Modifier.weight(1f))

                val stepProgress = remember(stepCount, steps) {
                    stepCount?.let { (current, total) -> "$current/$total" }
                        ?: "${steps.count { it.state == State.COMPLETED }}/${steps.size}"
                }

                Text(
                    text = stepProgress,
                    style = MaterialTheme.typography.labelSmall
                )

                ArrowButton(modifier = Modifier.size(24.dp), expanded = isExpanded, onClick = null)
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background.copy(0.6f))
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 20.dp)
                    .padding(start = 4.dp)
            ) {
                steps.forEach { step ->
                    val (progress, progressText) = when (step.progressKey) {
                        null -> null
                        ProgressKey.DOWNLOAD -> stepProgressProvider.downloadProgress?.let { (downloaded, total) ->
                            if (total != null) downloaded.toFloat() / total.toFloat() to "${downloaded.megaBytes}/${total.megaBytes} MB"
                            else null to "${downloaded.megaBytes} MB"
                        }
                    } ?: (null to null)

                    SubStep(
                        name = step.name,
                        state = step.state,
                        message = step.message,
                        progress = progress,
                        progressText = progressText
                    )
                }
            }
        }
    }
}

@Composable
fun SubStep(
    name: String,
    state: State,
    message: String? = null,
    progress: Float? = null,
    progressText: String? = null
) {
    var messageExpanded by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .run {
                if (message != null)
                    clickable { messageExpanded = !messageExpanded }
                else this
            }.add
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepIcon(
                size = 18.dp,
                state = state,
                progress = progress,
            )

            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, true),
            )

            when {
                message != null -> Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ArrowButton(
                        modifier = Modifier.size(20.dp),
                        expanded = messageExpanded,
                        onClick = null
                    )
                }

                progressText != null -> Text(
                    progressText,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        AnimatedVisibility(visible = messageExpanded && message != null) {
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 52.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun StepIcon(state: State, progress: Float? = null, size: Dp) {
    val strokeWidth = Dp(floor(size.value / 10) + 1)

    Crossfade(targetState = state, label = "State CrossFade") { state ->
        when (state) {
            State.COMPLETED -> Icon(
                Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.step_completed),
                tint = Color(0xFF59B463),
                modifier = Modifier.size(size)
            )

            State.FAILED -> Icon(
                Icons.Filled.Cancel,
                contentDescription = stringResource(R.string.step_failed),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(size)
            )

            State.WAITING -> Icon(
                Icons.Outlined.Circle,
                contentDescription = stringResource(R.string.step_waiting),
                tint = MaterialTheme.colorScheme.onSurface.copy(.2f),
                modifier = Modifier.size(size)
            )

            State.RUNNING -> {
                LoadingIndicator(
                    modifier = stringResource(R.string.step_running).let { description ->
                        Modifier
                            .size(size)
                            .semantics {
                                contentDescription = description
                            }
                    },

                    progress = { progress },
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}

private val Long.megaBytes get() = "%.1f".format(locale = Locale.ROOT, toDouble() / 1_000_000)
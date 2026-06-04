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
import app.revanced.manager.ui.model.State
import app.revanced.manager.ui.model.StepCategory
import app.revanced.manager.ui.model.Step
import java.util.Locale
import kotlin.math.floor

// Credits: https://github.com/Aliucord/AliucordManager/blob/main/app/src/main/kotlin/com/aliucord/manager/ui/component/installer/InstallGroup.kt
@Composable
fun Steps(
    category: StepCategory,
    steps: List<Step>,
    isExpanded: Boolean = false,
    onExpand: () -> Unit,
    onClick: () -> Unit
) {
    val state = remember(steps) {
        when {
            steps.all { it.state == State.COMPLETED } -> State.COMPLETED
            steps.any { it.state == State.FAILED } -> State.FAILED
            steps.any { it.state == State.RUNNING } -> State.RUNNING
            else -> State.WAITING
        }
    }

    val filteredSteps = remember(steps) {
        val failedCount = steps.count { it.state == State.FAILED }

        steps.filter { step ->
            // Show hidden steps if it's the only failed step.
            !step.hide || (step.state == State.FAILED && failedCount == 1)
        }
    }

    LaunchedEffect(state) {
        if (state == State.RUNNING || state == State.FAILED)
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
                .clickable(true, onClick = onClick)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            StepIcon(state = state, size = 24.dp)

            Text(stringResource(category.displayName))

            Spacer(modifier = Modifier.weight(1f))

            Text(
                // Use all steps instead of visible steps so the total remains stable and matches the progress bar
                text = "${steps.count { it.state == State.COMPLETED }}/${steps.size}",
                style = MaterialTheme.typography.labelSmall
            )

            ArrowButton(modifier = Modifier.size(24.dp), expanded = isExpanded, onClick = null)
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background.copy(0.6f))
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                filteredSteps.forEachIndexed { index, step ->
                    val (progress, sizeText) = step.progress?.let { (current, total) ->
                        if (total != null) current.toFloat() / total.toFloat() to "${current.megaBytes}/${total.megaBytes} MB"
                        else null to "${current.megaBytes} MB"
                    } ?: (null to null)

                    val progressText = listOfNotNull(sizeText, step.trailingText?.takeIf { it.isNotEmpty() })
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString(" - ")

                    val (message, trailing) = step.message?.let { msg ->
                        val tabIndex = msg.lastIndexOf('\t')
                        val lastNewline = msg.lastIndexOf('\n')
                        if (tabIndex > lastNewline) {
                            msg.substring(0, tabIndex) to msg.substring(tabIndex + 1)
                        } else {
                            msg to null
                        }
                    } ?: (null to null)

                    SubStep(
                        name = step.title,
                        state = step.state,
                        message = message,
                        trailing = trailing,
                        progress = progress,
                        progressText = progressText,
                        isFirst = index == 0,
                        isLast = index == filteredSteps.lastIndex,
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
    trailing: String? = null,
    progress: Float? = null,
    progressText: String? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
) {
    var messageExpanded by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .run {
                if (message != null)
                    clickable { messageExpanded = !messageExpanded }
                else this
            }
            .padding(top = if (isFirst) 10.dp else 8.dp, bottom = if (isLast) 20.dp else 8.dp)
            .padding(horizontal = 20.dp)
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
            Column(
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 8.dp)
            ) {
                val lines = message.orEmpty().split('\n')
                lines.forEachIndexed { i, line ->
                    val isLastLine = i == lines.lastIndex
                    if (isLastLine && trailing != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = line,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f, fill = true),
                            )
                            Text(
                                text = trailing,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    } else {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
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

package app.revanced.manager.ui.model

import android.os.Parcelable
import androidx.annotation.StringRes
import app.revanced.manager.R
import app.revanced.manager.patcher.StepId
import kotlinx.parcelize.Parcelize

enum class StepCategory(@param:StringRes val displayName: Int) {
    PREPARING(R.string.patcher_step_group_preparing),
    PATCHING(R.string.patcher_step_group_patching),
    SAVING(R.string.patcher_step_group_saving)
}

enum class State {
    WAITING, RUNNING, FAILED, COMPLETED
}

@Parcelize
data class Step(
    val id: StepId,
    val title: String,
    val category: StepCategory,
    val state: State = State.WAITING,
    val message: String? = null,
    val index: Int? = null,
    val progress: Pair<Long, Long?>? = null,
    val trailingText: String? = null,
    val messageTrailing: String? = null,
    val hide: Boolean = false,
) : Parcelable


fun Step.withState(
    state: State = this.state,
    message: String? = this.message,
    progress: Pair<Long, Long?>? = this.progress,
    index: Int? = this.index,
) = copy(state = state, message = message, progress = progress, index = index)

internal fun replaceLineAtIndex(current: String?, index: Int, replacement: String): String {
    if (current.isNullOrEmpty()) return replacement
    val lines = current.split("\n").toMutableList()
    when (index) {
        in lines.indices -> lines[index] = replacement
        lines.size -> lines.add(replacement)
    }
    return lines.joinToString("\n")
}
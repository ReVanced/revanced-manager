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
    val progress: Pair<Long, Long?>? = null,
    val hide: Boolean = false,
) : Parcelable


fun Step.withState(
    state: State = this.state,
    message: String? = this.message,
    progress: Pair<Long, Long?>? = this.progress
) = copy(state = state, message = message, progress = progress)
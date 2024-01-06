package app.revanced.manager.ui.model

import androidx.annotation.StringRes
import app.revanced.manager.R
import kotlinx.coroutines.flow.StateFlow

enum class StepCategory(@StringRes val displayName: Int) {
    PREPARING(R.string.patcher_step_group_preparing),
    PATCHING(R.string.patcher_step_group_patching),
    SAVING(R.string.patcher_step_group_saving)
}

enum class State {
    WAITING, RUNNING, FAILED, COMPLETED
}

data class Step(
    val name: String,
    val category: StepCategory,
    val state: State = State.WAITING,
    val message: String? = null,
    val downloadProgress: StateFlow<Pair<Float, Float>?>? = null
)
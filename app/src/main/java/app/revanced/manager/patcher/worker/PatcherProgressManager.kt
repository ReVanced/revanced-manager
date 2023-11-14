package app.revanced.manager.patcher.worker

import android.content.Context
import androidx.annotation.StringRes
import app.revanced.manager.R
import app.revanced.manager.ui.model.SelectedApp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
    val progress: StateFlow<Pair<Float, Float>?>? = null
)

class PatcherProgressManager(
    context: Context,
    selectedPatches: List<String>,
    selectedApp: SelectedApp,
    progress: StateFlow<Pair<Float, Float>?>
) {
    var steps: ImmutableList<Step> = persistentListOf()
        private set

    private var currentStepIndex: Int = 0

    init {
        steps = generateSteps(
            context,
            selectedPatches,
            selectedApp,
            progress
        )
    }

    fun updateProgress(state: State, message: String? = null) {
        steps = steps.toMutableList().apply {
            this[currentStepIndex] = this[currentStepIndex].copy(state = state, message = message)

            if (state == State.COMPLETED && currentStepIndex != steps.lastIndex) {
                currentStepIndex++

                this[currentStepIndex] = this[currentStepIndex].copy(state = State.RUNNING)
            }
        }.toImmutableList()
    }

    companion object {
        fun generateSteps(
            context: Context,
            selectedPatches: List<String>,
            selectedApp: SelectedApp,
            progress: StateFlow<Pair<Float, Float>?>? = null
        ): ImmutableList<Step> {
            val preparing = listOfNotNull(
                Step(
                    context.getString(R.string.patcher_step_load_patches),
                    StepCategory.PREPARING,
                    state = State.RUNNING
                ),
                Step(
                    context.getString(R.string.download_apk),
                    StepCategory.PREPARING,
                    progress = progress
                ).takeIf { selectedApp is SelectedApp.Download },
                Step(
                    context.getString(R.string.patcher_step_unpack),
                    StepCategory.PREPARING
                ),
                Step(
                    context.getString(R.string.patcher_step_integrations),
                    StepCategory.PREPARING
                )
            )

            val patches = selectedPatches.map { Step(it, StepCategory.PATCHING) }

            val saving = listOf(
                Step(context.getString(R.string.patcher_step_write_patched), StepCategory.SAVING),
                Step(context.getString(R.string.patcher_step_sign_apk), StepCategory.SAVING)
            )

            return (preparing + patches + saving).toImmutableList()
        }
    }
}
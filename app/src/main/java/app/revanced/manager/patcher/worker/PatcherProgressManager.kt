package app.revanced.manager.patcher.worker

import android.content.Context
import androidx.annotation.StringRes
import app.revanced.manager.R
import app.revanced.manager.ui.model.SelectedApp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow

enum class State {
    WAITING, COMPLETED, FAILED
}

class SubStep(
    val name: String,
    val state: State = State.WAITING,
    val message: String? = null,
    val progress: StateFlow<Pair<Float, Float>?>? = null
)

class Step(
    @StringRes val name: Int,
    val subSteps: ImmutableList<SubStep>,
    val state: State = State.WAITING
)

class PatcherProgressManager(context: Context, selectedPatches: List<String>, selectedApp: SelectedApp, downloadProgress: StateFlow<Pair<Float, Float>?>) {
    val steps = generateSteps(context, selectedPatches, selectedApp, downloadProgress)
    private var currentStep: StepKey? = StepKey(0, 0)

    private fun update(key: StepKey, state: State, message: String? = null) {
        val isLastSubStep: Boolean
        steps[key.step] = steps[key.step].let { step ->
            isLastSubStep = key.substep == step.subSteps.lastIndex

            val newStepState = when {
                // This step failed because one of its sub-steps failed.
                state == State.FAILED -> State.FAILED
                // All sub-steps succeeded.
                state == State.COMPLETED && isLastSubStep -> State.COMPLETED
                // Keep the old status.
                else -> step.state
            }

            Step(step.name, step.subSteps.mapIndexed { index, subStep ->
                if (index != key.substep) subStep else SubStep(subStep.name, state, message)
            }.toImmutableList(), newStepState)
        }

        val isFinal = isLastSubStep && key.step == steps.lastIndex

        if (state == State.COMPLETED) {
            // Move the cursor to the next step.
            currentStep = when {
                isFinal -> null // Final step has been completed.
                isLastSubStep -> StepKey(key.step + 1, 0) // Move to the next step.
                else -> StepKey(
                    key.step,
                    key.substep + 1
                ) // Move to the next sub-step.
            }
        }
    }

    fun replacePatchesList(newList: List<String>) {
        steps[1] = generatePatchesStep(newList)
    }

    private fun updateCurrent(newState: State, message: String? = null) {
        currentStep?.let { update(it, newState, message) }
    }

    fun failure(error: Throwable) = updateCurrent(
        State.FAILED,
        error.stackTraceToString()
    )

    fun success() = updateCurrent(State.COMPLETED)

    fun getProgress(): List<Step> = steps

    companion object {
        private fun generatePatchesStep(selectedPatches: List<String>) = Step(
            R.string.patcher_step_group_patching,
            selectedPatches.map { SubStep(it) }.toImmutableList()
        )

        fun generateSteps(context: Context, selectedPatches: List<String>, selectedApp: SelectedApp, downloadProgress: StateFlow<Pair<Float, Float>?>? = null) = mutableListOf(
            Step(
                R.string.patcher_step_group_prepare,
                listOfNotNull(
                    SubStep(context.getString(R.string.patcher_step_load_patches)),
                    SubStep("Download apk", progress = downloadProgress).takeIf { selectedApp is SelectedApp.Download },
                    SubStep(context.getString(R.string.patcher_step_unpack)),
                    SubStep(context.getString(R.string.patcher_step_integrations))
                ).toImmutableList()
            ),
            generatePatchesStep(selectedPatches),
            Step(
                R.string.patcher_step_group_saving,
                persistentListOf(SubStep(context.getString(R.string.patcher_step_write_patched)))
            )
        )
    }

    private data class StepKey(val step: Int, val substep: Int)
}
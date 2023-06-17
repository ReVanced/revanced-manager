package app.revanced.manager.patcher.worker

import android.content.Context
import androidx.annotation.StringRes
import app.revanced.manager.R
import app.revanced.manager.util.serialize
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class Progress {
    object Unpacking : Progress()
    object Merging : Progress()
    object PatchingStart : Progress()

    data class PatchSuccess(val patchName: String) : Progress()

    object Saving : Progress()
}

@Serializable
enum class State {
    WAITING, COMPLETED, FAILED
}

@Serializable
class SubStep(
    val name: String,
    val state: State = State.WAITING,
    @SerialName("msg")
    val message: String? = null
)

@Serializable
class Step(
    @StringRes val name: Int,
    val substeps: List<SubStep>,
    val state: State = State.WAITING
)

class PatcherProgressManager(context: Context, selectedPatches: List<String>) {
    val steps = generateSteps(context, selectedPatches)
    private var currentStep: StepKey? = StepKey(0, 0)

    private fun update(key: StepKey, state: State, message: String? = null) {
        val isLastSubStep: Boolean
        steps[key.step] = steps[key.step].let { step ->
            isLastSubStep = key.substep == step.substeps.lastIndex

            val newStepState = when {
                // This step failed because one of its sub-steps failed.
                state == State.FAILED -> State.FAILED
                // All sub-steps succeeded.
                state == State.COMPLETED && isLastSubStep -> State.COMPLETED
                // Keep the old status.
                else -> step.state
            }

            Step(step.name, step.substeps.mapIndexed { index, subStep ->
                if (index != key.substep) subStep else SubStep(subStep.name, state, message)
            }, newStepState)
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
        steps[stepKeyMap[Progress.PatchingStart]!!.step] = generatePatchesStep(newList)
    }

    private fun updateCurrent(newState: State, message: String? = null) =
        currentStep?.let { update(it, newState, message) }


    fun handle(progress: Progress) = success().also {
        stepKeyMap[progress]?.let { currentStep = it }
    }

    fun failure(error: Throwable) = updateCurrent(
        State.FAILED,
        error.stackTraceToString()
    )

    fun success() = updateCurrent(State.COMPLETED)

    fun workData() = steps.serialize()

    companion object {
        /**
         * A map of [Progress] to the corresponding position in [steps]
         */
        private val stepKeyMap = mapOf(
            Progress.Unpacking to StepKey(0, 1),
            Progress.Merging to StepKey(0, 2),
            Progress.PatchingStart to StepKey(1, 0),
            Progress.Saving to StepKey(2, 0),
        )

        private fun generatePatchesStep(selectedPatches: List<String>) = Step(
            R.string.patcher_step_group_patching,
            selectedPatches.map { SubStep(it) }
        )

        fun generateSteps(context: Context, selectedPatches: List<String>) = mutableListOf(
            Step(
                R.string.patcher_step_group_prepare,
                persistentListOf(
                    SubStep(context.getString(R.string.patcher_step_load_patches)),
                    SubStep(context.getString(R.string.patcher_step_unpack)),
                    SubStep(context.getString(R.string.patcher_step_integrations))
                )
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
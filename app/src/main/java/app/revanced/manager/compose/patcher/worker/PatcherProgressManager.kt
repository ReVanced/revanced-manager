package app.revanced.manager.compose.patcher.worker

import android.content.Context
import androidx.annotation.StringRes
import androidx.work.Data
import androidx.work.workDataOf
import app.revanced.manager.compose.R
import app.revanced.manager.compose.patcher.Session
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class Progress {
    object Unpacking : Progress()
    object Merging : Progress()
    object PatchingStart : Progress()

    data class PatchSuccess(val patchName: String) : Progress()

    object Saving : Progress()
}

@Serializable
enum class StepStatus {
    WAITING,
    COMPLETED,
    FAILURE,
}

@Serializable
class Step(val name: String, val status: StepStatus = StepStatus.WAITING)

@Serializable
class StepGroup(@StringRes val name: Int, val steps: ImmutableList<Step>, val status: StepStatus = StepStatus.WAITING)

class PatcherProgressManager(context: Context, selectedPatches: List<String>) {
    val stepGroups = generateGroupsList(context, selectedPatches)

    companion object {
        private const val PATCHES = 1
        private const val WORK_DATA_KEY = "progress"

        /**
         * A map of [Session.Progress] to the corresponding position in [stepGroups]
         */
        private val stepKeyMap = mapOf(
            Progress.Unpacking to StepKey(0, 0),
            Progress.Merging to StepKey(0, 1),
            Progress.PatchingStart to StepKey(PATCHES, 0),
            Progress.Saving to StepKey(2, 0),
        )

        fun generateGroupsList(context: Context, selectedPatches: List<String>) = mutableListOf(
            StepGroup(
                R.string.patcher_step_group_prepare,
                persistentListOf(
                    Step(context.getString(R.string.patcher_step_unpack)),
                    Step(context.getString(R.string.patcher_step_integrations))
                )
            ),
            StepGroup(
                R.string.patcher_step_group_patching,
                selectedPatches.map { Step(it) }.toImmutableList()
            ),
            StepGroup(
                R.string.patcher_step_group_saving,
                persistentListOf(Step(context.getString(R.string.patcher_step_write_patched)))
            )
        )

        fun groupsFromWorkData(workData: Data) = workData.getString(WORK_DATA_KEY)
            ?.let { Json.decodeFromString<List<StepGroup>>(it) }
    }

    fun groupsToWorkData() = workDataOf(WORK_DATA_KEY to Json.Default.encodeToString(stepGroups))

    private var currentStep: StepKey? = null

    private fun <T> MutableList<T>.mutateIndex(index: Int, callback: (T) -> T) = apply {
        this[index] = callback(this[index])
    }

    private fun updateStepStatus(key: StepKey, newStatus: StepStatus) {
        var isLastStepOfGroup = false
        stepGroups.mutateIndex(key.groupIndex) { group ->
            isLastStepOfGroup = key.stepIndex == group.steps.size - 1
            val newGroupStatus = when {
                // This group failed if a step in it failed.
                newStatus == StepStatus.FAILURE -> StepStatus.FAILURE
                // All steps in the group succeeded.
                newStatus == StepStatus.COMPLETED && isLastStepOfGroup -> StepStatus.COMPLETED
                // Keep the old status.
                else -> group.status
            }

            StepGroup(group.name, group.steps.toMutableList().mutateIndex(key.stepIndex) { step ->
                Step(step.name, newStatus)
            }.toImmutableList(), newGroupStatus)
        }

        val isFinalStep = isLastStepOfGroup && key.groupIndex == stepGroups.size - 1

        if (newStatus == StepStatus.COMPLETED) {
            // Move the cursor to the next step.
            currentStep = when {
                isFinalStep -> null // Final step has been completed.
                isLastStepOfGroup -> StepKey(key.groupIndex + 1, 0) // Move to the next group.
                else -> StepKey(key.groupIndex, key.stepIndex + 1) // Move to the next step of this group.
            }
        }
    }

    private fun setCurrentStepStatus(newStatus: StepStatus) = currentStep?.let { updateStepStatus(it, newStatus) }

    private data class StepKey(val groupIndex: Int, val stepIndex: Int)

    fun handle(progress: Progress) {
        if (progress is Progress.PatchSuccess) {
            val patchStepKey = StepKey(
                PATCHES,
                stepGroups[PATCHES].steps.indexOfFirst { it.name == progress.patchName })

            updateStepStatus(patchStepKey, StepStatus.COMPLETED)
        } else {
            currentStep?.let { updateStepStatus(it, StepStatus.COMPLETED) }

            currentStep = stepKeyMap[progress]!!
        }
    }

    fun failure() {
        // TODO: associate the exception with the step that just failed.
        setCurrentStepStatus(StepStatus.FAILURE)
    }

    fun success() {
        setCurrentStepStatus(StepStatus.COMPLETED)
    }
}
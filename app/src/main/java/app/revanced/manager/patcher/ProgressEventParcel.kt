package app.revanced.manager.patcher

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class ProgressEventParcel(
    val eventType: Int,
    val stepType: Int,
    val index: Int = -1,
    val done: Long? = null,
    val total: Long? = null,
    val message: String? = null,
    val error: RemoteError? = null,
) : Parcelable {
    companion object {
        const val EVENT_TYPE_STARTED = 0
        const val EVENT_TYPE_PROGRESS = 1
        const val EVENT_TYPE_COMPLETED = 2
        const val EVENT_TYPE_FAILED = 3

        const val STEP_ID_UNKNOWN = -1
        const val STEP_ID_DOWNLOAD_APK = 0
        const val STEP_ID_LOAD_PATCHES = 1
        const val STEP_ID_READ_APK = 2
        const val STEP_ID_EXECUTE_PATCHES = 3
        const val STEP_ID_EXECUTE_PATCH = 4
        const val STEP_ID_WRITE_APK = 5
        const val STEP_ID_SIGN_APK = 6
    }
}

fun ProgressEvent.toParcel(): ProgressEventParcel {
    val stepType = when (stepId) {
        is StepId.DownloadAPK -> ProgressEventParcel.STEP_ID_DOWNLOAD_APK
        is StepId.LoadPatches -> ProgressEventParcel.STEP_ID_LOAD_PATCHES
        is StepId.ReadAPK -> ProgressEventParcel.STEP_ID_READ_APK
        is StepId.ExecutePatches -> ProgressEventParcel.STEP_ID_EXECUTE_PATCHES
        is StepId.ExecutePatch -> ProgressEventParcel.STEP_ID_EXECUTE_PATCH
        is StepId.WriteAPK -> ProgressEventParcel.STEP_ID_WRITE_APK
        is StepId.SignAPK -> ProgressEventParcel.STEP_ID_SIGN_APK
        null -> ProgressEventParcel.STEP_ID_UNKNOWN
    }

    return when (this) {
        is ProgressEvent.Started -> ProgressEventParcel(
            eventType = ProgressEventParcel.EVENT_TYPE_STARTED,
            stepType = stepType,
            index = if (stepId is StepId.ExecutePatch) stepId.index else -1
        )

        is ProgressEvent.Progress -> ProgressEventParcel(
            eventType = ProgressEventParcel.EVENT_TYPE_PROGRESS,
            stepType = stepType,
            index = if (stepId is StepId.ExecutePatch) stepId.index else -1,
            done = done,
            total = total,
            message = message
        )

        is ProgressEvent.Completed -> ProgressEventParcel(
            eventType = ProgressEventParcel.EVENT_TYPE_COMPLETED,
            stepType = stepType,
            index = if (stepId is StepId.ExecutePatch) stepId.index else -1
        )

        is ProgressEvent.Failed -> ProgressEventParcel(
            eventType = ProgressEventParcel.EVENT_TYPE_FAILED,
            stepType = stepType,
            index = if (stepId is StepId.ExecutePatch) stepId.index else -1,
            error = error
        )
    }
}

fun ProgressEventParcel.toEvent(): ProgressEvent {
    val stepId: StepId? = when (stepType) {
        ProgressEventParcel.STEP_ID_DOWNLOAD_APK -> StepId.DownloadAPK
        ProgressEventParcel.STEP_ID_LOAD_PATCHES -> StepId.LoadPatches
        ProgressEventParcel.STEP_ID_READ_APK -> StepId.ReadAPK
        ProgressEventParcel.STEP_ID_EXECUTE_PATCHES -> StepId.ExecutePatches
        ProgressEventParcel.STEP_ID_EXECUTE_PATCH -> StepId.ExecutePatch(index)
        ProgressEventParcel.STEP_ID_WRITE_APK -> StepId.WriteAPK
        ProgressEventParcel.STEP_ID_SIGN_APK -> StepId.SignAPK
        ProgressEventParcel.STEP_ID_UNKNOWN -> null
        else -> throw IllegalArgumentException("Unknown step type: $stepType")
    }

    return when (eventType) {
        ProgressEventParcel.EVENT_TYPE_STARTED -> ProgressEvent.Started(stepId!!)
        ProgressEventParcel.EVENT_TYPE_PROGRESS -> ProgressEvent.Progress(
            stepId = stepId!!,
            done = done,
            total = total,
            message = message
        )
        ProgressEventParcel.EVENT_TYPE_COMPLETED -> ProgressEvent.Completed(stepId!!)
        ProgressEventParcel.EVENT_TYPE_FAILED -> ProgressEvent.Failed(
            stepId = stepId,
            error = error!!
        )
        else -> throw IllegalArgumentException("Unknown event type: $eventType")
    }
}

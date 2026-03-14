package app.revanced.manager.patcher

import android.os.Parcelable
import app.revanced.manager.patcher.logger.LogLevel
import kotlinx.parcelize.Parcelize


@Parcelize
sealed class ProgressEvent : Parcelable {
    abstract val stepId: StepId?

    data class Started(override val stepId: StepId) : ProgressEvent()

    data class Progress(
        override val stepId: StepId,
        val current: Long? = null,
        val total: Long? = null,
        val message: String? = null,
    ) : ProgressEvent()

    data class Log(
        override val stepId: StepId,
        val level: LogLevel,
        val message: String,
    ) : ProgressEvent()

    data class Completed(
        override val stepId: StepId,
    ) : ProgressEvent()

    data class Failed(
        override val stepId: StepId?,
        val error: RemoteError,
    ) : ProgressEvent()
}

/**
 * Parcelable wrapper for [ProgressEvent].
 *
 * Required because AIDL does not support sealed classes.
 */
@Parcelize
data class ProgressEventParcel(val event: ProgressEvent) : Parcelable

fun ProgressEventParcel.toEvent(): ProgressEvent = event
fun ProgressEvent.toParcel(): ProgressEventParcel = ProgressEventParcel(this)

@Parcelize
sealed class StepId : Parcelable {
    data object DownloadAPK : StepId()
    data object LoadPatches : StepId()
    data object ReadAPK : StepId()
    data object ExecutePatches : StepId()
    data class ExecutePatch(val index: Int) : StepId()
    data object WriteAPK : StepId()
    data object SignAPK : StepId()
}

@Parcelize
data class RemoteError(
    val type: String,
    val message: String?,
    val stackTrace: String,
) : Parcelable

fun Exception.toRemoteError() = RemoteError(
    type = this::class.java.name,
    message = this.message,
    stackTrace = this.stackTraceToString(),
)


inline fun <T> runStep(
    stepId: StepId,
    onEvent: (ProgressEvent) -> Unit,
    block: () -> T,
): T = try {
    onEvent(ProgressEvent.Started(stepId))
    val value = block()
    onEvent(ProgressEvent.Completed(stepId))
    value
} catch (error: Exception) {
    onEvent(ProgressEvent.Failed(stepId, error.toRemoteError()))
    throw error
}
package app.revanced.manager.patcher

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
sealed class ProgressEvent : Parcelable {
    abstract val stepId: StepId?

    @Parcelize
    data class Started(override val stepId: StepId) : ProgressEvent()

    @Parcelize
    data class Progress(
        override val stepId: StepId,
        val done: Long? = null,
        val total: Long? = null,
        val message: String? = null,
    ) : ProgressEvent()

    @Parcelize
    data class Completed(
        override val stepId: StepId,
    ) : ProgressEvent()

    @Parcelize
    data class Failed(
        override val stepId: StepId?,
        val error: RemoteError,
    ) : ProgressEvent()
}

@Parcelize
sealed class StepId : Parcelable {
    @Parcelize
    data object DownloadAPK : StepId()
    @Parcelize
    data object LoadPatches : StepId()
    @Parcelize
    data object ReadAPK : StepId()
    @Parcelize
    data object ExecutePatches : StepId()
    @Parcelize
    data class ExecutePatch(val index: Int) : StepId()
    @Parcelize
    data object WriteAPK : StepId()
    @Parcelize
    data object SignAPK : StepId()
}

@Parcelize
data class RemoteError(
    val type: String? = null,
    val message: String? = null,
    val stackTrace: String? = null,
    val code: Int? = null,
) : Parcelable

fun Exception.toRemoteError(): RemoteError = RemoteError(
    type = this::class.java.name,
    message = this.message,
    stackTrace = this.stackTraceToString(),
)


inline fun <T> runStep(
    stepId: StepId,
    onEvent: (ProgressEvent) -> Unit,
    block: () -> T,
): T {
    onEvent(ProgressEvent.Started(stepId))
    try {
        val value = block()
        onEvent(ProgressEvent.Completed(stepId))
        return value
    } catch (error: Exception) {
        onEvent(ProgressEvent.Failed(stepId, error.toRemoteError()))
        throw error
    }
}
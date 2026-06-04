package app.revanced.manager.patcher

import app.revanced.manager.patcher.logger.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class Heartbeat(
    scope: CoroutineScope,
    private val onEvent: (ProgressEvent) -> Unit,
    private val stepId: StepId,
    initialDelayMs: Long = 3_000L,
    private val intervalMs: Long = 1_000L,
    private val tickMessage: (elapsedSeconds: Long) -> String,
) {
    private val startNs = System.nanoTime()
    private val finished = AtomicBoolean(false)

    private val elapsedSeconds get() = (System.nanoTime() - startNs) / 1_000_000_000L

    private val job: Job = scope.launch {
        delay(initialDelayMs)
        while (!finished.get()) {
            emit(tickMessage(elapsedSeconds))
            delay(intervalMs)
        }
    }

    private fun emit(message: String) = onEvent(
        ProgressEvent.Log(stepId, LogLevel.INFO, message, replaceLast = true)
    )

    fun complete(finalMessage: (elapsedSeconds: Long) -> String) {
        if (!finished.compareAndSet(false, true)) return
        emit(finalMessage(elapsedSeconds))
        job.cancel()
    }

    fun stop() {
        finished.set(true)
        job.cancel()
    }
}

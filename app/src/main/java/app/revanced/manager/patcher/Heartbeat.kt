package app.revanced.manager.patcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class Heartbeat(
    scope: CoroutineScope,
    initialDelayMs: Long = 3_000L,
    private val intervalMs: Long = 1_000L,
    private val onTick: (elapsedSeconds: Long) -> Unit,
) {
    private val startNs = System.nanoTime()
    private val finished = AtomicBoolean(false)

    val elapsedSeconds get() = (System.nanoTime() - startNs) / 1_000_000_000L

    private val job: Job = scope.launch {
        delay(initialDelayMs.milliseconds)
        while (!finished.get()) {
            onTick(elapsedSeconds)
            delay(intervalMs.milliseconds)
        }
    }

    fun complete(onComplete: (elapsedSeconds: Long) -> Unit) {
        if (!finished.compareAndSet(false, true)) return
        onComplete(elapsedSeconds)
        job.cancel()
    }

    fun stop() {
        finished.set(true)
        job.cancel()
    }
}

package app.revanced.manager.data.redux

import android.util.Log
import app.revanced.manager.util.tag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.whileSelect
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// This file implements React Redux-like state management.

class Store<S>(private val coroutineScope: CoroutineScope, initialState: S): ActionContext<S> {
    private val _state = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    // Do not touch these without the lock.
    private var isRunningActions = false
    private val queueChannel = Channel<Action<S>>(capacity = 10)
    private val lock = Mutex()

    override suspend fun dispatch(action: Action<S>) = lock.withLock {
        queueChannel.send(action)

        if (isRunningActions) return@withLock
        isRunningActions = true
        coroutineScope.launch {
            runActions()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun runActions() = whileSelect {
        queueChannel.onReceive { action ->
            _state.value = try {
                with(action) { this@Store.execute(_state.value) }
            } catch (c: CancellationException) {
                // This is done without the lock, but cancellation usually means the store is no longer needed.
                isRunningActions = false
                throw c
            } catch (e: Exception) {
                action.catch(e)
                return@onReceive true
            }

            true
        }
        onTimeout(200L) {
            lock.withLock {
                // New actions may be dispatched during the timeout.
                isRunningActions = queueChannel.isEmpty
                isRunningActions
            }
        }
    }
}

interface ActionContext<S> {
    suspend fun dispatch(action: Action<S>)
}

interface Action<S> {
    suspend fun ActionContext<S>.execute(current: S): S
    suspend fun catch(exception: Exception) {
        Log.e(tag, "Got exception while executing $this", exception)
    }
}
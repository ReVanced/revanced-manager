package app.revanced.manager.compose.domain.repository

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.revanced.manager.compose.patcher.patch.PatchBundle
import app.revanced.manager.compose.util.launchAndRepeatWithViewLifecycle
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BundleRepository(private val sourceRepository: SourceRepository) {
    /**
     * A [Flow] that emits whenever the sources change.
     *
     * The outer flow emits whenever the sources configuration changes.
     * The inner flow emits whenever one of the bundles update.
     */
    private val sourceUpdates = sourceRepository.sources.map { sources ->
        sources.map { (name, source) ->
            source.bundle.map { bundle ->
                name to bundle
            }
        }.merge().buffer()
    }

    private val _bundles = MutableStateFlow<Map<String, PatchBundle>>(emptyMap())

    /**
     * A [Flow] that gives you all loaded [PatchBundle]s.
     * This is only synced when the app is in the foreground.
     */
    val bundles = _bundles.asStateFlow()

    fun onAppStart(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycleScope.launch {
            sourceRepository.loadSources()
        }

        lifecycleOwner.launchAndRepeatWithViewLifecycle {
            sourceUpdates.collect { events ->
                val map = HashMap<String, PatchBundle>()
                _bundles.emit(map)

                events.collect { (name, new) ->
                    map[name] = new
                    _bundles.emit(map)
                }
            }
        }
    }
}
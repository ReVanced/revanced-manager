package app.revanced.manager.compose.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class PatchesSelectorViewModel: ViewModel() {
    private val patchesList = listOf(
        Patch("amogus-patch", "adds amogus to all apps, mogus mogus mogus mogus mogus mogus mogus mogus mogus mogus mogus mogus mogus mogus mogus mogus mogus ",
            options = listOf(
                Option(
                    "amogus"
                )
            ),
            isSupported = true
        ),
        Patch("microg-support", "makes microg work",
            options = listOf(),
            isSupported = false
        ),Patch("microg-support", "makes microg work",
            options = listOf(),
            isSupported = false
        ),Patch("microg-support", "makes microg work",
            options = listOf(),
            isSupported = true
        ),Patch("microg-support", "makes microg work",
            options = listOf(),
            isSupported = false
        ),Patch("microg-support", "makes microg work",
            options = listOf(),
            isSupported = false
        ),Patch("microg-support", "makes microg work",
            options = listOf(),
            isSupported = false
        ),Patch("microg-support", "makes microg work",
            options = listOf(
                Option(
                    "amogus"
                )
            ),
            isSupported = false
        ),Patch("microg-support", "makes microg work",
            options = listOf(),
            isSupported = false
        ),Patch("microg-support", "makes microg work",
            options = listOf(),
            isSupported = false
        ),

    ).let { it + it + it + it + it }

    private val patchesLists = patchesList.groupBy { if (it.isSupported) "supported" else "unsupported" }

    val bundles = listOf(
        Bundle(
            name = "offical",
            patches = patchesLists
        ),
        Bundle(
            name = "extended",
            patches = patchesLists
        ),
        Bundle(
            name = "balls",
            patches = patchesLists
        ),
    )

    var selectedPatches = mutableStateListOf<Patch>()

    data class Bundle(
        val name: String,
        val patches: Map<String, List<Patch>>
    )

    data class Patch(
        val name: String,
        val description: String,
        val options: List<Option>,
        val isSupported: Boolean
    )

    data class Option(
        val name: String
    )
}
package app.revanced.manager.patcher.patches

import app.revanced.patcher.patch.Patch
import app.revanced.patches.Index
import kotlin.reflect.cast

class PatchLoader {
    private val patches: List<Patch> = Index.patches.map { patch ->
        Patch::class.cast(patch.javaObjectType)
    }

    fun loadPatches() : List<Patch> {
        return patches
    }
}

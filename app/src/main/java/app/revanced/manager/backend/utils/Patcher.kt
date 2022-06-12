package app.revanced.manager.backend.utils

// TODO: adapt cli code
//import app.revanced.patcher.Patcher
//import app.revanced.patcher.data.base.Data
//import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
//import app.revanced.patcher.extensions.PatchExtensions.include
//import app.revanced.patcher.extensions.PatchExtensions.patchName
//import app.revanced.patcher.patch.base.Patch
//import app.revanced.patcher.util.patch.implementation.JarPatchBundle
//
//fun Patcher.addPatchesFiltered(
//    includeFilter: Boolean = false
//) {
//    val packageName = this.data.packageMetadata.packageName
//    val packageVersion = this.data.packageMetadata.packageVersion
//
//    patchBundles.forEach { bundle ->
//        val includedPatches = mutableListOf<Class<out Patch<Data>>>()
//        JarPatchBundle(bundle).loadPatches().forEach patch@{ patch ->
//            val compatiblePackages = patch.compatiblePackages
//            val patchName = patch.patchName
//
//            val prefix = "[skipped] $patchName"
//
//            if (includeFilter) {
//                if (!MainCommand.includedPatches.contains(patchName)) {
//                    println("$prefix: Explicitly excluded.")
//                    return@patch
//                }
//            } else if (!patch.include) {
//                println("$prefix: Implicitly excluded.")
//                return@patch
//            }
//
//            if (compatiblePackages == null) println("$prefix: Missing compatibility annotation. Continuing.")
//            else compatiblePackages.forEach { compatiblePackage ->
//                if (compatiblePackage.name != packageName) {
//                    println("$prefix: Package name not matching ${compatiblePackage.name}.")
//                    return@patch
//                }
//
//                if (!(debugging || compatiblePackage.versions.any { it == packageVersion })) {
//                    println("$prefix: Unsupported version.")
//                    return@patch
//                }
//            }
//
//            includedPatches.add(patch)
//            println("[added] $patchName")
//        }
//        this.addPatches(includedPatches)
//    }
//}
//
//fun Patcher.applyPatchesVerbose() {
//    this.applyPatches().forEach { (patch, result) ->
//        if (result.isSuccess) {
//            println("[success] $patch")
//            return@forEach
//        }
//        println("[error] $patch:")
//        result.exceptionOrNull()!!.printStackTrace()
//    }
//}
//
//fun Patcher.mergeFiles() {
//    this.addFiles(MainCommand.mergeFiles)
//}
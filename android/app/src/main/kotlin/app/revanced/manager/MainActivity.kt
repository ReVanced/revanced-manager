package app.revanced.manager

import androidx.annotation.NonNull
import app.revanced.patcher.data.Data
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.extensions.PatchExtensions.description
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.extensions.PatchExtensions.version
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.util.patch.implementation.DexPatchBundle
import dalvik.system.DexClassLoader
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "app.revanced/patcher"
    private var patches = mutableListOf<Class<out Patch<Data>>>()

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "loadPatches" -> {
                    val pathBundlesPaths = call.argument<List<String>>("pathBundlesPaths")
                    if (pathBundlesPaths != null) {
                        loadPatches(pathBundlesPaths)
                        result.success("OK")
                    } else {
                        result.notImplemented()
                    }
                }
                "getCompatiblePackages" -> result.success(getCompatiblePackages())
                "getFilteredPatches" -> {
                    val targetPackage = call.argument<String>("targetPackage")
                    val targetVersion = call.argument<String>("targetVersion")
                    val ignoreVersion = call.argument<Boolean>("ignoreVersion")
                    if (targetPackage != null && targetVersion != null && ignoreVersion != null) {
                        result.success(getFilteredPatches(targetPackage, targetVersion, ignoreVersion))
                    } else {
                        result.notImplemented()
                    }
                }
                else -> result.notImplemented()
            }
        }
    }

    fun loadPatches(pathBundlesPaths: List<String>) {
        pathBundlesPaths.forEach { path ->
            patches.addAll(DexPatchBundle(
                path, DexClassLoader(
                    path,
                    context.cacheDir.path,
                    null,
                    javaClass.classLoader
                )
            ).loadPatches())
        }
    }

    fun getCompatiblePackages(): List<String> {
        val filteredPackages = mutableListOf<String>()
        patches.forEach patch@{ patch ->
            patch.compatiblePackages?.forEach { pkg ->
                filteredPackages.add(pkg.name)
            }
        }
        return filteredPackages.distinct()
    }

    fun getFilteredPatches(targetPackage: String, targetVersion: String, ignoreVersion: Boolean): List<Map<String, String?>> {
        val filteredPatches = mutableListOf<Map<String, String?>>()
        patches.forEach patch@{ patch ->
            patch.compatiblePackages?.forEach { pkg ->
                if (pkg.name == targetPackage && (ignoreVersion || pkg.versions.isNotEmpty() || pkg.versions.contains(targetVersion))) {
                    var p = mutableMapOf<String, String?>();
                    p.put("name", patch.patchName);
                    p.put("version", patch.version);
                    p.put("description", patch.description);
                    filteredPatches.add(p)
                }
            }
        }
        return filteredPatches
    }
}

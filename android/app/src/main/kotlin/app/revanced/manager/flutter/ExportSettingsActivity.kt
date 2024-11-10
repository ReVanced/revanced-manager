package app.revanced.manager.flutter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.MessageDigest

class ExportSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (getFingerprint(callingPackage!!) == getFingerprint(packageName)) {
            // Create JSON Object
            val json = JSONObject()

            // Default Data
            json.put("keystorePassword", "s3cur3p@ssw0rd")

            // Load Shared Preferences
            val sharedPreferences = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val allEntries: Map<String, *> = sharedPreferences.getAll()
            for ((key, value) in allEntries.entries) {
                json.put(key.replace("flutter.", ""), value)
            }

            // Load keystore
            val keystoreFile = File(getExternalFilesDir(null), "/revanced-manager.keystore")
            if (keystoreFile.exists()) {
                val keystoreBytes = keystoreFile.readBytes()
                val keystoreBase64 = Base64.encodeToString(keystoreBytes, Base64.DEFAULT)
                json.put("keystore", keystoreBase64)
            }

            // Load saved patches
            val storedPatchesFile = File(filesDir.parentFile.absolutePath, "/app_flutter/selected-patches.json")
            if (storedPatchesFile.exists()) {
                val patchesBytes = storedPatchesFile.readBytes()
                val patches = String(patchesBytes, Charsets.UTF_8)
                json.put("patches", JSONObject(patches))
            }

            // Send data back
            val resultIntent = Intent()
            resultIntent.putExtra("data", json.toString())
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } else {
            val resultIntent = Intent()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    fun getFingerprint(packageName: String): String {
        // Get the signature of the app that matches the package name
        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        val signature = packageInfo.signatures!![0]

        // Get the raw certificate data
        val rawCert = signature.toByteArray()

        // Generate an X509Certificate from the data
        val certFactory = CertificateFactory.getInstance("X509")
        val x509Cert = certFactory.generateCertificate(ByteArrayInputStream(rawCert)) as X509Certificate

        // Get the SHA256 fingerprint
        val fingerprint = MessageDigest.getInstance("SHA256").digest(x509Cert.encoded).joinToString("") {
            "%02x".format(it)
        }

        return fingerprint
    }
}

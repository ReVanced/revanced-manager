package app.revanced.manager.flutter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.MessageDigest

import android.util.Log

class ExportSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val revancedFingerprint = "b6362c6ea7888efd15c0800f480786ad0f5b133b4f84e12d46afba5f9eac1223"

        // Get the package name of the app that started the activity
        val packageName = getCallingPackage()!!

        // Get the signature of the app that matches the package name
        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        val signatures = packageInfo.signatures

        // Loop through each signature and print its properties
        for (signature in signatures) {
            // Get the raw certificate data
            val rawCert = signature.toByteArray()

            // Generate an X509Certificate from the data
            val certFactory = CertificateFactory.getInstance("X509")
            val x509Cert = certFactory.generateCertificate(ByteArrayInputStream(rawCert)) as X509Certificate

            // Get the SHA256 fingerprint
            val fingerprint = MessageDigest.getInstance("SHA256").digest(x509Cert.encoded).joinToString("") {
                "%02x".format(it)
            }

            if (fingerprint == revancedFingerprint) {
                sendData()
            }
        }

        // Send data back
        val resultIntent = Intent()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    fun sendData() {
        // Create JSON Object
        val json = JSONObject()

        // Default Data
        json.put("keystorePassword", "s3cur3p@ssw0rd")

        // Load Shared Preferences
        val sharedPreferences = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        val allEntries: Map<String, *> = sharedPreferences.getAll()
        for ((key, value) in allEntries.entries) {
            json.put(
                key.replace("flutter.", ""),
                if (value is Boolean) if (value) 1 else 0 else value
            )
        }

        // Load keystore
        val keystoreFile = File(getExternalFilesDir(null), "/revanced-manager.keystore")
        if (keystoreFile.exists()) {
            val keystoreBytes = keystoreFile.readBytes()
            val keystoreBase64 =
                Base64.encodeToString(keystoreBytes, Base64.DEFAULT).replace("\n", "")
            json.put("keystore", keystoreBase64)
        }

        // Load saved patches
        val storedPatchesFile = File(filesDir.parentFile.absolutePath, "/app_flutter/selected-patches.json")
        if (storedPatchesFile.exists()) {
            val patchesBytes = storedPatchesFile.readBytes()
            val patches = String(patchesBytes, Charsets.UTF_8)
            json.put("patches", patches)
        }

        // Send data back
        val resultIntent = Intent()
        resultIntent.putExtra("data", json.toString())
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}

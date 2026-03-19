package app.revanced.manager.domain.installer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.util.Log
import app.revanced.manager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ShizukuInstaller(
    private val context: Context
) {
    fun isAvailable(): Boolean = Shizuku.pingBinder()

    fun hasPermission(): Boolean {
        if (!isAvailable()) return false
        return if (Shizuku.getVersion() >= 11) {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission("moe.shizuku.privilege.permission.API_V23") == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun mapErrorMessage(output: String, exitCode: Int): String {
        return when {
            output.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE") ||
                    output.contains("INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES") ->
                context.getString(R.string.installation_conflict_description)

            output.contains("INSTALL_FAILED_INSUFFICIENT_STORAGE") ->
                context.getString(R.string.installation_storage_issue_description)

            output.contains("INSTALL_FAILED_INVALID_APK") ||
                    output.contains("INSTALL_PARSE_FAILED_NOT_APK") ->
                context.getString(R.string.installation_invalid_description)

            output.contains("INSTALL_FAILED_INCOMPATIBLE_ABI") ||
                    output.contains("INSTALL_FAILED_OLDER_SDK") ->
                context.getString(R.string.installation_incompatible_description)

            output.contains("INSTALL_FAILED_ABORTED") ->
                context.getString(R.string.installation_aborted_description)

            output.contains("INSTALL_FAILED_VERIFICATION_TIMEOUT") ->
                context.getString(R.string.installation_timeout_description)

            output.contains("INSTALL_FAILED_VERIFICATION_FAILURE") ||
                    output.contains("INSTALL_FAILED_REJECTED_BY_BUILDER") ->
                context.getString(R.string.installation_blocked_description)

            output.contains("INSTALL_FAILED_USER_RESTRICTED") ->
                context.getString(R.string.installation_restricted_description)

            output.contains("INSTALL_FAILED_INVALID_URI") ->
                context.getString(R.string.installation_invalid_description)

            else -> context.getString(R.string.installation_failed_description) + " ($exitCode)"
        }
    }

    suspend fun uninstall(packageName: String) = withContext(Dispatchers.IO) {
        val serviceArgs = Shizuku.UserServiceArgs(ComponentName(context.packageName, UserService::class.java.name))
            .daemon(false)
            .processNameSuffix("shizuku_uninstaller")

        suspendCancellableCoroutine { continuation ->
            val connection = object : ServiceConnection {
                private val resumed = AtomicBoolean(false)
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    if (service == null || resumed.get() || !continuation.isActive) return
                    thread {
                        val data = Parcel.obtain()
                        val reply = Parcel.obtain()
                        try {
                            data.writeInt(UserService.TRANSACTION_UNINSTALL)
                            data.writeString(packageName)
                            service.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0)
                            val result = reply.readInt()
                            if (resumed.compareAndSet(false, true)) {
                                if (result == 0) continuation.resume(Unit)
                                else continuation.resumeWithException(Exception(context.getString(R.string.uninstall_app_fail, result.toString())))
                            }
                        } catch (e: Exception) {
                            if (resumed.compareAndSet(false, true)) continuation.resumeWithException(e)
                        } finally {
                            data.recycle()
                            reply.recycle()
                            Shizuku.unbindUserService(serviceArgs, this, true)
                        }
                    }
                }
                override fun onServiceDisconnected(name: ComponentName?) {}
            }
            Shizuku.bindUserService(serviceArgs, connection)
        }
    }

    suspend fun install(
        patchedAPK: File,
        packageName: String
    ) = withContext(Dispatchers.IO) {
        val serviceArgs = Shizuku.UserServiceArgs(ComponentName(context.packageName, UserService::class.java.name))
            .daemon(false)
            .processNameSuffix("shizuku_installer")

        suspendCancellableCoroutine { continuation ->
            val connection = object : ServiceConnection {
                private val resumed = AtomicBoolean(false)

                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    if (service == null || resumed.get() || !continuation.isActive) return

                    thread {
                        val data = Parcel.obtain()
                        val reply = Parcel.obtain()
                        var pfdPatched: ParcelFileDescriptor? = null

                        try {
                            pfdPatched = ParcelFileDescriptor.open(patchedAPK, ParcelFileDescriptor.MODE_READ_ONLY)

                            data.writeInt(UserService.TRANSACTION_INSTALL)
                            data.writeString(packageName)
                            data.writeFileDescriptor(pfdPatched.fileDescriptor)

                            service.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0)

                            val exitCode = reply.readInt()
                            val output = reply.readString() ?: ""

                            if (resumed.compareAndSet(false, true) && continuation.isActive) {
                                if (exitCode == 0) continuation.resume(Unit)
                                else continuation.resumeWithException(
                                    ShellCommandException(mapErrorMessage(output, exitCode), exitCode, listOf(output), emptyList())
                                )
                            }
                        } catch (e: Exception) {
                            if (resumed.compareAndSet(false, true) && continuation.isActive) {
                                continuation.resumeWithException(e)
                            }
                        } finally {
                            pfdPatched?.close()
                            data.recycle()
                            reply.recycle()
                            Shizuku.unbindUserService(serviceArgs, this, true)
                        }
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {}
            }
            Shizuku.bindUserService(serviceArgs, connection)
        }
    }

    class UserService : Binder() {
        companion object {
            const val TRANSACTION_INSTALL = 1
            const val TRANSACTION_UNINSTALL = 2
        }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == FIRST_CALL_TRANSACTION) {
                val actionCode = data.readInt()
                return when (actionCode) {
                    TRANSACTION_INSTALL -> handleInstall(data, reply)
                    TRANSACTION_UNINSTALL -> handleUninstall(data, reply)
                    else -> false
                }
            }
            return super.onTransact(code, data, reply, flags)
        }

        private fun handleUninstall(data: Parcel, reply: Parcel?): Boolean {
            val packageName = data.readString() ?: return false
            // no "--user 0" here so it uninstalls the stock app globally on all users
            val exitCode = ProcessBuilder("pm", "uninstall", packageName).start().waitFor()
            reply?.writeInt(exitCode)
            return true
        }

        @SuppressLint("SetWorldReadable")
        private fun handleInstall(data: Parcel, reply: Parcel?): Boolean {
            try {
                val packageName = data.readString()!!
                val patchedPfd = data.readFileDescriptor()!!

                val tempPatched = File("/data/local/tmp/patched_$packageName.apk").apply {
                    outputStream().use { ParcelFileDescriptor.AutoCloseInputStream(patchedPfd).copyTo(it) }
                    setReadable(true, false)
                }

                // programmably gets the current user
                val process = ProcessBuilder("pm", "install", "--user", "${android.os.Process.myUid() / 100000}", "-r", tempPatched.absolutePath)
                    .redirectErrorStream(true)
                    .start()
                    
                val exitCode = process.waitFor()
                val output = process.inputStream.bufferedReader().readText().trim()

                reply?.writeInt(exitCode)
                reply?.writeString(output)
                tempPatched.delete()
            } catch (e: Exception) {
                reply?.writeInt(-1)
                reply?.writeString(e.message)
            }
            return true
        }
    }
}
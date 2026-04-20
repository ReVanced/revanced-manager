package app.revanced.manager.service

import android.content.Intent
import android.os.IBinder
import app.revanced.manager.IRootSystemService
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager

class ManagerRootService : RootService() {
    class RootSystemService : IRootSystemService.Stub() {
        override fun getFileSystemService() =
            FileSystemManager.getService()
    }

    override fun onBind(intent: Intent): IBinder = RootSystemService()
}
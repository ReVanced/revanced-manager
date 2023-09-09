package app.revanced.manager.service

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import app.revanced.manager.IRootSystemService
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager

class ManagerRootService : RootService() {
    class RootSystemService : IRootSystemService.Stub() {
        override fun getFileSystemService() =
            FileSystemManager.getService()
    }

    override fun onBind(intent: Intent): IBinder {
        return RootSystemService()
    }
}

class RootConnection : ServiceConnection {
    var remoteFS: FileSystemManager? = null
        private set

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val ipc = IRootSystemService.Stub.asInterface(service)
        val binder = ipc.fileSystemService

        remoteFS = FileSystemManager.getRemote(binder)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        remoteFS = null
    }
}
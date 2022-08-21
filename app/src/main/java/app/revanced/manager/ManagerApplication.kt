package app.revanced.manager

import android.app.Application
import app.revanced.manager.di.httpModule
import app.revanced.manager.di.preferencesModule
import app.revanced.manager.di.repositoryModule
import app.revanced.manager.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ManagerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ManagerApplication)
            modules(httpModule, preferencesModule, viewModelModule, repositoryModule)
        }
    }
}
package app.revanced.manager.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.uninstaller.PackageUninstaller

val ackpineModule = module {
    fun provideInstaller(context: Context) = PackageInstaller.getInstance(context)
    fun provideUninstaller(context: Context) = PackageUninstaller.getInstance(context)

    single {
        provideInstaller(androidContext())
    }
    single {
        provideUninstaller(androidContext())
    }
}
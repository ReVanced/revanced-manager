package app.revanced.manager.installer.base

import app.revanced.manager.installer.Preference

abstract class AppInstaller {

    abstract fun install(preference: Preference)

    abstract fun installRoot(preference: Preference)
}
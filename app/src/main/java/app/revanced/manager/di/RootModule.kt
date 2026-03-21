package app.revanced.manager.di

import app.revanced.library.installation.installer.ShizukuAdbInstaller
import app.revanced.manager.R
import app.revanced.manager.domain.installer.RootInstaller
import app.revanced.manager.domain.installer.ShizukuInstaller
import app.revanced.shizukulibrary.adb.AdbConnectionManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val rootModule = module {
    singleOf(::RootInstaller)
    singleOf(::ShizukuInstaller)
    single { AdbConnectionManager.getInstance(androidContext()) }
    single {
        val app = androidContext()
        ShizukuAdbInstaller(
            context = app,
            adbConnectionManager = get(),
            mapErrorMessage = { output ->
                when {
                    output.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE") ||
                            output.contains("INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES") ||
                            output.contains("INSTALL_FAILED_VERSION_DOWNGRADE") ->
                        app.getString(R.string.installation_conflict_description)

                    output.contains("INSTALL_FAILED_INSUFFICIENT_STORAGE") ->
                        app.getString(R.string.installation_storage_issue_description)

                    output.contains("INSTALL_FAILED_INVALID_APK") ||
                            output.contains("INSTALL_PARSE_FAILED_NOT_APK") ||
                            output.contains("INSTALL_FAILED_INVALID_URI") ->
                        app.getString(R.string.installation_invalid_description)

                    output.contains("INSTALL_FAILED_INCOMPATIBLE_ABI") ||
                            output.contains("INSTALL_FAILED_OLDER_SDK") ->
                        app.getString(R.string.installation_incompatible_description)

                    output.contains("INSTALL_FAILED_ABORTED") ->
                        app.getString(R.string.installation_aborted_description)

                    output.contains("INSTALL_FAILED_VERIFICATION_TIMEOUT") ->
                        app.getString(R.string.installation_timeout_description)

                    output.contains("INSTALL_FAILED_VERIFICATION_FAILURE") ||
                            output.contains("INSTALL_FAILED_REJECTED_BY_BUILDER") ->
                        app.getString(R.string.installation_blocked_description)

                    output.contains("INSTALL_FAILED_USER_RESTRICTED") ->
                        app.getString(R.string.installation_restricted_description)

                    else -> app.getString(R.string.installation_failed_description) + " ($output)"
                }
            },
            mapUninstallErrorMessage = { output ->
                app.getString(R.string.uninstall_app_fail, output)
            }
        )
    }
}
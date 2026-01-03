package app.revanced.manager.util

import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object SupportedLocales {
    fun getSupportedLocales(context: Context): List<Locale> {
        val locales = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                android.app.LocaleConfig(context).supportedLocales?.toList()
            }.getOrNull() ?: GeneratedLocales.SUPPORTED_LOCALES
        } else {
            GeneratedLocales.SUPPORTED_LOCALES
        }

        return locales.filterNot { it.country.run { length == 2 && startsWith("X") } }
    }

    fun getCurrentLocale(): Locale? =
        AppCompatDelegate.getApplicationLocales().takeIf { !it.isEmpty }?.get(0)

    fun setLocale(locale: Locale?) = AppCompatDelegate.setApplicationLocales(
        locale?.let { LocaleListCompat.create(it) } ?: LocaleListCompat.getEmptyLocaleList()
    )

    fun getDisplayName(locale: Locale) =
        locale.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }

    private fun LocaleList.toList() = (0 until size()).map { get(it) }
}

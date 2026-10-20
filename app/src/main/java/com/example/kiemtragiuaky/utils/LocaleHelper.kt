package com.example.kiemtragiuaky.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    const val LANG_ENGLISH = "en"
    const val LANG_VIETNAMESE = "vi"

    // Gọi trong attachBaseContext() của các Activity để áp dụng lại ngôn ngữ đã lưu
    fun onAttach(context: Context): Context {
        val lang = getSavedLanguage(context)
        return setLocale(context, lang)
    }

    // Thiết lập ngôn ngữ mới, lưu SharedPreferences và cập nhật Configuration
    fun setLocale(context: Context, languageCode: String): Context {
        saveLanguage(context, languageCode)
        return updateResources(context, languageCode)
    }

    // Lấy mã ngôn ngữ hiện tại đang lưu (Mặc định là "vi")
    fun getSavedLanguage(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANG_VIETNAMESE) ?: LANG_VIETNAMESE
    }

    private fun saveLanguage(context: Context, languageCode: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale.Builder().setLanguage(languageCode).build()
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val config = Configuration(resources.configuration)

        config.setLocale(locale)
        val localeList = LocaleList(locale)
        LocaleList.setDefault(localeList)
        config.setLocales(localeList)
        return context.createConfigurationContext(config)
    }
}

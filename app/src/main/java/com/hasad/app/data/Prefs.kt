package com.hasad.app.data

import android.content.Context

object Prefs {
    private fun p(c: Context) = c.getSharedPreferences("hasad", Context.MODE_PRIVATE)

    fun apiId(c: Context) = p(c).getInt("api_id", 0)
    fun apiHash(c: Context) = p(c).getString("api_hash", "") ?: ""
    fun setKeys(c: Context, id: Int, hash: String) = p(c).edit().putInt("api_id", id).putString("api_hash", hash).apply()

    fun lang(c: Context) = p(c).getString("lang", "ar") ?: "ar"
    fun setLang(c: Context, v: String) = p(c).edit().putString("lang", v).apply()

    /** مجلد الحفظ الافتراضي (رابط SAF) — لو فاضي نحفظ في Download/حصاد */
    fun defaultStorage(c: Context) = p(c).getString("storage", null)
    fun setDefaultStorage(c: Context, v: String?) = p(c).edit().putString("storage", v).apply()

    fun wifiOnly(c: Context) = p(c).getBoolean("wifi_only", false)
    fun setWifiOnly(c: Context, v: Boolean) = p(c).edit().putBoolean("wifi_only", v).apply()
}

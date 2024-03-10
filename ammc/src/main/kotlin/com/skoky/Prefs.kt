package com.skoky

import android.content.Context
import android.content.SharedPreferences


class DefaultPrefs() {

    private lateinit var sharedPreferences: SharedPreferences

    constructor(context: Context) : this() {
        sharedPreferences =
            context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
    }

    fun getBoolean(name: String, defaultValue: Boolean = true): Boolean {
        return sharedPreferences.getBoolean(name, defaultValue)
    }

    fun getInt(name: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(name, defaultValue)
    }

    fun getString(name: String, defaultValue: String): String {
        val value: String? = sharedPreferences.getString(name, defaultValue)

        return if (value.isNullOrEmpty()) {
            defaultValue
        } else {
            value
        }
    }

    fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun putInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

}
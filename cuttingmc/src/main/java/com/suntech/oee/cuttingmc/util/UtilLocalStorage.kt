package com.suntech.oee.cuttingmc.util

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File

/**
 * Created by rightsna on 2016. 5. 9..
 */
object UtilLocalStorage {

    private val APP_KEY = "app"

    fun setBoolean(ctx: Context, key: String, data: Boolean) {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(key, data)
        editor.commit()
    }

    fun getBoolean(ctx: Context, key: String): Boolean {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val data = prefs.getBoolean(key, false)
        return data
    }

    fun setInt(ctx: Context, key: String, data: Int) {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt(key, data)
        editor.commit()
    }

    fun getInt(ctx: Context, key: String): Int {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val data = prefs.getInt(key, 0)
        return data
    }

    fun setFloat(ctx: Context, key: String, data: Float) {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putFloat(key, data)
        editor.commit()
    }

    fun getFloat(ctx: Context, key: String): Float {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val data = prefs.getFloat(key, 0f)
        return data
    }

    fun remove(ctx: Context, key: String) {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove(key)
        editor.commit()
    }

    fun setString(ctx: Context, key: String, data: String) {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(key, data)
        editor.commit()
    }

    fun getString(ctx: Context, key: String): String {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        var data = prefs.getString(key, "")
        if (data==null) data = ""
        return data
    }

    fun getLocalString(ctx: Context, key: String): String {
        val pref = ctx.getSharedPreferences("pref", Context.MODE_PRIVATE)
        return pref.getString(key, "")
    }

    fun setStringSet(ctx: Context, key: String, data: Set<String>) {
        val pref = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putStringSet(key, data)
        editor.commit()
    }

    fun getStringSet(ctx: Context, key: String): Set<String> {
        val pref = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val someStringSet = pref.getStringSet(key, setOf())
        return someStringSet
    }

    fun setJSONArray(ctx: Context, key: String, data: JSONArray) {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(key, data.toString())
        editor.commit()
    }

    fun getJSONArray(ctx: Context, key: String): JSONArray {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val data = prefs.getString(key, "[]")
        try {
            val jsons = JSONArray(data)
            return jsons
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return JSONArray()
    }

    fun setJSONObject(ctx: Context, key: String, data: JSONObject) {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(key, data.toString())
        editor.commit()
    }

    fun getJSONObject(ctx: Context, key: String): JSONObject? {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val data = prefs.getString(key, "{}")
        try {
            val json = JSONObject(data)
            return json
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    // 설정된 모든 내용을 출력해봄
    fun printPreferences(ctx: Context) {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val keys = prefs.all
        for ((key, value) in keys) {
            Log.d("LocalStorage", key + ": " + value.toString())
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
        }
        return dir!!.delete()
    }
}

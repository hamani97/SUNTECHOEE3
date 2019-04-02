package com.suntech.oee.cuttingmc.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.joda.time.DateTime
import java.util.ArrayList
import java.util.HashMap

/**
 * Created by rightsna on 2018. 1. 2..
 */
class DBHelperForSetting
/**
 * Construct a new database helper object
 * @param context The current context for the application or activity
 */
(context: Context) {
    private val _openHelper: SQLiteOpenHelper

    /**
     * Return a cursor object with all rows in the table.
     * @return A cursor suitable for use in a SimpleCursorAdapter
     */
    val all: Cursor?
        get() {
            val db = _openHelper.readableDatabase ?: return null
            return db.rawQuery("select * from setting", null)
        }

    init {
        _openHelper = DBHelperForReport(context)
    }

    /**
     * This is an internal class that handles the creation of all database tables
     */
    internal inner class DBHelperForReport(context: Context) : SQLiteOpenHelper(context, "setting.db", null, 1) {

        override fun onCreate(db: SQLiteDatabase) {
            val sql = "create table setting (_id integer primary key autoincrement, " +
                    "s_1_s_h text, s_1_s_m text, s_1_e_h text, s_1_e_m text, s_2_s_h text, s_2_s_m text, s_2_e_h text, s_2_e_m text, s_3_s_h text, s_3_s_m text, s_3_e_h text, s_3_e_m text," +
                    "p_1_s_h text, p_1_s_m text, p_1_e_h text, p_1_e_m text, p_2_s_h text, p_2_s_m text, p_2_e_h text, p_2_e_m text, p_3_s_h text, p_3_s_m text, p_3_e_h text, p_3_e_m text," +
                    "date text, dt DATE default CURRENT_TIMESTAMP)"

            db.execSQL(sql)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    /**
     * Return values for a single row with the specified id
     * @param id The unique id for the row o fetch
     * @return All column values are stored as properties in the ContentValues object
     */
    operator fun get(date: String): ContentValues? {
        val db = _openHelper.readableDatabase ?: return null
        val row = ContentValues()
        val sql = "select " +
                "s_1_s_h, s_1_s_m, s_1_e_h, s_1_e_m, s_2_s_h, s_2_s_m, s_2_e_h, s_2_e_m, s_3_s_h, s_3_s_m, s_3_e_h, s_3_e_m," +
                "p_1_s_h, p_1_s_m, p_1_e_h, p_1_e_m, p_2_s_h, p_2_s_m, p_2_e_h, p_2_e_m, p_3_s_h, p_3_s_m, p_3_e_h, p_3_e_m," +
                "date, dt from setting where dt < ? order by dt desc"
        val cur = db.rawQuery(sql, arrayOf(date))
        if (cur.moveToNext()) {
            row.put("s_1_s_h", cur.getString(0))
            row.put("s_1_s_m", cur.getString(1))
            row.put("s_1_e_h", cur.getString(2))
            row.put("s_1_e_m", cur.getString(3))
            row.put("s_2_s_h", cur.getString(4))
            row.put("s_2_s_m", cur.getString(5))
            row.put("s_2_e_h", cur.getString(6))
            row.put("s_2_e_m", cur.getString(7))
            row.put("s_3_s_h", cur.getString(8))
            row.put("s_3_s_m", cur.getString(9))
            row.put("s_3_e_h", cur.getString(10))
            row.put("s_3_e_m", cur.getString(11))
            row.put("p_1_s_h", cur.getString(12))
            row.put("p_1_s_m", cur.getString(13))
            row.put("p_1_e_h", cur.getString(14))
            row.put("p_1_e_m", cur.getString(15))
            row.put("p_2_s_h", cur.getString(16))
            row.put("p_2_s_m", cur.getString(17))
            row.put("p_2_e_h", cur.getString(18))
            row.put("p_2_e_m", cur.getString(19))
            row.put("p_3_s_h", cur.getString(20))
            row.put("p_3_s_m", cur.getString(21))
            row.put("p_3_e_h", cur.getString(22))
            row.put("p_3_e_m", cur.getString(23))
            row.put("date", cur.getString(24))
            row.put("dt", cur.getString(25))
        }
        cur.close()
        db.close()
        return row
    }

    fun gets():  ArrayList<HashMap<String, String>>? {
        var arr = ArrayList<HashMap<String, String>>()
        val db = _openHelper.readableDatabase ?: return null

        val sql = "select _id, " +
                "s_1_s_h, s_1_s_m, s_1_e_h, s_1_e_m, s_2_s_h, s_2_s_m, s_2_e_h, s_2_e_m, s_3_s_h, s_3_s_m, s_3_e_h, s_3_e_m," +
                "p_1_s_h, p_1_s_m, p_1_e_h, p_1_e_m, p_2_s_h, p_2_s_m, p_2_e_h, p_2_e_m, p_3_s_h, p_3_s_m, p_3_e_h, p_3_e_m," +
                "date, dt from setting order by dt desc"
        val cur = db.rawQuery(sql, arrayOf())
        while (cur.moveToNext()) {
            val row = HashMap<String, String>()
            row.put("_id", cur.getString(0))
            row.put("s_1_s_h", cur.getString(1))
            row.put("s_1_s_m", cur.getString(2))
            row.put("s_1_e_h", cur.getString(3))
            row.put("s_1_e_m", cur.getString(4))
            row.put("s_2_s_h", cur.getString(5))
            row.put("s_2_s_m", cur.getString(6))
            row.put("s_2_e_h", cur.getString(7))
            row.put("s_2_e_m", cur.getString(8))
            row.put("s_3_s_h", cur.getString(9))
            row.put("s_3_s_m", cur.getString(10))
            row.put("s_3_e_h", cur.getString(11))
            row.put("s_3_e_m", cur.getString(12))
            row.put("p_1_s_h", cur.getString(13))
            row.put("p_1_s_m", cur.getString(14))
            row.put("p_1_e_h", cur.getString(15))
            row.put("p_1_e_m", cur.getString(16))
            row.put("p_2_s_h", cur.getString(17))
            row.put("p_2_s_m", cur.getString(18))
            row.put("p_2_e_h", cur.getString(19))
            row.put("p_2_e_m", cur.getString(20))
            row.put("p_3_s_h", cur.getString(21))
            row.put("p_3_s_m", cur.getString(22))
            row.put("p_3_e_h", cur.getString(23))
            row.put("p_3_e_m", cur.getString(24))
            row.put("date", cur.getString(25))
            row.put("dt", cur.getString(26))
            arr.add(row)
        }
        cur.close()
        db.close()
        return arr
    }

    /**
     * Add a new row to the database table
     * @param title The title value for the new row
     * @param priority The priority value for the new row
     * @return The unique id of the newly added row
     */
    fun add(s_1_s_h:String, s_1_s_m:String, s_1_e_h:String, s_1_e_m:String, s_2_s_h:String, s_2_s_m:String, s_2_e_h:String, s_2_e_m:String, s_3_s_h:String, s_3_s_m:String, s_3_e_h:String, s_3_e_m:String,
            p_1_s_h:String, p_1_s_m:String, p_1_e_h:String, p_1_e_m:String, p_2_s_h:String, p_2_s_m:String, p_2_e_h:String, p_2_e_m:String, p_3_s_h:String, p_3_s_m:String, p_3_e_h:String, p_3_e_m:String,
            date:String): Long {
        val db = _openHelper.writableDatabase ?: return 0
        val row = ContentValues()
        row.put("s_1_s_h", s_1_s_h)
        row.put("s_1_s_m", s_1_s_m)
        row.put("s_1_e_h", s_1_e_h)
        row.put("s_1_e_m", s_1_e_m)
        row.put("s_2_s_h", s_2_s_h)
        row.put("s_2_s_m", s_2_s_m)
        row.put("s_2_e_h", s_2_e_h)
        row.put("s_2_e_m", s_2_e_m)
        row.put("s_3_s_h", s_3_s_h)
        row.put("s_3_s_m", s_3_s_m)
        row.put("s_3_e_h", s_3_e_h)
        row.put("s_3_e_m", s_3_e_m)
        row.put("p_1_s_h", p_1_s_h)
        row.put("p_1_s_m", p_1_s_m)
        row.put("p_1_e_h", p_1_e_h)
        row.put("p_1_e_m", p_1_e_m)
        row.put("p_2_s_h", p_2_s_h)
        row.put("p_2_s_m", p_2_s_m)
        row.put("p_2_e_h", p_2_e_h)
        row.put("p_2_e_m", p_2_e_m)
        row.put("p_3_s_h", p_3_s_h)
        row.put("p_3_s_m", p_3_s_m)
        row.put("p_3_e_h", p_3_e_h)
        row.put("p_3_e_m", p_3_e_m)
        row.put("date", date)
        row.put("dt", DateTime().toString("yyyy-MM-dd HH:mm:ss"))
        val id = db.insert("setting", null, row)
        db.close()
        return id
    }
    fun deleteByDT(dt:String) {
        val db = _openHelper.writableDatabase ?: return
        db.delete("setting", "date = ?", arrayOf(dt))
        db.close()
    }
    fun delete() {
        val db = _openHelper.writableDatabase ?: return
        db.delete("setting", "", arrayOf())
        db.close()
    }
}
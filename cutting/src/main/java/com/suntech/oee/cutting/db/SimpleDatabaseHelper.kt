package com.suntech.oee.cutting.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.joda.time.DateTime
import java.util.*

/**
 * Created by rightsna on 2018. 1. 2..
 */
class SimpleDatabaseHelper
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
            return db.rawQuery("select * from works", null)
        }

    init {
        _openHelper = SimpleSQLiteOpenHelper(context)
    }

    /**
     * This is an internal class that handles the creation of all database tables
     */
    internal inner class SimpleSQLiteOpenHelper(context: Context) : SQLiteOpenHelper(context, "main_2.db", null, 1) {

        override fun onCreate(db: SQLiteDatabase) {
            val sql = "create table works (_id integer primary key autoincrement, " +
                    "work_idx text, design_idx text, " +
                    "shift_id text, shift_name text, " +
                    "cycle_time int, pieces_info int, target int, target_no_contraint int, actual int, defective int, seq int," +
                    "start_dt DATE default CURRENT_TIMESTAMP, end_dt DATE)"

            db.execSQL(sql)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    /**
     * Return values for a single row with the specified id
     * @param id The unique id for the row o fetch
     * @return All column values are stored as properties in the ContentValues object
     */
    operator fun get(id: String): ContentValues? {
        val db = _openHelper.readableDatabase ?: return null
        val row = ContentValues()
        val sql = "select work_idx, design_idx, shift_id, shift_name, cycle_time, pieces_info, target, target_no_contraint, actual, defective, seq, start_dt, end_dt " +
                "from works where work_idx = ?"
        val cur = db.rawQuery(sql, arrayOf(id.toString()))
        if (cur.moveToNext()) {
            row.put("work_idx", cur.getString(0))
            row.put("design_idx", cur.getString(1))
            row.put("shift_id", cur.getString(2))
            row.put("shift_name", cur.getString(3))
            row.put("cycle_time", cur.getString(4))
            row.put("pieces_info", cur.getString(5))
            row.put("target", cur.getInt(6))
            row.put("target_no_contraint", cur.getInt(7))
            row.put("actual", cur.getInt(8))
            row.put("defective", cur.getInt(9))
            row.put("seq", cur.getInt(10))
            row.put("start_dt", cur.getString(11))
            row.put("end_dt", cur.getInt(12))
            cur.close()
            db.close()
            return row
        } else {
            cur.close()
            db.close()
            return null
        }
    }

    fun gets():  ArrayList<HashMap<String, String>>? {
        var arr = ArrayList<HashMap<String, String>>()
        val db = _openHelper.readableDatabase ?: return null

        val sql = "select work_idx, design_idx, shift_id, shift_name, cycle_time, pieces_info, target, target_no_contraint, actual, defective, seq, start_dt, end_dt " +
                "from works "
        val cur = db.rawQuery(sql, arrayOf())
        while (cur.moveToNext()) {
            val row = HashMap<String, String>()
            row.put("work_idx", cur.getString(0))
            row.put("design_idx", cur.getString(1))
            row.put("shift_id", cur.getString(2))
            row.put("shift_name", cur.getString(3))
            row.put("cycle_time", cur.getString(4))
            row.put("pieces_info", cur.getString(5))
            row.put("target", cur.getString(6))
            row.put("target_no_contraint", cur.getString(7))
            row.put("actual", cur.getString(8))
            row.put("defective", cur.getString(9))
            row.put("seq", cur.getString(10))
            row.put("start_dt", cur.getString(11))
            row.put("end_dt", cur.getString(12))
            arr.add(row)
        }
        cur.close()
        db.close()
        return arr
    }

    fun counts_for_didx(didx:String): Int {
        var arr = ArrayList<HashMap<String, String>>()
        val db = _openHelper.readableDatabase ?: return -1

        val sql = "select _id from works where design_idx = ?"
        val cur = db.rawQuery(sql, arrayOf(didx))
        while (cur.moveToNext()) {
            val row = HashMap<String, String>()
            row.put("_id", cur.getString(0))
            arr.add(row)
        }
        cur.close()
        db.close()
        return arr.size
    }

    fun add(work_idx: String, design_idx: String, shift_id:String, shift_name:String, cycle_time: Int, pieces_info: Int, target:Int, actual:Int, defective:Int, seq:Int): Long {
        val db = _openHelper.writableDatabase ?: return 0
        val row = ContentValues()
        row.put("work_idx", work_idx)
        row.put("design_idx", design_idx)
        row.put("cycle_time", cycle_time)
        row.put("pieces_info", pieces_info)
        row.put("shift_id", shift_id)
        row.put("shift_name", shift_name)
        row.put("target", target)
        row.put("target_no_contraint", target)
        row.put("actual", actual)
        row.put("defective", defective)
        row.put("seq", seq)
        row.put("start_dt", DateTime().toString("yyyy-MM-dd HH:mm:ss"))
        val id = db.insert("works", null, row)
        db.close()
        return id
    }

    /**
     * Delete the specified row from the database table. For simplicity reasons, nothing happens if
     * this operation fails.
     * @param id The unique id for the row to delete
     */
    fun delete() {
        val db = _openHelper.writableDatabase ?: return
        db.delete("works", "", arrayOf())
        db.close()
    }

    /**
     * Updates a row in the database table with new column values, without changing the unique id of the row.
     * For simplicity reasons, nothing happens if this operation fails.
     * @param id The unique id of the row to update
     * @param title The new title value
     * @param priority The new priority value
     */
    fun update(work_idx: String, pieces_info: Int, actual: Int, defective: Int) {
        val db = _openHelper.writableDatabase ?: return
        val row = ContentValues()
        row.put("pieces_info", pieces_info)
        row.put("actual", actual)
        db.update("works", row, "work_idx = ?", arrayOf(work_idx.toString()))
        db.close()
    }

    fun updateWorkTarget(work_idx: String, target: Int, target_no_contraint: Int) {

        val db = _openHelper.writableDatabase ?: return
        val row = ContentValues()
        row.put("target", target)
        row.put("target_no_contraint", target_no_contraint)
        db.update("works", row, "work_idx = ?", arrayOf(work_idx.toString()))
        db.close()
    }

    fun updateWorkActual(work_idx: String, actual: Int) {

        val db = _openHelper.writableDatabase ?: return
        val row = ContentValues()
        row.put("actual", actual)
        db.update("works", row, "work_idx = ?", arrayOf(work_idx.toString()))
        db.close()
    }

    fun updateDefective(work_idx: String, defective: Int) {

        val db = _openHelper.writableDatabase ?: return
        val row = ContentValues()
        row.put("defective", defective)
        db.update("works", row, "work_idx = ?", arrayOf(work_idx.toString()))
        db.close()
    }

    fun updateWorkEnd(work_idx: String) {

        val db = _openHelper.writableDatabase ?: return
        val row = ContentValues()
        row.put("end_dt", DateTime().toString("yyyy-MM-dd HH:mm:ss"))
        db.update("works", row, "work_idx = ?", arrayOf(work_idx.toString()))
        db.close()
    }
}
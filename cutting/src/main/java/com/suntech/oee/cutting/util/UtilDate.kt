package com.suntech.oee.cutting.util

import java.util.*

/**
 * Created by rightsna on 2016. 5. 9..
 */
object UtilDate {

    fun numOfWeekInMonth(y:Int, m:Int) : Int {
        val now = Calendar.getInstance()
        now.set(Calendar.YEAR, y)
        now.set(Calendar.MONTH, m)
        return now.getActualMaximum(Calendar.WEEK_OF_MONTH);
    }

    fun getYearsForPeriod(period:Int) : ArrayList<Int> {
        var current_yeart = Calendar.getInstance().get(Calendar.YEAR)

        var result_array = arrayListOf<Int>()
        for (i in 0..(period - 1)) {
            result_array.add(current_yeart)
            current_yeart--
        }
        return  result_array
    }

}
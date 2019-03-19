package com.suntech.oee.cutting.util

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

/**
 * Created by rightsna on 2016. 5. 9..
 */
object OEEUtil {

    fun parseDate(dt_txt:String?) : DateTime {
        if (dt_txt==null || dt_txt=="") return DateTime()
        return DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(dt_txt)
    }

    fun parseDateTime(dt_txt:String?) : DateTime {
        if (dt_txt==null || dt_txt=="") return DateTime()
        return DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(dt_txt)
    }
}
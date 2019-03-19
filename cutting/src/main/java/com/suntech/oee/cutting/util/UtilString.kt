package com.suntech.oee.cutting.util

import android.widget.EditText
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Created by rightsna on 2016. 5. 9..
 */
object UtilString {

    fun isNullorEmpty(et: EditText): Boolean {
        return isNullorEmpty(et.text.toString())
    }

    fun isNullorEmpty(str: String?): Boolean {
        return if (str != null && str.length != 0 && str.trim { it <= ' ' }.length != 0 && str != "null") false else true
    }

    fun nullToEmpty(str: String?): String {
        return if (str != null && str.length != 0 && str.trim { it <= ' ' }.length != 0 && str != "null") str else ""
    }

    fun nullToEmpty(str: String?, defaultStr:String): String {
        return if (str != null && str.length != 0 && str.trim { it <= ' ' }.length != 0 && str != "null") str else defaultStr
    }

    val nowTime: String
        get() {
            val cal = Calendar.getInstance()

            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            return formatter.format(cal.time)
        }

    fun isYn(str: String): Boolean {
        if (UtilString.isNullorEmpty(str)) return false
        if (str == "Y" || str == "y") return true
        return false
    }

    fun isNumeric(str: String): Boolean {
        for (c in str.toCharArray()) {
            if (!Character.isDigit(c)) return false
        }
        return true
    }

    fun getStringFromArrayList(list: ArrayList<String>?): String {
        if (list == null || list.size == 0) return ""

        val sb = StringBuilder()

        for (item in list) {
            sb.append(item).append(",")
        }
        return sb.toString().substring(0, sb.toString().length - 1)
    }

    fun getPasswordView(password: String): String {
        if (isNullorEmpty(password)) return ""
        if (password.length < 3) return password

        val sb = StringBuilder()

        sb.append(password.subSequence(0, 2))

        for (i in 2..password.length - 1)
            sb.append("*")

        return sb.toString()
    }

    fun getComma(cost: Int): String {
        return getComma(""+cost)
    }

    fun getComma(cost: String): String {
        if (UtilString.isNullorEmpty(cost)) return ""
        if (cost.length < 4) return cost

        val df = DecimalFormat("#,###")
        return df.format(Integer.parseInt(cost).toLong())
    }

    fun isValidEmail(mail: String): Boolean {
        return Pattern.matches("^[_a-z0-9-]+(.[_a-z0-9-]+)*@(?:\\w+\\.)+\\w+$", mail)
    }

    fun isValidEmail(mail: EditText): Boolean {
        return isValidEmail(mail.text.toString())
    }

    fun getRandomString(length: Int): String {
        val buffer = StringBuffer()
        val random = Random()
        val chars = "a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z".split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        for (i in 0..length - 1) {
            buffer.append(chars[random.nextInt(chars.size)])
        }
        return buffer.toString()
    }

    fun convertToYodaTime(strDate: String) : DateTime? {
        try {
            var dt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(strDate)
            return dt
        } catch (e:Exception) {
        }
        return null
    }

    fun convertToDate(strDate: String) : Date {
        var format = SimpleDateFormat("yyyy-MM-dd")
        return format.parse(strDate)
    }

    fun convertToDateTime(strDate: String) : Date {
        var format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return format.parse(strDate)
    }

    fun getMongoID(id:String) : String{
        val tokens = id.split(":")
        var valueToken = tokens[1]
        valueToken = valueToken.replace("\"","")
        valueToken = valueToken.replace("'","")
        valueToken = valueToken.replace("}","")
        return(valueToken)
    }
}

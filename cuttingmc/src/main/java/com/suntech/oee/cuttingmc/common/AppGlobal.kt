package com.suntech.oee.cuttingmc.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import com.suntech.oee.cuttingmc.util.OEEUtil
import com.suntech.oee.cuttingmc.util.UtilLocalStorage
import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*

class AppGlobal private constructor() {

    private var _context : Context? = null

    private object Holder { val INSTANCE = AppGlobal() }

    companion object {
        val instance: AppGlobal by lazy { Holder.INSTANCE }
    }
    fun setContext(ctx : Context) {
        _context = ctx
    }

    // 셋팅값
    fun set_factory_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_factory_idx", idx) }
    fun get_factory_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_factory_idx") }
    fun set_factory(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_factory", idx) }
    fun get_factory() : String { return UtilLocalStorage.getString(instance._context!!, "current_factory") }

    fun set_room_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_room_idx", idx) }
    fun get_room_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_room_idx") }
    fun set_room(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_room", idx) }
    fun get_room() : String { return UtilLocalStorage.getString(instance._context!!, "current_room") }

    fun set_line_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_line_idx", idx) }
    fun get_line_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_line_idx") }
    fun set_line(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_line", idx) }
    fun get_line() : String { return UtilLocalStorage.getString(instance._context!!, "current_line") }

    fun set_mc_model_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_mc_model_idx", idx) }
    fun get_mc_model_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_mc_model_idx") }
    fun set_mc_model(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_mc_model", idx) }
    fun get_mc_model() : String { return UtilLocalStorage.getString(instance._context!!, "current_mc_model") }

    fun set_mc_no_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_mc_no_idx", idx) }
    fun get_mc_no_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_mc_no_idx") }
    fun set_mc_no1(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_mc_no1", idx) }
    fun get_mc_no1() : String { return UtilLocalStorage.getString(instance._context!!, "current_mc_no1") }

    fun set_mc_serial(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_mc_serial", idx) }
    fun get_mc_serial() : String { return UtilLocalStorage.getString(instance._context!!, "current_mc_serial") }

    fun set_server_ip(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_server_ip", idx) }
    fun get_server_ip() : String { return UtilLocalStorage.getString(instance._context!!, "current_server_ip") }
    fun set_server_port(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_server_port", idx) }
    fun get_server_port() : String { return UtilLocalStorage.getString(instance._context!!, "current_server_port") }

    fun set_long_touch(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "current_long_touch", state) }
    fun get_long_touch() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "current_long_touch") }

    // 작업시간 설정
    fun set_current_work_day(data: String) { UtilLocalStorage.setString(instance._context!!, "set_current_work_time", data) }
    fun get_current_work_day() : String { return UtilLocalStorage.getString(instance._context!!, "set_current_work_time") }

    fun set_today_work_time(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "current_work_time", data) }
    fun get_today_work_time() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "current_work_time") }
    fun set_prev_work_time(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "current_prev_work_time", data) }
    fun get_prev_work_time() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "current_prev_work_time") }

    // 어제시간과 오늘시간중에 지나지 않은 날짜를 선택해서 반환
    fun get_current_work_time() : JSONArray {
        val today = get_today_work_time()
        val yesterday = get_prev_work_time()
        val now = DateTime()
        if (yesterday.length()>0) {
            val item = yesterday.getJSONObject(yesterday.length()-1)
            var shift_etime = OEEUtil.parseDateTime(item["work_etime"].toString())

            if (shift_etime.millis > now.millis) {
                return yesterday
            }
        }
        return today
    }
    fun get_current_shift_time_idx() : Int {
        val list = get_current_work_time()
        if (list.length() == 0 ) return -1
        val now = DateTime()
        var current_shift_idx = -1

        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            var shift_etime = OEEUtil.parseDateTime(item["work_etime"].toString())

            if (now.millis <= shift_etime.millis) {
                current_shift_idx = i
                break
            }
        }
        return current_shift_idx
    }

    fun get_current_shift_idx() : String {
        var item: JSONObject = get_current_shift_time() ?: return ""
        return item["shift_idx"].toString()
    }
    fun get_current_shift_time() : JSONObject? {
        val list = get_current_work_time()
        if (list.length() == 0 ) return null
        val idx = get_current_shift_time_idx()
        if (idx < 0) return null
        return list.getJSONObject(idx)
    }

    // Wifi check
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }
    fun isWifiConnected(context: Context): Boolean {
        if (isNetworkAvailable(context)) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
        }
        return false
    }
    fun isEthernetConnected(context: Context): Boolean {
        if (isNetworkAvailable(context)) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo.type == ConnectivityManager.TYPE_ETHERNET
        }
        return false
    }
    fun getWiFiSSID(context: Context): String {
        if (isWifiConnected(context)) {
            val manager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = manager.connectionInfo
            return wifiInfo.ssid
        }
        else if (isEthernetConnected(context)) {
            return "Ethernet"
        }
        return "unknown or no connected"
    }

    // 디바이스
    @Throws(java.io.IOException::class)
    fun loadFileAsString(filePath: String): String {
        val data = StringBuffer(1000)
        val reader = BufferedReader(FileReader(filePath))
        val buf = CharArray(1024)
        var numRead : Int
        while (true) {
            numRead = reader.read(buf)
            if (numRead == -1) break
            val readData = String(buf, 0, numRead)
            data.append(readData)
        }
        reader.close()
        return data.toString()
    }

    fun getMACAddress(): String? {
        var mac = ""
        try {
            mac = loadFileAsString("/sys/class/net/eth0/address").toUpperCase().substring(0, 17)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (mac=="") mac = getMACAddress2()
        if (mac=="") mac = "NO_MAC_ADDRESS"
        return mac
    }
    fun getMACAddress2(): String {
        val interfaceName = "wlan0"
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
//                if (interfaceName != null) {
                    if (!intf.getName().equals(interfaceName)) continue
//                }
                val mac = intf.getHardwareAddress() ?: return ""
                val buf = StringBuilder()
                for (idx in mac.indices)
                    buf.append(String.format("%02X:", mac[idx]))
                if (buf.length > 0) buf.deleteCharAt(buf.length - 1)
                return buf.toString()
            }
        } catch (ex: Exception) {
            Log.e("Error", ex.toString())
        }
        return ""
    }

    fun getLocalIpAddress(): String {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val interf = en.nextElement()
                val ips = interf.inetAddresses
                while (ips.hasMoreElements()) {
                    val inetAddress = ips.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress.toString()
                    }
                }
            }
        } catch (ex: SocketException) {
            Log.e("Error", ex.toString())
        }
        return ""
    }
}
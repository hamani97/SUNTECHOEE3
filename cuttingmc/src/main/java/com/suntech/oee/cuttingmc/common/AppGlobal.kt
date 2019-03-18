package com.suntech.oee.cuttingmc.common

import android.content.Context
import android.util.Log
import com.suntech.oee.cuttingmc.util.UtilLocalStorage
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

    fun set_server_ip(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_server_ip", idx) }
    fun get_server_ip() : String { return UtilLocalStorage.getString(instance._context!!, "current_server_ip") }

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
package com.suntech.oee.cuttingmc.common

import android.content.Context
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import com.suntech.oee.cuttingmc.db.SimpleDatabaseHelper
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

/**
 * Created by theinvest on 2016-08-05.
 */
class AppGlobal private constructor() {

    private var _context : Context? = null

    private object Holder { val INSTANCE = AppGlobal() }

    companion object {
        val instance: AppGlobal by lazy { Holder.INSTANCE }
    }
    fun setContext(ctx : Context) {
        _context = ctx
    }

    // 디바이스 정보
    var deviceToken : String = ""
    var _server_state : Boolean = false

    // 작업 프로덕트 고유값 설정
    fun reset_product_idx() {
        UtilLocalStorage.setString(instance._context!!, "work_idx", "")
    }
    fun set_product_idx() {
        var product_idx = get_product_idx()
        if (product_idx == "" ) product_idx = "1000"
        val new_product_idx = product_idx.toInt() + 1
        UtilLocalStorage.setString(instance._context!!, "work_idx", new_product_idx.toString())
    }
    fun get_product_idx() : String {
        return UtilLocalStorage.getString(instance._context!!, "work_idx")
    }

    // 작업시간 설정
    fun set_current_work_day(data: String) { UtilLocalStorage.setString(instance._context!!, "set_current_work_time", data) }
    fun get_current_work_day() : String { return UtilLocalStorage.getString(instance._context!!, "set_current_work_time") }

    fun set_today_work_time(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "current_work_time", data) }
    fun get_today_work_time() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "current_work_time") }
    fun set_prev_work_time(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "current_prev_work_time", data) }
    fun get_prev_work_time() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "current_prev_work_time") }

    // 어제시간과 오늘시간 중에 지나지 않은 날짜를 선택해서 반환
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
    fun get_current_shift_idx() : String {
        var item: JSONObject = get_current_shift_time() ?: return ""
        return item["shift_idx"].toString()
    }
    fun get_current_shift_name() : String {
        var item: JSONObject = get_current_shift_time() ?: return ""
        return item["shift_name"].toString()
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
    fun get_current_shift_time() : JSONObject? {
        val list = get_current_work_time()
        if (list.length() == 0 ) return null
        val idx = get_current_shift_time_idx()
        if (idx < 0) return null
        return list.getJSONObject(idx)
    }

    // 현재 프로덕트의 누적 시간을 구함
    fun get_current_product_accumulated_time(with_no_constraint:Boolean = true) : Int {
        val product_idx = get_product_idx()

        var db = SimpleDatabaseHelper(_context!!)
        val row = db.get(product_idx)

        var product_stime = OEEUtil.parseDateTime(row!!["start_dt"].toString())
        var now = DateTime()

        if (with_no_constraint) {
            // 현재 작업이 처음 작업인 경우,
            // 쉬프트 시작시간으로 설정, 이후부터는 프로덕트 시작시간 기준으로 계산
            val list = db.gets()
            if (list?.size==1) {
                var item = get_current_shift_time()
                if (item!=null)
                    product_stime = OEEUtil.parseDateTime(item["work_stime"].toString())
            }
        }
        return compute_work_time(product_stime, now)
    }

    // 현재 쉬프트의 누적 시간을 구함
    fun get_current_shift_accumulated_time() : Int {
        var item = get_current_shift_time()
        if (item==null) return 0

        var shift_stime = OEEUtil.parseDateTime(item["work_stime"].toString())
        var shift_etime = OEEUtil.parseDateTime(item["work_etime"].toString())

        return compute_work_time(shift_stime, shift_etime, false)
    }

    // 현재 쉬프트의 총 시간을 구함 (쉬프트시작시간,종료시간무시)
    fun get_current_shift_total_time() : Int {
        var item = get_current_shift_time()
        if (item==null) return 0

        var shift_stime = OEEUtil.parseDateTime(item["work_stime"].toString())
        var shift_etime = OEEUtil.parseDateTime(item["work_etime"].toString())

        return compute_work_time(shift_stime, shift_etime, false, false)
    }

    // 두시간(기간)에서 겹치는 시간을 계산
    fun compute_time(src_dt_s:DateTime, src_dt_e:DateTime, dst_dt_s:DateTime, dst_dt_e:DateTime) : Int {
        var dst_dt_s_cpy = dst_dt_s
        var dst_dt_e_cpy = dst_dt_e
        if (src_dt_s.millis > dst_dt_s_cpy.millis ) dst_dt_s_cpy = src_dt_s
        if (src_dt_e.millis < dst_dt_e_cpy.millis ) dst_dt_e_cpy = src_dt_e

        var src_diff = ( src_dt_e.millis - src_dt_s.millis)
        var dst_diff = ( dst_dt_e_cpy.millis - dst_dt_s_cpy.millis)

        if (dst_dt_s_cpy.millis >= src_dt_s.millis && dst_dt_s_cpy.millis <= src_dt_e.millis &&
                dst_dt_e_cpy.millis >= src_dt_s.millis && dst_dt_e_cpy.millis <= src_dt_e.millis) {
            return (dst_diff / 1000 ).toInt()
        }
        return 0
    }

    // 특정시간(기간)안에 휴식시간과 다운타임시간을 뺀 시간을 계산
    fun compute_work_time(stime:DateTime, etime:DateTime, is_downtime:Boolean = true, is_total:Boolean = true) : Int {
        var item = get_current_shift_time()
        if (item==null) return 0

        var shift_etime = etime
        var shift_stime = stime

        // 작업시간내에서만 계산
        var shift_stime_src = OEEUtil.parseDateTime(item["work_stime"].toString())
        var shift_etime_src = OEEUtil.parseDateTime(item["work_etime"].toString())

        if (shift_stime.millis < shift_stime_src.millis ) shift_stime = shift_stime_src
        if (shift_etime.millis < shift_stime_src.millis ) return 0
        if (shift_stime.millis > shift_etime_src.millis ) return 0

        if (is_total) {
            var now = DateTime()
            if (now.millis < shift_etime.millis ) shift_etime = now // 작업종료시간보다 넘은경우, 현재시간은 종료시간으로 고정
            if (now.millis < shift_stime.millis ) shift_stime = now  // 작업시작시간보다 빠른경우, 현재시간은 시작시간으로 고정
        }

        var dif = (shift_etime.millis - shift_stime.millis)/1000

        //Log.e("test", "shift_stime = "+ shift_stime.toString())
        //Log.e("test", "shift_etime = "+ shift_etime.toString())

        // 휴식 시간 계산

        var planned1_stime = OEEUtil.parseDateTime(item["planned1_stime_dt"].toString())
        var planned1_etime = OEEUtil.parseDateTime(item["planned1_etime_dt"].toString())
        var planned2_stime = OEEUtil.parseDateTime(item["planned2_stime_dt"].toString())
        var planned2_etime = OEEUtil.parseDateTime(item["planned2_etime_dt"].toString())

        val d1 = compute_time(shift_stime, shift_etime, planned1_stime, planned1_etime)
        val d2 = compute_time(shift_stime, shift_etime, planned2_stime, planned2_etime)

        dif = dif - d1 - d2
        //Log.e("test", "dif1 = "+ dif.toString())

        if (is_downtime==false) return dif.toInt()
/*
        // 다운타임 시간 계산
        var db = DBHelperForDownTime(_context!!)
        var list = db.gets() ?: null

        list?.forEach { item ->
            var end_dt = item["end_dt"].toString()
            if (end_dt=="null") end_dt = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")

            val downtime_s = OEEUtil.parseDateTime(item["start_dt"].toString())
            val downtime_e = OEEUtil.parseDateTime(end_dt)
            val df = compute_time(shift_stime, shift_etime, downtime_s, downtime_e)
            dif -= df

            // 휴식시간과 다운타임시간 중복 계산
            if (planned1_stime.millis < shift_stime.millis ) planned1_stime = shift_stime
            if (planned1_etime.millis > shift_etime.millis ) planned1_etime = shift_etime

            if (planned1_etime.millis-planned1_stime.millis >0) {
                val p1_dw = compute_time(planned1_stime, planned1_etime, downtime_s, downtime_e)
                dif += p1_dw
            }

            if (planned2_stime.millis < shift_stime.millis ) planned2_stime = shift_stime
            if (planned2_etime.millis > shift_etime.millis ) planned2_etime = shift_etime
            if (planned2_etime.millis-planned2_stime.millis >0) {
                val p2_dw = compute_time(planned2_stime, planned2_etime, downtime_s, downtime_e)
                dif += p2_dw
            }
        }
        //Log.e("test", "dif2 = "+ dif.toString())
*/
        return dif.toInt()
    }

    // 작업자 정보 설정
    fun set_worker_no(name: String) { UtilLocalStorage.setString(instance._context!!, "current_worker_no", name) }
    fun get_worker_no() : String { return UtilLocalStorage.getString(instance._context!!, "current_worker_no") }
    fun set_worker_name(name: String) { UtilLocalStorage.setString(instance._context!!, "current_worker_name", name) }
    fun get_worker_name() : String { return UtilLocalStorage.getString(instance._context!!, "current_worker_name") }

    fun get_last_workers() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "last_workers") }
    fun push_last_worker(no: String, name: String) {
        remove_last_worker(no)

        var list = AppGlobal.instance.get_last_workers()
        var json = JSONObject()
        json.put("number", no)
        json.put("name", name)
        list.put(json)

        if (list.length() > 4) list.remove(0)

        UtilLocalStorage.setJSONArray(instance._context!!, "last_workers", list)
    }
    fun remove_last_worker(no:String) {
        var list = AppGlobal.instance.get_last_workers()

        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            val item_no = item.getString("number")
            if (item_no==no) {
                list.remove(i)
                break
            }
        }
        UtilLocalStorage.setJSONArray(instance._context!!, "last_workers", list)
    }

    // 디자인 정보 설정
    fun set_design_info(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "design_info", data) }
    fun get_design_info() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "design_info") }
    fun set_design_info_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_design_info_idx", idx) }
    fun get_design_info_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_design_info_idx") }

    fun set_model(data: String) { UtilLocalStorage.setString(instance._context!!, "current_model", data) }
    fun get_model() : String { return UtilLocalStorage.getString(instance._context!!, "current_model") }
    fun set_article(data: String) { UtilLocalStorage.setString(instance._context!!, "current_article", data) }
    fun get_article() : String { return UtilLocalStorage.getString(instance._context!!, "current_article") }
    fun set_material_way(data: String) { UtilLocalStorage.setString(instance._context!!, "current_material_way", data) }
    fun get_material_way() : String { return UtilLocalStorage.getString(instance._context!!, "current_material_way") }
    fun set_component(data: String) { UtilLocalStorage.setString(instance._context!!, "current_component", data) }
    fun get_component() : String { return UtilLocalStorage.getString(instance._context!!, "current_component") }

    fun set_cycle_time(idx: Int) { UtilLocalStorage.setInt(instance._context!!, "current_cycle_time", idx) }
    fun get_cycle_time() : Int { return UtilLocalStorage.getInt(instance._context!!, "current_cycle_time") }

    fun set_pieces_info(idx: Int) {
        UtilLocalStorage.setInt(instance._context!!, "current_pieces_info", idx)
    }
    fun get_pieces_info() : Int {
        var r = UtilLocalStorage.getInt(instance._context!!, "current_pieces_info")
        if (r ==0) r=1
        return r
    }

    // 작업 정보
    fun set_accumulated_count(idx: Int) { UtilLocalStorage.setInt(instance._context!!, "accumulated_count", idx) }
    fun get_accumulated_count() : Int { return UtilLocalStorage.getInt(instance._context!!, "accumulated_count") }

    fun set_total_target(data: Int) { UtilLocalStorage.setInt(instance._context!!, "total_target", data) }
    fun get_total_target() : Int { return UtilLocalStorage.getInt(instance._context!!, "total_target") }

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
    fun set_mc_no2(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_mc_no2", idx) }
    fun get_mc_no2() : String { return UtilLocalStorage.getString(instance._context!!, "current_mc_no2") }

    fun set_mc_serial(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_mc_serial", idx) }
    fun get_mc_serial() : String { return UtilLocalStorage.getString(instance._context!!, "current_mc_serial") }

    fun set_server_ip(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_server_ip", idx) }
    fun get_server_ip() : String { return UtilLocalStorage.getString(instance._context!!, "current_server_ip") }
    fun set_server_port(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_server_port", idx) }
    fun get_server_port() : String { return UtilLocalStorage.getString(instance._context!!, "current_server_port") }

    fun set_long_touch(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "current_long_touch", state) }
    fun get_long_touch() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "current_long_touch") }

    fun set_layer_pairs(layer_no: String, pair: String) { UtilLocalStorage.setString(instance._context!!, "current_layer_" + layer_no, pair) }
    fun get_layer_pairs(layer_no: String) : String { return UtilLocalStorage.getString(instance._context!!, "current_layer_" + layer_no) }


    // Component 필터 세팅값
    fun set_compo_wos_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_wos_idx", idx) }
    fun get_compo_wos_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_wos_idx") }
    fun set_compo_wos(value: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_wos", value) }
    fun get_compo_wos() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_wos") }

    fun set_compo_model(value: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_model", value) }
    fun get_compo_model() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_model") }
    fun set_compo_style(value: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_style", value) }
    fun get_compo_style() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_style") }

    fun set_compo_component_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_component_idx", idx) }
    fun get_compo_component_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_component_idx") }
    fun set_compo_component(value: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_component", value) }
    fun get_compo_component() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_component") }
//
    fun set_compo_size_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_size_idx", idx) }
    fun get_compo_size_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_size_idx") }
    fun set_compo_size(value: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_size", value) }
    fun get_compo_size() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_size") }
    fun set_compo_target(value: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_target", value) }
    fun get_compo_target() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_target") }

    fun set_compo_layer(value: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_layer", value) }
    fun get_compo_layer() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_layer") }


    fun set_target_setting(value: Int) { UtilLocalStorage.setInt(instance._context!!, "current_target_setting", value) }
    fun get_target_setting() : Int { return UtilLocalStorage.getInt(instance._context!!, "current_target_setting") }

    fun set_target_shift(shift_no: String, value: String) { UtilLocalStorage.setString(instance._context!!, "current_target_shift_" + shift_no, value) }
    fun get_target_shift(shift_no: String) : String { return UtilLocalStorage.getString(instance._context!!, "current_target_shift_" + shift_no) }

    // 다운 타임
    fun set_downtime_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_downtime_idx", idx) }
    fun get_downtime_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_downtime_idx") }
    fun set_downtime_sec(value: String) { UtilLocalStorage.setString(instance._context!!, "current_downtime_sec", value) }
    fun get_downtime_sec() : String { return UtilLocalStorage.getString(instance._context!!, "current_downtime_sec") }
    fun set_downtime_list(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "downtime_list", data) }
    fun get_downtime_list() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "downtime_list") }

    // 기타
    fun set_color_code(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "color_code", data) }
    fun get_color_code() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "color_code") }

    fun set_comopnent_data(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "comopnent_data", data) }
    fun get_comopnent_data() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "comopnent_data") }

    fun set_availability(value: String) { UtilLocalStorage.setString(instance._context!!, "current_availability", value) }
    fun get_availability() : String { return UtilLocalStorage.getString(instance._context!!, "current_availability") }
    fun set_performance(value: String) { UtilLocalStorage.setString(instance._context!!, "current_performance", value) }
    fun get_performance() : String { return UtilLocalStorage.getString(instance._context!!, "current_performance") }
    fun set_quality(value: String) { UtilLocalStorage.setString(instance._context!!, "current_quality", value) }
    fun get_quality() : String { return UtilLocalStorage.getString(instance._context!!, "current_quality") }

    fun set_target_type(value: String) { UtilLocalStorage.setString(instance._context!!, "target_type", value) }
    fun get_target_type() : String { return UtilLocalStorage.getString(instance._context!!, "target_type") }

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
        if (mac == "") mac = getMACAddress2()
//        var mac = getMACAddress2()
        if (mac == "") mac = "NO_MAC_ADDRESS"
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

    // Network & Wifi check
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

    fun playSound(context: Context) {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(context, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isOnline(context: Context) : Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }
}
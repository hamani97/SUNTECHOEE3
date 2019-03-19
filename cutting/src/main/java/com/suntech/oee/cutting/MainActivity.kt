package com.suntech.oee.cutting

import android.content.*
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import android.widget.Toast
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.suntech.oee.cutting.base.BaseActivity
import com.suntech.oee.cutting.base.BaseFragment
import com.suntech.oee.cutting.common.AppGlobal
import com.suntech.oee.cutting.db.DBHelperForCount
import com.suntech.oee.cutting.db.DBHelperForDownTime
import com.suntech.oee.cutting.db.SimpleDatabaseHelper
import com.suntech.oee.cutting.popup.*
import com.suntech.oee.cutting.service.UsbService
import com.suntech.oee.cutting.util.OEEUtil
import com.suntech.oee.cutting.util.UtilLocalStorage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_top_menu.*
import org.joda.time.DateTime
import org.joda.time.Days
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.ceil

class MainActivity : BaseActivity() {

    var _stitch_db = DBHelperForCount(this)

    private var _doubleBackToExitPressedOnce = false
    private var _last_count_received_time = DateTime()

    var _is_call = false

    private val _broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)){
                    btn_wifi_state.isSelected = true
                } else {
                    btn_wifi_state.isSelected = false
                }
            }
            if (action.equals(Constants.BR_ADD_COUNT)) {
                handleData("{\"cmd\" : \"count\", \"value\" : 1}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppGlobal.instance.setContext(this)

        initView()
        start_timer()
//        fetchRequiredData()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    public override fun onResume() {
        super.onResume()
        setFilters()  // Start listening notifications from UsbService
        startService(UsbService::class.java, usbConnection, null) // Start UsbService(if it was not started before) and Bind it
        registerReceiver(_broadcastReceiver, IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
        registerReceiver(_broadcastReceiver, IntentFilter(Constants.BR_ADD_COUNT))

        updateView()
//        fetchRequiredData()
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(mUsbReceiver)
        unbindService(usbConnection)
        unregisterReceiver(_broadcastReceiver)
    }

    override fun onBackPressed() {
        if (vp_fragments.currentItem != 0) {
            changeFragment(0)
            return
        }
        if (_doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this._doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        Handler().postDelayed({ _doubleBackToExitPressedOnce = false }, 2000)
    }

    private fun initView() {
        mHandler = MyHandler(this)

        if (AppGlobal.instance.get_long_touch()) {
            iv_logo.setOnLongClickListener { changeFragment(0); true }
            btn_product_list.setOnLongClickListener { startActivity(Intent(this, ProductListActivity::class.java));true }
            btn_downtime.setOnLongClickListener { startDowntimeActivity();true }
            btn_defective_info.setOnLongClickListener { startActivity(Intent(this, DefectiveActivity::class.java));true }
            btn_actual_count_edit.setOnLongClickListener { startActivity(Intent(this, ActualCountEditActivity::class.java));true }
            btn_worksheet.setOnLongClickListener { startActivity(Intent(this, WorkSheetActivity::class.java));true }
            btn_push_to_app.setOnLongClickListener { startActivity(Intent(this, PushActivity::class.java));true }
        } else {
            iv_logo.setOnClickListener { changeFragment(0) }
            btn_product_list.setOnClickListener { startActivity(Intent(this, ProductListActivity::class.java)) }
            btn_downtime.setOnClickListener { startDowntimeActivity() }
            btn_defective_info.setOnClickListener { startActivity(Intent(this, DefectiveActivity::class.java)) }
            btn_actual_count_edit.setOnClickListener { startActivity(Intent(this, ActualCountEditActivity::class.java)) }
            btn_worksheet.setOnClickListener { startActivity(Intent(this, WorkSheetActivity::class.java)) }
            btn_push_to_app.setOnClickListener { startActivity(Intent(this, PushActivity::class.java)) }
        }

        val adapter = TabAdapter(supportFragmentManager)
        adapter.addFragment(HomeFragment(), "")
        adapter.addFragment(CountViewFragment(), "")
        vp_fragments.adapter = adapter
        adapter.notifyDataSetChanged()
        vp_fragments.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(state: Int) {
                (adapter.getItem(state) as BaseFragment).onSelected()
            }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageScrollStateChanged(position: Int) {}
        })
    }

    fun changeFragment(pos:Int) {
        vp_fragments.setCurrentItem(pos, true)
    }

    private fun updateView() {
        if (AppGlobal.instance.isOnline(this)) btn_wifi_state.isSelected = true
        else btn_wifi_state.isSelected = false
    }

    private fun sendPing() {
        btn_server_state.isSelected = false
        AppGlobal.instance._server_state = false
        if (AppGlobal.instance.get_server_ip()=="") return

        val br_intent = Intent("need.refresh.server.state")
        br_intent.putExtra("state", "N")
        this.sendBroadcast(br_intent)

        val currentTimeMillisStart = System.currentTimeMillis()
        val uri = "/ping.php"

        request(this, uri, false, false, false,null, { result ->

            val currentTimeMillisEnd = System.currentTimeMillis()
            val millis = currentTimeMillisEnd - currentTimeMillisStart

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){
                btn_server_state.isSelected = true
                AppGlobal.instance._server_state = true
                tv_ms.text = "" + millis + " ms"

                val br_intent = Intent("need.refresh.server.state")
                br_intent.putExtra("state", "Y")
                this.sendBroadcast(br_intent)
            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendStartDownTime(dt:DateTime) {
        if (AppGlobal.instance.get_server_ip()=="") return

        val work_idx = ""+AppGlobal.instance.get_product_idx()
        if (work_idx=="") return

        if (_is_call) return
        _is_call = true
/*
        var db = SimpleDatabaseHelper(this)
        val row = db.get(work_idx)
        val seq = row!!["seq"].toString().toInt()
*/
        var down_db = DBHelperForDownTime(this)
        val count = down_db.counts_for_notcompleted()
        if (count>0) return

        val list = down_db.gets()

        val uri = "/downtimedata.php"
        var params = listOf("code" to "start",
                "mac_addr" to AppGlobal.instance.getMACAddress(),
                "didx" to AppGlobal.instance.get_design_info_idx(),
                "sdate" to dt.toString("yyyy-MM-dd"),
                "stime" to dt.toString("HH:mm:ss"),
                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                "factory_idx" to AppGlobal.instance.get_room_idx(),
                "line_idx" to AppGlobal.instance.get_line_idx(),
                "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
                "seq" to (list?.size ?: 0) + 1)

        request(this, uri, true,false, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){
                var idx = result.getString("idx")
                AppGlobal.instance.set_downtime_idx(idx)

                val didx = AppGlobal.instance.get_design_info_idx()
                val work_info =AppGlobal.instance.get_current_shift_time()
                val shift_idx = work_info?.getString("shift_idx") ?: ""
                val shift_name = work_info?.getString("shift_name") ?: ""

                down_db.add(idx, work_idx, didx, shift_idx, shift_name, dt.toString("yyyy-MM-dd HH:mm:ss"))

            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
            _is_call = false
        },{
            _is_call = false
        })

    }

    private fun sendEndDownTimeForce() {
        if (AppGlobal.instance.get_server_ip()=="") return

        if (AppGlobal.instance.get_downtime_idx()=="") return

        val downtime = "99"
        val uri = "/downtimedata.php"
        var params = listOf("code" to "end",
                "idx" to AppGlobal.instance.get_downtime_idx(),
                "downtime" to downtime,
                "edate" to DateTime().toString("yyyy-MM-dd"),
                "etime" to DateTime().toString("HH:mm:ss"))

        request(this, uri, true,false, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00") {

                val idx = AppGlobal.instance.get_downtime_idx()
                AppGlobal.instance.set_downtime_idx("")
                var db = DBHelperForDownTime(this)
                db.updateEnd(idx, "ignored")

                // 기존 다운타임 화면이 열려있으면 닫고
                val br_intent = Intent("start.downtime")
                this.sendBroadcast(br_intent)

                // 카운트뷰로 이동
                if (vp_fragments.currentItem != 1) changeFragment(1)

            }else if(code == "99") {
                //?
            }else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendTarget(target:String) {
        if (AppGlobal.instance.get_server_ip()=="") return

        val work_idx = ""+AppGlobal.instance.get_product_idx()
        if (work_idx=="") return

        var db = SimpleDatabaseHelper(this)
        val row = db.get(work_idx)
        val seq = row!!["seq"].toString().toInt()

        val uri = "/targetdata.php"
        var params = listOf("mac_addr" to AppGlobal.instance.getMACAddress(),
                "didx" to AppGlobal.instance.get_design_info_idx(),
                "target" to target,
                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                "factory_idx" to AppGlobal.instance.get_room_idx(),
                "line_idx" to AppGlobal.instance.get_line_idx(),
                "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
                "seq" to seq)

        request(this, uri, true,false, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){

            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendCountData(count:String) {
        if (AppGlobal.instance.get_server_ip()=="") return

        val work_idx = ""+AppGlobal.instance.get_product_idx()
        if (work_idx=="") return

        var db = SimpleDatabaseHelper(this)
        val row = db.get(work_idx)
        val actual = row!!["actual"].toString().toInt()
        val seq = row!!["seq"].toString().toInt()

        val uri = "/senddata1.php"
        var params = listOf("mac_addr" to AppGlobal.instance.getMACAddress(),
                "didx" to AppGlobal.instance.get_design_info_idx(),
                "count" to count,
                "total_count" to actual,
                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                "factory_idx" to AppGlobal.instance.get_room_idx(),
                "line_idx" to AppGlobal.instance.get_line_idx(),
                "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
                "seq" to seq)

        request(this, uri, true,false, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){

            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchRequiredData() {
        fetchWorkData()
        fetchDesignData()
        fetchDownTimeType()
        fetchColorData()
        if (Constants.DEMO_VERSION) fetchComponentData()
    }

    private fun fetchWorkData() {
        if (AppGlobal.instance.get_server_ip()=="") return

        var dt = DateTime()
        val uri = "/getlist1.php"
        var params = listOf("code" to "work_time",
                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                "factory_idx" to AppGlobal.instance.get_room_idx(),
                "line_idx" to AppGlobal.instance.get_line_idx(),
                "date" to dt.toString("yyyy-MM-dd"))

        request(this, uri, false, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){

                var list = result.getJSONArray("item")
                list = handleWorkData(list)
                AppGlobal.instance.set_today_work_time(list)
            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })

        // 전날짜 데이터 가져오기
        var prev_params = listOf("code" to "work_time",
                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                "factory_idx" to AppGlobal.instance.get_room_idx(),
                "line_idx" to AppGlobal.instance.get_line_idx(),
                "date" to dt.minusDays(1).toString("yyyy-MM-dd"))

        request(this, uri, false, prev_params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){

                var list = result.getJSONArray("item")
                list = handleWorkData(list)
                AppGlobal.instance.set_prev_work_time(list)
            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchDesignData() {
        if (AppGlobal.instance.get_server_ip()=="") return

        val uri = "/getlist1.php"
        var params = listOf("code" to "design",
                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                "factory_idx" to AppGlobal.instance.get_room_idx())

        request(this, uri, false, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){
                var list = result.getJSONArray("item")
                AppGlobal.instance.set_design_info(list)
            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchDownTimeType() {
        if (AppGlobal.instance.get_server_ip()=="") return

        val uri = "/getlist1.php"
        var params = listOf("code" to "check_time")

        request(this, uri, false, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){
                var value = result.getString("value")
                AppGlobal.instance.set_downtime_sec(value)
                val s = value.toInt()
                if (s>0) {
                }

            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchColorData() {

        val uri = "/getlist1.php"
        var params = listOf("code" to "color")

        request(this, uri, false, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){

                var list = result.getJSONArray("item")
                AppGlobal.instance.set_color_code(list)
            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchOEEGraph() {

        val uri = "/getoee.php"
        var params = listOf("mac_addr" to AppGlobal.instance.getMACAddress(),
                "shift_idx" to AppGlobal.instance.get_current_shift_idx(),
                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                "factory_idx" to AppGlobal.instance.get_room_idx(),
                "line_idx" to AppGlobal.instance.get_line_idx())

        request(this, uri, false, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){
                var availability = result.getString("availability")
                var performance = result.getString("performance")
                var quality = result.getString("quality")

                AppGlobal.instance.set_availability(availability)
                AppGlobal.instance.set_performance(performance)
                AppGlobal.instance.set_quality(quality)
            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchComponentData() {

        val uri = "/getlist1.php"
        var params = listOf("code" to "component",
                "mac_addr" to AppGlobal.instance.getMACAddress(),
                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                "factory_idx" to AppGlobal.instance.get_room_idx(),
                "line_idx" to AppGlobal.instance.get_line_idx())

        request(this, uri, false, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){

                var list = result.getJSONArray("item")
                AppGlobal.instance.set_comopnent_data(list)

                var notified_component_set = UtilLocalStorage.getStringSet(this, "notified_component_set")

                var is_popup = false
                for (i in 0..(list.length() - 1)) {

                    val item = list.getJSONObject(i)
                    val idx = item.getString("idx").toString()
                    val total_cycle_time = item.getString("total_cycle_time").toInt()
                    val now_cycle_time = item.getString("now_cycle_time").toInt()
                    val rt = total_cycle_time - now_cycle_time
                    if (rt <= 10 && !notified_component_set.contains(idx)) {
                        notified_component_set = notified_component_set.plus(idx)
                        is_popup = true
                    }
                }
                if (is_popup) {
                    UtilLocalStorage.setStringSet(this, "notified_component_set", notified_component_set)
                    startComponentActivity()
                }
            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleWorkData(list:JSONArray) :JSONArray {
        var shift_stime = DateTime()
        for (i in 0..(list.length() - 1)) {

            var item = list.getJSONObject(i)
            val over_time = item["over_time"]
            val date = item["date"].toString()
            if (i==0) { // 첫시간 기준
                shift_stime = OEEUtil.parseDateTime(date +" " + item["available_stime"] + ":00")
            }

            var work_stime = OEEUtil.parseDateTime(date +" " + item["available_stime"] + ":00")
            var work_etime = OEEUtil.parseDateTime(date +" " + item["available_etime"] + ":00")
            work_etime = work_etime.plusHours(over_time.toString().toInt())

            val planned1_stime_txt = date +" " + if (item["planned1_stime"] =="") "00:00:00" else item["planned1_stime"].toString() + ":00"
            val planned1_etime_txt = date +" " + if (item["planned1_etime"] =="") "00:00:00" else item["planned1_etime"].toString() + ":00"
            val planned2_stime_txt = date +" " + if (item["planned2_stime"] =="") "00:00:00" else item["planned2_stime"].toString() + ":00"
            val planned2_etime_txt = date +" " + if (item["planned2_etime"] =="") "00:00:00" else item["planned2_etime"].toString() + ":00"

            var planned1_stime_dt = OEEUtil.parseDateTime(planned1_stime_txt)
            var planned1_etime_dt = OEEUtil.parseDateTime(planned1_etime_txt)
            var planned2_stime_dt = OEEUtil.parseDateTime(planned2_stime_txt)
            var planned2_etime_dt = OEEUtil.parseDateTime(planned2_etime_txt)

            if (shift_stime.secondOfDay > work_stime.secondOfDay) work_stime = work_stime.plusDays(1)
            if (shift_stime.secondOfDay > work_etime.secondOfDay) work_etime = work_etime.plusDays(1)
            if (shift_stime.secondOfDay > planned1_stime_dt.secondOfDay) planned1_stime_dt = planned1_stime_dt.plusDays(1)
            if (shift_stime.secondOfDay > planned1_etime_dt.secondOfDay) planned1_etime_dt = planned1_etime_dt.plusDays(1)
            if (shift_stime.secondOfDay > planned2_stime_dt.secondOfDay) planned2_stime_dt = planned2_stime_dt.plusDays(1)
            if (shift_stime.secondOfDay > planned2_etime_dt.secondOfDay) planned2_etime_dt = planned2_etime_dt.plusDays(1)

            item.put("work_stime", work_stime.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("work_etime", work_etime.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned1_stime_dt", planned1_stime_dt.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned1_etime_dt", planned1_etime_dt.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned2_stime_dt", planned2_stime_dt.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned2_etime_dt", planned2_etime_dt.toString("yyyy-MM-dd HH:mm:ss"))
        }
        return list
    }

    ////////// USB
    private val mUsbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbService.ACTION_USB_PERMISSION_GRANTED // USB PERMISSION GRANTED
                -> Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show()
                UsbService.ACTION_USB_PERMISSION_NOT_GRANTED // USB PERMISSION NOT GRANTED
                -> Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show()
                UsbService.ACTION_NO_USB // NO USB CONNECTED
                -> Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show()
                UsbService.ACTION_USB_DISCONNECTED // USB DISCONNECTED
                -> Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show()
                UsbService.ACTION_USB_NOT_SUPPORTED // USB NOT SUPPORTED
                -> Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private var usbService: UsbService? = null
    private var mHandler: MyHandler? = null
    private val usbConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            usbService = (arg1 as UsbService.UsbBinder).service
            usbService!!.setHandler(mHandler)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            usbService = null
        }
    }

    private fun startService(service: Class<*>, serviceConnection: ServiceConnection, extras: Bundle?) {
        if (!UsbService.SERVICE_CONNECTED) {
            val startService = Intent(this, service)
            if (extras != null && !extras.isEmpty) {
                val keys = extras.keySet()
                for (key in keys) {
                    val extra = extras.getString(key)
                    startService.putExtra(key, extra)
                }
            }
            startService(startService)
        }
        val bindingIntent = Intent(this, service)
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setFilters() {
        val filter = IntentFilter()
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED)
        filter.addAction(UsbService.ACTION_NO_USB)
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED)
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED)
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED)
        registerReceiver(mUsbReceiver, filter)
    }

    private class MyHandler(activity: MainActivity) : Handler() {
        private val mActivity: WeakReference<MainActivity>

        init {
            mActivity = WeakReference(activity)
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                UsbService.MESSAGE_FROM_SERIAL_PORT -> {
                    val data = msg.obj as String
                    mActivity.get()?.handleData(data)
                }
                UsbService.CTS_CHANGE -> Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show()
                UsbService.DSR_CHANGE -> Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show()
            }
        }
    }

    private var recvBuffer = ""
    fun handleData (data:String) {
        if (data.indexOf("{") >= 0)  recvBuffer = ""

        recvBuffer += data

        val pos_end = recvBuffer.indexOf("}")
        if (pos_end < 0) return

        if (isJSONValid(recvBuffer)) {

            val parser = JsonParser()
            val element = parser.parse(recvBuffer)
            val cmd = element.asJsonObject.get("cmd").asString
            val value = element.asJsonObject.get("value")

            Toast.makeText(this, element.toString(), Toast.LENGTH_SHORT).show()

            Log.w("test", "usb = " + recvBuffer)

            saveRowData(cmd, value)
        } else {
            Log.e("test", "usb parsing error! = " + recvBuffer)
        }
    }

    private fun isJSONValid(test: String): Boolean {
        try {
            JSONObject(test)
        } catch (ex: JSONException) {
            try {
                JSONArray(test)
            } catch (ex1: JSONException) {
                return false
            }
        }
        return true
    }

    private fun saveRowData(cmd:String, value: JsonElement) {
        var db = SimpleDatabaseHelper(this)

        if (cmd=="barcode") {
            val arr = value.asJsonArray
            val didx = arr[0].asString
            var number = -1
            if (arr.size()>1) {
                val value2 = value.asJsonArray[1].asString
                number = value2.replace("[^0-9]", "").toInt()
            }

            var list = AppGlobal.instance.get_design_info()
            for (i in 0..(list.length() - 1)) {

                val item = list.getJSONObject(i)
                val idx = item.getString("idx")
                if (idx==didx) {
                    val cycle_time = item.getString("ct").toInt()
                    val model = item.getString("model").toString()
                    val article = item.getString("article").toString()
                    val material_way = item.getString("material_way").toString()
                    val component = item.getString("component").toString()

                    startNewProduct(didx, number, cycle_time, model, article, material_way, component)
                    return
                }
            }

            Toast.makeText(this, getString(R.string.msg_no_design), Toast.LENGTH_SHORT).show()

        } else if (cmd=="count") {
            val idx = AppGlobal.instance.get_design_info_idx()
            if (idx=="") return

            val work_idx = ""+AppGlobal.instance.get_product_idx()
            if (work_idx=="") return

            // 다운타임이 있으면 완료로 처리
            val downtime_idx = AppGlobal.instance.get_downtime_idx()
            Log.e("test", "downtime_idx = " + downtime_idx )
            if (downtime_idx!="") sendEndDownTimeForce()

            val row = db.get(work_idx)
            val pieces_info = AppGlobal.instance.get_pieces_info()

            val accumulated_count = AppGlobal.instance.get_accumulated_count() + 1
            if (pieces_info>accumulated_count) {
                AppGlobal.instance.set_accumulated_count(accumulated_count)
                return
            }
            AppGlobal.instance.set_accumulated_count(0)

            val actual = (row!!["actual"].toString().toInt() + 1)
            val defective = row!!["defective"].toString().toInt()
            db.update(work_idx, pieces_info, actual, defective)

            AppGlobal.instance.playSound(this)

            _last_count_received_time = DateTime()

            sendCountData(value.toString())

            _stitch_db.add(work_idx, value.toString())
        }
    }

    fun startNewProduct(didx:String, piece_info:Int, cycle_time:Int, model:String, article:String, material_way:String, component:String) {
        var db = SimpleDatabaseHelper(this)

        // 전의 작업과 동일한 디자인 번호이면 새작업이 아님
        val prev_didx = AppGlobal.instance.get_design_info_idx()
        if (didx==prev_didx) { return }

        // 전에 완료되지 않은 작업이 있다면 완료처리
        var prev_work_idx = ""+AppGlobal.instance.get_product_idx()
        if (prev_work_idx!="") db.updateWorkEnd(prev_work_idx)

        AppGlobal.instance.set_design_info_idx(didx)
        AppGlobal.instance.set_model(model)
        AppGlobal.instance.set_article(article)
        AppGlobal.instance.set_material_way(material_way)
        AppGlobal.instance.set_component(component)
        AppGlobal.instance.set_cycle_time(cycle_time)

        if (piece_info>0) AppGlobal.instance.set_pieces_info(piece_info)
        AppGlobal.instance.set_product_idx()

        //val seq = db.counts_for_didx(didx) + 1

        val s = db.gets()
        val seq = (s?.size ?: 0) + 1
        Log.e("test", "seq = "+ seq)

        val work_idx = ""+AppGlobal.instance.get_product_idx()
        val work_info =AppGlobal.instance.get_current_shift_time()
        val shift_idx = work_info?.getString("shift_idx") ?: ""
        val shift_name = work_info?.getString("shift_name") ?: ""
        db.add(work_idx, didx, shift_idx, shift_name, cycle_time, piece_info, 0, 0, 0, seq)

        val br_intent = Intent("need.refresh")
        this.sendBroadcast(br_intent)

        // 작업시작할때 현재 쉬프트의 날짜를 기록해놓음
        val current = AppGlobal.instance.get_current_work_time()
        val shift = current.getJSONObject(0)
        var shift_stime = OEEUtil.parseDateTime(shift["work_stime"].toString())
        AppGlobal.instance.set_current_work_day(shift_stime.toString("yyyy-MM-dd"))

        // 현재 shift의 첫생산인데 지각인경우 downtime 처리
        val list = db.gets()
        if (list?.size == 1) {
            val item = AppGlobal.instance.get_current_shift_time()
            if (item==null) return

            var work_stime = OEEUtil.parseDateTime(item["work_stime"].toString())
            val now = DateTime()
            if (now.millis - work_stime.millis > Constants.DOWNTIME_FIRST) {

                sendStartDownTime(work_stime)
                startDowntimeActivity()
            }
        }
    }

    private fun updateCurrentWorkTarget() {

        val idx = AppGlobal.instance.get_design_info_idx()
        if (idx=="") return

        val work_idx = ""+AppGlobal.instance.get_product_idx()
        if (work_idx=="") return

        var db = SimpleDatabaseHelper(this)
        val row = db.get(work_idx)
        if (row==null) return

        val actual = row["actual"].toString().toInt()

        val elapsedTime = AppGlobal.instance.get_current_product_accumulated_time()
        val elapsedTime_no_constraint = AppGlobal.instance.get_current_product_accumulated_time(true)
        val cycle_time = AppGlobal.instance.get_cycle_time()
        var target = (ceil(elapsedTime.toFloat() / cycle_time.toFloat())).toInt()
        var target_no_contraint = (ceil(elapsedTime_no_constraint.toFloat() / cycle_time.toFloat())).toInt()

        //Log.e("test", "elapsedTime = " + elapsedTime)
        //Log.e("test", "cycle_time = " + cycle_time)
        //Log.e("test", "target = " + target)
        db.updateWorkTarget(work_idx, target, target_no_contraint)

        if (Constants.DEMO_VERSION) {
            val current_shift_time = AppGlobal.instance.get_current_shift_time()
            var work_stime = OEEUtil.parseDateTime(current_shift_time?.getString("work_stime"))
            var work_etime = OEEUtil.parseDateTime(current_shift_time?.getString("work_etime"))

            var start_dt = OEEUtil.parseDateTime(row["start_dt"].toString())
            var end_dt = work_etime
            val list = db.gets()

            if (list==null||list.size<=1) start_dt = work_stime
            val t = AppGlobal.instance.compute_work_time(start_dt, end_dt, false, false)

            target = ( t / cycle_time )
        }

        // actual 이 0이면 서버로 보내지 않음
        if (actual>0) sendTarget(target.toString())
    }

    fun endWork() {
        AppGlobal.instance.reset_product_idx()
        AppGlobal.instance.set_worker_no("")
        AppGlobal.instance.set_worker_name("")
        AppGlobal.instance.set_design_info_idx("")
        AppGlobal.instance.set_model("")
        AppGlobal.instance.set_article("")
        AppGlobal.instance.set_material_way("")
        AppGlobal.instance.set_component("")

        // 다운타임이 있으면 완료로 처리
        val downtime_idx = AppGlobal.instance.get_downtime_idx()
        if (downtime_idx!="") sendEndDownTimeForce()

        var db = SimpleDatabaseHelper(this)
        db.delete()

        var db2 = DBHelperForDownTime(this)
        db2.delete()

        var db3 = DBHelperForCount(this)
        db3.delete()
        Toast.makeText(this, getString(R.string.msg_exit_automatically), Toast.LENGTH_SHORT).show()
    }

    private fun checkExit() {
        val work_idx = ""+AppGlobal.instance.get_product_idx()
        if (work_idx=="") return

        val last_work_dt = OEEUtil.parseDate(AppGlobal.instance.get_current_work_day())

        val current = AppGlobal.instance.get_current_work_time()
        if (current.length()==0) return

        val shift = current.getJSONObject(0)
        var shift_stime = OEEUtil.parseDateTime(shift["work_stime"].toString())
        var d = Days.daysBetween(last_work_dt.toLocalDate(), shift_stime.toLocalDate()).getDays()

        if (d != 0) {
            endWork()
        }
    }

    private fun checkDownTime() {

        var db = DBHelperForDownTime(this)
        val count = db.counts_for_notcompleted()
        if (count>0) {
            _last_count_received_time = DateTime()
            return
        }

        val idx = AppGlobal.instance.get_design_info_idx()
        if (idx=="") return

        val work_idx = ""+AppGlobal.instance.get_product_idx()
        if (work_idx=="") return

        val now = DateTime()
        val downtime_time = AppGlobal.instance.get_downtime_sec()

        if (downtime_time =="") {
            Toast.makeText(this, getString(R.string.msg_no_downtime), Toast.LENGTH_SHORT).show()
            return
        }

        val item = AppGlobal.instance.get_current_shift_time()
        if (item==null) return

        var work_stime = OEEUtil.parseDateTime(item["work_stime"].toString())
        var work_etime = OEEUtil.parseDateTime(item["work_etime"].toString())
        var planned1_stime_dt = OEEUtil.parseDateTime(item["planned1_stime_dt"].toString())
        var planned1_etime_dt = OEEUtil.parseDateTime(item["planned1_etime_dt"].toString())
        var planned2_stime_dt = OEEUtil.parseDateTime(item["planned2_stime_dt"].toString())
        var planned2_etime_dt = OEEUtil.parseDateTime(item["planned2_etime_dt"].toString())

        val downtime_time_sec = downtime_time.toInt()

        if (work_stime.millis < now.millis && work_etime.millis > now.millis &&
                !(planned1_stime_dt.millis < now.millis && planned1_etime_dt.millis > now.millis )&&
                !(planned2_stime_dt.millis < now.millis && planned2_etime_dt.millis > now.millis )&&
                downtime_time_sec > 0 && now.millis - _last_count_received_time.millis > downtime_time_sec*1000) {

            sendStartDownTime(_last_count_received_time)
            startDowntimeActivity()
        }

        if (work_stime.millis > now.millis || work_etime.millis < now.millis ||
                (planned1_stime_dt.millis < now.millis && planned1_etime_dt.millis > now.millis ) ||
                (planned2_stime_dt.millis < now.millis && planned2_etime_dt.millis > now.millis )) {
            _last_count_received_time = DateTime()
        }
    }

    private fun startComponentActivity () {

        val br_intent = Intent("start.component")
        this.sendBroadcast(br_intent)

        startActivity(Intent(this, ComponentActivity::class.java))
    }
    private fun startDowntimeActivity () {

        val br_intent = Intent("start.downtime")
        this.sendBroadcast(br_intent)

        val intent = Intent(this, DownTimeActivity::class.java)
        startActivity(intent)
    }

    /////// 쓰레드
    private val _downtime_timer = Timer()
    private val _timer_task1 = Timer()
    private val _timer_task2 = Timer()
    private val _timer_task3 = Timer()

    private fun start_timer () {
        val downtime_task = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    checkDownTime()
                    checkExit()
                }
            }
        }
        _downtime_timer.schedule(downtime_task, 500, 1000)

        val task1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    sendPing()
                    updateCurrentWorkTarget()
                }
            }
        }
        _timer_task1.schedule(task1, 2000, 10000)

        val task2 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    fetchRequiredData()
                }
            }
        }
        _timer_task2.schedule(task2, 600000, 600000)

        val task3 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    fetchOEEGraph()
                }
            }
        }
        _timer_task3.schedule(task3, 3000, 30000)
    }
    private fun cancel_timer () {
        _downtime_timer.cancel()
        _timer_task1.cancel()
        _timer_task2.cancel()
        _timer_task3.cancel()
    }


    private class TabAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        override fun getCount(): Int { return mFragments.size }

        private val mFragments = ArrayList<Fragment>()
        private val mFragmentTitles = ArrayList<String>()

        fun addFragment(fragment: Fragment, title: String) {
            mFragments.add(fragment)
            mFragmentTitles.add(title)
        }
        override fun getItem(position: Int): Fragment {
            return mFragments.get(position)
        }
        override fun getItemPosition(`object`: Any?): Int {
            return PagerAdapter.POSITION_NONE
        }
        override fun getPageTitle(position: Int): CharSequence {
            return mFragmentTitles.get(position)
        }
    }

}

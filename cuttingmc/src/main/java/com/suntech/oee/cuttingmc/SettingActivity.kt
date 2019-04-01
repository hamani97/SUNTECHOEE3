package com.suntech.oee.cuttingmc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.Toast
import com.suntech.oee.cuttingmc.base.BaseActivity
import com.suntech.oee.cuttingmc.common.AppGlobal
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*
import java.util.*

class SettingActivity : BaseActivity() {

    private var tab_pos: Int = 1
    private var _selected_target_type: Int = 0  // Server : 11 = Accumulate, 12 = hourly, 13 = shift total
                                                // Manual : 21 = Accumulate, 22 = hourly, 23 = shift total

    private var _selected_factory_idx: String = ""
    private var _selected_room_idx: String = ""
    private var _selected_line_idx: String = ""
    private var _selected_mc_no_idx: String = ""
    private var _selected_mc_model_idx: String = ""

    private var _selected_layer_pair_1: String = ""
    private var _selected_layer_pair_2: String = ""
    private var _selected_layer_pair_4: String = ""
    private var _selected_layer_pair_6: String = ""
    private var _selected_layer_pair_8: String = ""
    private var _selected_layer_pair_10: String = ""

    val _broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.getAction()
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false))
                    btn_wifi_state.isSelected = true
                else
                    btn_wifi_state.isSelected = false

            } else if (action.equals("need.refresh.server.state")) {
                val state = intent.getStringExtra("state")
                if (state == "Y") {
                    btn_server_state.isSelected = true
                } else btn_server_state.isSelected = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        initView()
    }

    public override fun onResume() {
        super.onResume()
        registerReceiver(_broadcastReceiver, IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(_broadcastReceiver)
    }

    private fun initView() {
        tv_title.setText(R.string.label_setting)

        // set hidden value
        // system setting
        _selected_factory_idx = AppGlobal.instance.get_factory_idx()
        _selected_room_idx = AppGlobal.instance.get_room_idx()
        _selected_line_idx = AppGlobal.instance.get_line_idx()
        _selected_mc_no_idx = AppGlobal.instance.get_mc_no_idx()
        _selected_mc_model_idx = AppGlobal.instance.get_mc_model_idx()

        // count setting
        _selected_layer_pair_1 = AppGlobal.instance.get_layer_pairs("1")
        _selected_layer_pair_2 = AppGlobal.instance.get_layer_pairs("2")
        _selected_layer_pair_4 = AppGlobal.instance.get_layer_pairs("4")
        _selected_layer_pair_6 = AppGlobal.instance.get_layer_pairs("6")
        _selected_layer_pair_8 = AppGlobal.instance.get_layer_pairs("8")
        _selected_layer_pair_10 = AppGlobal.instance.get_layer_pairs("10")


        // set widget value
        // system setting
        tv_setting_wifi.text = AppGlobal.instance.getWiFiSSID(this)
        tv_setting_ip.text = AppGlobal.instance.getLocalIpAddress()
        tv_setting_mac.text = AppGlobal.instance.getMACAddress()
        tv_setting_factory.text = AppGlobal.instance.get_factory()
        tv_setting_room.text = AppGlobal.instance.get_room()
        tv_setting_line.text = AppGlobal.instance.get_line()
        tv_setting_mc_model.text = AppGlobal.instance.get_mc_model()
        tv_setting_mc_no1.setText(AppGlobal.instance.get_mc_no1())
        et_setting_mc_serial.setText(AppGlobal.instance.get_mc_serial())

        et_setting_server_ip.setText(AppGlobal.instance.get_server_ip())
        et_setting_port.setText(AppGlobal.instance.get_server_port())

        sw_long_touch.isChecked = AppGlobal.instance.get_long_touch()

        // count setting
        if (_selected_layer_pair_1 != "") tv_layer_1.text = _selected_layer_pair_1 + " pair"
        if (_selected_layer_pair_2 != "") tv_layer_2.text = _selected_layer_pair_2 + " pair"
        if (_selected_layer_pair_4 != "") tv_layer_4.text = _selected_layer_pair_4 + " pair"
        if (_selected_layer_pair_6 != "") tv_layer_6.text = _selected_layer_pair_6 + " pair"
        if (_selected_layer_pair_8 != "") tv_layer_8.text = _selected_layer_pair_8 + " pair"
        if (_selected_layer_pair_10 != "") tv_layer_10.text = _selected_layer_pair_10 + " pair"

        // target setting
        if (AppGlobal.instance.get_target_setting() == 0) targetTypeChange(11)
        else targetTypeChange(AppGlobal.instance.get_target_setting())

        tv_shift_1.setText(AppGlobal.instance.get_target_shift("1"))
        tv_shift_2.setText(AppGlobal.instance.get_target_shift("2"))
        tv_shift_3.setText(AppGlobal.instance.get_target_shift("3"))

        // Tab button click
        btn_setting_system.setOnClickListener { tabChange(1) }
        btn_setting_count.setOnClickListener { tabChange(2) }
        btn_setting_target.setOnClickListener { tabChange(3) }

        // System setting button listener
        tv_setting_factory.setOnClickListener { fetchDataForFactory() }
        tv_setting_room.setOnClickListener { fetchDataForRoom() }
        tv_setting_line.setOnClickListener { fetchDataForLine() }
        tv_setting_mc_model.setOnClickListener { fetchDataForMCModel() }

        // Count setting button listener
        tv_layer_1.setOnClickListener { fetchLayerPairs("1") }
        tv_layer_2.setOnClickListener { fetchLayerPairs("2") }
        tv_layer_4.setOnClickListener { fetchLayerPairs("4") }
        tv_layer_6.setOnClickListener { fetchLayerPairs("6") }
        tv_layer_8.setOnClickListener { fetchLayerPairs("8") }
        tv_layer_10.setOnClickListener { fetchLayerPairs("10") }

        // Target setting button listener
        btn_server_accumulate.setOnClickListener { targetTypeChange(11) }
        btn_server_hourly.setOnClickListener { targetTypeChange(12) }
        btn_server_shifttotal.setOnClickListener { targetTypeChange(13) }
        btn_manual_accumulate.setOnClickListener { targetTypeChange(21) }
        btn_manual_hourly.setOnClickListener { targetTypeChange(22) }
        btn_manual_shifttotal.setOnClickListener { targetTypeChange(23) }


        // check server button
        btn_setting_check_server.setOnClickListener {
            checkServer(true)
            var new_ip = et_setting_server_ip.text.toString()
            var old_ip = AppGlobal.instance.get_server_ip()
            if (!new_ip.equals(old_ip)) {
                tv_setting_factory.text = ""
                tv_setting_room.text = ""
                tv_setting_line.text = ""
                tv_setting_mc_model.text = ""
            }
        }

        // Save button click
        btn_setting_confirm.setOnClickListener {
            if (tv_setting_factory.text.toString() == "" || tv_setting_room.text.toString() == "" ||
                    tv_setting_line.text.toString() == "" || tv_setting_mac.text.toString() == "") {
                Toast.makeText(this, getString(R.string.msg_require_info), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveSettingData()
        }
        // Cancel button click
        btn_setting_cancel.setOnClickListener { finish() }

        if (AppGlobal.instance.isOnline(this)) btn_wifi_state.isSelected = true
        else btn_wifi_state.isSelected = false

        if (AppGlobal.instance._server_state) btn_server_state.isSelected = true
        else btn_server_state.isSelected = false

        // TODO: TEST
        if (et_setting_server_ip.text.toString() == "") et_setting_server_ip.setText("10.10.10.90")
        if (et_setting_port.text.toString() == "") et_setting_port.setText("80")
    }

    private fun fetchLayerPairs(tv_no: String) {
        var arr: ArrayList<String> = arrayListOf<String>()
        var lists : ArrayList<HashMap<String, String>> = arrayListOf()

        arr.add("0.5 pair")
        lists.add(hashMapOf("pair" to "0.5", "desc" to "0.5 pair"))
        for (i in 1..5) {
            var num = i.toString()
            arr.add(num + " pair")
            lists.add(hashMapOf("pair" to num, "desc" to num + " pair"))
        }

        val intent = Intent(this, PopupSelectList::class.java)
        intent.putStringArrayListExtra("list", arr)
        startActivity(intent, { r, c, m, d ->
            if (r) {
                when (tv_no) {
                    "1" -> {
                        tv_layer_1.text = lists[c]["desc"] ?: ""
                        _selected_layer_pair_1 = lists[c]["pair"] ?: ""
                    }
                    "2" -> {
                        tv_layer_2.text = lists[c]["desc"] ?: ""
                        _selected_layer_pair_2 = lists[c]["pair"] ?: ""
                    }
                    "4" -> {
                        tv_layer_4.text = lists[c]["desc"] ?: ""
                        _selected_layer_pair_4 = lists[c]["pair"] ?: ""
                    }
                    "6" -> {
                        tv_layer_6.text = lists[c]["desc"] ?: ""
                        _selected_layer_pair_6 = lists[c]["pair"] ?: ""
                    }
                    "8" -> {
                        tv_layer_8.text = lists[c]["desc"] ?: ""
                        _selected_layer_pair_8 = lists[c]["pair"] ?: ""
                    }
                    "10" -> {
                        tv_layer_10.text = lists[c]["desc"] ?: ""
                        _selected_layer_pair_10 = lists[c]["pair"] ?: ""
                    }
                }
            }
        })
    }

    private fun checkServer(show_toast:Boolean = false) {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/ping.php"
        var params = listOf("" to "")

        request(this, url, port, uri, false, false,false, params, { result ->
            var code = result.getString("code")
            if (show_toast) Toast.makeText(this, result.getString("msg"), Toast.LENGTH_SHORT).show()
            if (code == "00") {
                btn_server_state.isSelected = true
            } else {
                btn_server_state.isSelected = false
            }
        }, {
            btn_server_state.isSelected = false
            if (show_toast) Toast.makeText(this, getString(R.string.msg_connection_fail), Toast.LENGTH_SHORT).show()
        })
    }

    private fun saveSettingData() {
        // setting value
        AppGlobal.instance.set_factory_idx(_selected_factory_idx)
        AppGlobal.instance.set_room_idx(_selected_room_idx)
        AppGlobal.instance.set_line_idx(_selected_line_idx)
        AppGlobal.instance.set_mc_no_idx(_selected_mc_no_idx)
        AppGlobal.instance.set_mc_model_idx(_selected_mc_model_idx)

        AppGlobal.instance.set_factory(tv_setting_factory.text.toString())
        AppGlobal.instance.set_room(tv_setting_room.text.toString())
        AppGlobal.instance.set_line(tv_setting_line.text.toString())
        AppGlobal.instance.set_mc_model(tv_setting_mc_model.text.toString())
        AppGlobal.instance.set_mc_no1(tv_setting_mc_no1.text.toString())
        AppGlobal.instance.set_mc_serial(et_setting_mc_serial.text.toString())

        AppGlobal.instance.set_server_ip(et_setting_server_ip.text.toString())
        AppGlobal.instance.set_server_port(et_setting_port.text.toString())
        AppGlobal.instance.set_long_touch(sw_long_touch.isChecked)

        // count layer
        AppGlobal.instance.set_layer_pairs("1", _selected_layer_pair_1)
        AppGlobal.instance.set_layer_pairs("2", _selected_layer_pair_2)
        AppGlobal.instance.set_layer_pairs("4", _selected_layer_pair_4)
        AppGlobal.instance.set_layer_pairs("6", _selected_layer_pair_6)
        AppGlobal.instance.set_layer_pairs("8", _selected_layer_pair_8)
        AppGlobal.instance.set_layer_pairs("10", _selected_layer_pair_10)

        // target type
        AppGlobal.instance.set_target_setting(_selected_target_type)
        AppGlobal.instance.set_target_shift("1", tv_shift_1.text.toString())
        AppGlobal.instance.set_target_shift("2", tv_shift_2.text.toString())
        AppGlobal.instance.set_target_shift("3", tv_shift_3.text.toString())

        val uri = "/setting1.php"
        var params = listOf(
                "code" to "server",
                "factory_parent_idx" to _selected_factory_idx,
                "factory_idx" to _selected_room_idx,
                "line_idx" to _selected_line_idx,
                "shift_idx" to AppGlobal.instance.get_current_shift_idx(),
                "mac_addr" to tv_setting_mac.text,
                "machine_no" to tv_setting_mc_no1.text.toString(),
                "ip_addr" to tv_setting_ip.text,
                "mc_model" to tv_setting_mc_model.text,
                "mc_serial" to et_setting_mc_serial.text.toString()
        )
        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            Toast.makeText(this, result.getString("msg"), Toast.LENGTH_SHORT).show()
            if(code == "00") {
                finish()
            }
        })
    }

    private fun fetchDataForFactory() {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/getlist1.php"
        var params = listOf("code" to "factory_parent")

        request(this, url, port, uri, false, false,false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00"){
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map = hashMapOf(
                            "idx" to item.getString("idx"),
                            "name" to item.getString("name")
                    )
                    lists.add(map)
                    arr.add(item.getString("name"))
                }

                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_factory_idx = lists[c]["idx"] ?: ""
                        tv_setting_factory.text = lists[c]["name"] ?: ""
                        tv_setting_room.text = ""
                        tv_setting_line.text = ""
                    }
                })
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchDataForRoom() {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/getlist1.php"
        var params = listOf(
                "code" to "factory",
                "factory_parent_idx" to _selected_factory_idx)

        request(this, url, port, uri, false, false,false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists :ArrayList<HashMap<String, String>> = arrayListOf()

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map=hashMapOf(
                            "idx" to item.getString("idx"),
                            "name" to item.getString("name")
                    )
                    lists.add(map)
                    arr.add(item.getString("name"))
                }

                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_room_idx = lists[c]["idx"] ?: ""
                        tv_setting_room.text = lists[c]["name"] ?: ""
                        tv_setting_line.text = ""
                    }
                })
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchDataForLine() {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/getlist1.php"
        var params = listOf(
                "code" to "line",
                "factory_parent_idx" to _selected_factory_idx,
                "factory_idx" to _selected_room_idx)

        request(this, url, port, uri, false, false,false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists :ArrayList<HashMap<String, String>> = arrayListOf()

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map=hashMapOf(
                            "idx" to item.getString("idx"),
                            "name" to item.getString("name")
                    )
                    lists.add(map)
                    arr.add(item.getString("name"))
                }

                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_line_idx = lists[c]["idx"] ?: ""
                        tv_setting_line.text = lists[c]["name"] ?: ""
                    }
                })
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchDataForMCModel() {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/getlist1.php"
        var params = listOf("code" to "machine_model")

        request(this, url, port, uri, false, false,false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists :ArrayList<HashMap<String, String>> = arrayListOf()

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map=hashMapOf(
                            "idx" to item.getString("idx"),
                            "name" to item.getString("name")
                    )
                    lists.add(map)
                    arr.add(item.getString("name"))
                }

                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_mc_model_idx = lists[c]["idx"] ?: ""
                        tv_setting_mc_model.text = lists[c]["name"] ?: ""
                    }
                })
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun tabChange(v : Int) {
        if (tab_pos == v) return
        tab_pos = v
        when (tab_pos) {
            1 -> {
                btn_setting_system.setTextColor(ContextCompat.getColor(this, R.color.white))
                btn_setting_system.setBackgroundResource(R.color.tab_on_bg_color)
                btn_setting_count.setTextColor(ContextCompat.getColor(this, R.color.gray))
                btn_setting_count.setBackgroundResource(R.color.tab_off_bg_color)
                btn_setting_target.setTextColor(ContextCompat.getColor(this, R.color.gray))
                btn_setting_target.setBackgroundResource(R.color.tab_off_bg_color)
                layout_setting_system.visibility = View.VISIBLE
                layout_setting_count.visibility = View.GONE
                layout_setting_target.visibility = View.GONE
            }
            2 -> {
                btn_setting_system.setTextColor(ContextCompat.getColor(this, R.color.gray))
                btn_setting_system.setBackgroundResource(R.color.tab_off_bg_color)
                btn_setting_count.setTextColor(ContextCompat.getColor(this, R.color.white))
                btn_setting_count.setBackgroundResource(R.color.tab_on_bg_color)
                btn_setting_target.setTextColor(ContextCompat.getColor(this, R.color.gray))
                btn_setting_target.setBackgroundResource(R.color.tab_off_bg_color)
                layout_setting_system.visibility = View.GONE
                layout_setting_count.visibility = View.VISIBLE
                layout_setting_target.visibility = View.GONE
            }
            3 -> {
                btn_setting_system.setTextColor(ContextCompat.getColor(this, R.color.gray))
                btn_setting_system.setBackgroundResource(R.color.tab_off_bg_color)
                btn_setting_count.setTextColor(ContextCompat.getColor(this, R.color.gray))
                btn_setting_count.setBackgroundResource(R.color.tab_off_bg_color)
                btn_setting_target.setTextColor(ContextCompat.getColor(this, R.color.white))
                btn_setting_target.setBackgroundResource(R.color.tab_on_bg_color)
                layout_setting_system.visibility = View.GONE
                layout_setting_count.visibility = View.GONE
                layout_setting_target.visibility = View.VISIBLE
            }
        }
    }

    private fun targetTypeChange(v : Int) {
        if (_selected_target_type == v) return
        when (_selected_target_type) {
            11 -> btn_server_accumulate.setTextColor(ContextCompat.getColor(this, R.color.gray))
            12 -> btn_server_hourly.setTextColor(ContextCompat.getColor(this, R.color.gray))
            13 -> btn_server_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.gray))
            21 -> btn_manual_accumulate.setTextColor(ContextCompat.getColor(this, R.color.gray))
            22 -> btn_manual_hourly.setTextColor(ContextCompat.getColor(this, R.color.gray))
            23 -> btn_manual_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.gray))
        }
        when (_selected_target_type) {
            in 11..13 -> tv_setting_target_type_server.setTextColor(ContextCompat.getColor(this, R.color.white))
            in 21..23 -> tv_setting_target_type_manual.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
        _selected_target_type = v
        when (_selected_target_type) {
            11 -> btn_server_accumulate.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
            12 -> btn_server_hourly.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
            13 -> btn_server_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
            21 -> btn_manual_accumulate.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
            22 -> btn_manual_hourly.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
            23 -> btn_manual_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
        }
        when (_selected_target_type) {
            in 11..13 -> tv_setting_target_type_server.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
            in 21..23 -> tv_setting_target_type_manual.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
        }
    }
}

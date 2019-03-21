package com.suntech.oee.cuttingmc

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.Toast
import com.suntech.oee.cuttingmc.base.BaseActivity
import com.suntech.oee.cuttingmc.common.AppGlobal
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*

class SettingActivity : BaseActivity() {

    private var tab_pos : Int = 1
    private var target_pos : Int = 1

    private var _selected_factory_idx:String = ""
    private var _selected_room_idx:String = ""
    private var _selected_line_idx:String = ""
    private var _selected_mc_no_idx:String = ""
    private var _selected_mc_model_idx:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        initView()
    }

    private fun initView() {
        tv_title.setText("SETTING")

        // set widget value
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

        // set hidden value
        _selected_factory_idx = AppGlobal.instance.get_factory_idx()
        _selected_room_idx = AppGlobal.instance.get_room_idx()
        _selected_line_idx = AppGlobal.instance.get_line_idx()
        _selected_mc_no_idx = AppGlobal.instance.get_mc_no_idx()
        _selected_mc_model_idx = AppGlobal.instance.get_mc_model_idx()

        // Tab button click
        btn_setting_system.setOnClickListener { tabChange(1) }
        btn_setting_count.setOnClickListener { tabChange(2) }
        btn_setting_target.setOnClickListener { tabChange(3) }

        // Target type button click
        btn_setting_target_type_server_accumulate.setOnClickListener { targetTypeChange(1) }
        btn_setting_target_type_server_hourly.setOnClickListener { targetTypeChange(2) }
        btn_setting_target_type_server_shifttotal.setOnClickListener { targetTypeChange(3) }
        btn_setting_target_type_manual_accumulate.setOnClickListener { targetTypeChange(4) }
        btn_setting_target_type_manual_hourly.setOnClickListener { targetTypeChange(5) }
        btn_setting_target_type_manual_shifttotal.setOnClickListener { targetTypeChange(6) }

        // Command button click
        btn_setting_confirm.setOnClickListener { saveSettingData() }
        btn_setting_cancel.setOnClickListener { finish() }
        btn_setting_check_server.setOnClickListener { checkServer() }
    }

    private fun checkServer() {
        var full_url = et_setting_server_ip.text.toString()
        if (full_url == "") {
            Toast.makeText(this, getString(R.string.msg_require_info), Toast.LENGTH_SHORT).show()
            return
        }

        AppGlobal.instance.set_server_ip(full_url)
        AppGlobal.instance.set_server_port(et_setting_port.text.toString())

        var params = listOf("" to "")

        request(this, "/ping.php", false, false, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveSettingData() {

        val machine_no = tv_setting_mc_no1.text.toString()

        if (tv_setting_factory.text.toString() == "" ||
                tv_setting_room.text.toString() == "" ||
                tv_setting_line.text.toString() == "" ||
                tv_setting_mac.text.toString() == "" || machine_no == "") {
            Toast.makeText(this, getString(R.string.msg_require_info), Toast.LENGTH_SHORT).show()
            return
        }
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

        var params = listOf(
                "code" to "server",
                "factory_parent_idx" to _selected_factory_idx,
                "factory_idx" to _selected_room_idx,
                "line_idx" to _selected_line_idx,
                "shift_idx" to AppGlobal.instance.get_current_shift_idx(),
                "mac_addr" to tv_setting_mac.text,
                "machine_no" to machine_no,
                "ip_addr" to tv_setting_ip.text,
                "mc_model" to tv_setting_mc_model.text,
                "mc_serial" to et_setting_mc_serial.text.toString()
        )

        request(this, "/setting1.php", false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                finish()
            }else{
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
        if (target_pos == v) return
        when (target_pos) {
            1 -> btn_setting_target_type_server_accumulate.setTextColor(ContextCompat.getColor(this, R.color.gray))
            2 -> btn_setting_target_type_server_hourly.setTextColor(ContextCompat.getColor(this, R.color.gray))
            3 -> btn_setting_target_type_server_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.gray))
            4 -> btn_setting_target_type_manual_accumulate.setTextColor(ContextCompat.getColor(this, R.color.gray))
            5 -> btn_setting_target_type_manual_hourly.setTextColor(ContextCompat.getColor(this, R.color.gray))
            6 -> btn_setting_target_type_manual_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.gray))
        }
        when (target_pos) {
            in 1..3 -> tv_setting_target_type_server.setTextColor(ContextCompat.getColor(this, R.color.white))
            in 4..6 -> tv_setting_target_type_manual.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
        target_pos = v
        when (target_pos) {
            1 -> btn_setting_target_type_server_accumulate.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
            2 -> btn_setting_target_type_server_hourly.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
            3 -> btn_setting_target_type_server_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
            4 -> btn_setting_target_type_manual_accumulate.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
            5 -> btn_setting_target_type_manual_hourly.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
            6 -> btn_setting_target_type_manual_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
        }
        when (target_pos) {
            in 1..3 -> tv_setting_target_type_server.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
            in 4..6 -> tv_setting_target_type_manual.setTextColor(ContextCompat.getColor(this, R.color.bottom_text_color))
        }
    }
}

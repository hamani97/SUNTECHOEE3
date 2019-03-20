package com.suntech.oee.cuttingmc

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import com.suntech.oee.cuttingmc.base.BaseActivity
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*

class SettingActivity : BaseActivity() {

    var tab_pos : Int = 1
    var target_pos : Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        initView()
    }

    private fun initView() {
        tv_title.setText("SETTING")

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
        btn_setting_confirm.setOnClickListener { finish() }
        btn_setting_cancel.setOnClickListener { finish() }
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

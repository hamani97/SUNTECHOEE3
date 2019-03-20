package com.suntech.oee.cuttingmc

import android.os.Bundle
import android.support.v4.content.ContextCompat
import com.suntech.oee.cuttingmc.base.BaseActivity
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*

class SettingActivity : BaseActivity() {

    var tab_pos : Int = 1

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

        // Command button click
        btn_setting_confirm.setOnClickListener { finish() }
        btn_setting_cancel.setOnClickListener { finish() }
    }

    private fun tabChange(v : Int) {
        if (tab_pos == v) return
        when (tab_pos) {
            1 -> {  btn_setting_system.setTextColor(ContextCompat.getColor(this, R.color.gray))
                    btn_setting_system.setBackgroundResource(R.color.tab_off_bg_color)
            }
            2 -> {  btn_setting_count.setTextColor(ContextCompat.getColor(this, R.color.gray))
                    btn_setting_count.setBackgroundResource(R.color.tab_off_bg_color)
            }
            3 -> {  btn_setting_target.setTextColor(ContextCompat.getColor(this, R.color.gray))
                    btn_setting_target.setBackgroundResource(R.color.tab_off_bg_color)
            }
        }
        tab_pos = v
        when (tab_pos) {
            1 -> {  btn_setting_system.setTextColor(ContextCompat.getColor(this, R.color.white))
                    btn_setting_system.setBackgroundResource(R.color.tab_on_bg_color)
            }
            2 -> {  btn_setting_count.setTextColor(ContextCompat.getColor(this, R.color.white))
                    btn_setting_count.setBackgroundResource(R.color.tab_on_bg_color)
            }
            3 -> {  btn_setting_target.setTextColor(ContextCompat.getColor(this, R.color.white))
                    btn_setting_target.setBackgroundResource(R.color.tab_on_bg_color)
            }
        }
    }
}

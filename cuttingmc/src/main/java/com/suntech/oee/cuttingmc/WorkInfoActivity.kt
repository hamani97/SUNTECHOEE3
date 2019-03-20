package com.suntech.oee.cuttingmc

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import com.suntech.oee.cuttingmc.base.BaseActivity
import kotlinx.android.synthetic.main.activity_work_info.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*

class WorkInfoActivity : BaseActivity() {

    var tab_pos : Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work_info)
        initView()
    }

    private fun initView() {
        tv_title.setText("WORK INFO")

        // Tab button click
        btn_work_info_server.setOnClickListener { tabChange(1) }
        btn_work_info_manual.setOnClickListener { tabChange(2) }

        // Command button click
        btn_setting_confirm.setOnClickListener { finish() }
        btn_setting_cancel.setOnClickListener { finish() }
    }

    private fun tabChange(v : Int) {
        if (tab_pos == v) return
        tab_pos = v
        when (tab_pos) {
            1 -> {
                btn_work_info_server.setTextColor(ContextCompat.getColor(this, R.color.white))
                btn_work_info_server.setBackgroundResource(R.color.tab_on_bg_color)
                btn_work_info_manual.setTextColor(ContextCompat.getColor(this, R.color.gray))
                btn_work_info_manual.setBackgroundResource(R.color.tab_off_bg_color)
                layout_work_info_server.visibility = View.VISIBLE
                layout_work_info_manual.visibility = View.GONE
            }
            2 -> {
                btn_work_info_server.setTextColor(ContextCompat.getColor(this, R.color.gray))
                btn_work_info_server.setBackgroundResource(R.color.tab_off_bg_color)
                btn_work_info_manual.setTextColor(ContextCompat.getColor(this, R.color.white))
                btn_work_info_manual.setBackgroundResource(R.color.tab_on_bg_color)
                layout_work_info_server.visibility = View.GONE
                layout_work_info_manual.visibility = View.VISIBLE
            }
        }
    }
}
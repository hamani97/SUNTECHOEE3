package com.suntech.oee.cuttingmc

import android.os.Bundle
import com.suntech.oee.cuttingmc.base.BaseActivity
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*

class SettingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        initView()
    }

    private fun initView() {
        tv_title.setText("SETTING")
        btn_setting_confirm.setOnClickListener { finish() }
        btn_setting_cancel.setOnClickListener { finish() }
    }
}

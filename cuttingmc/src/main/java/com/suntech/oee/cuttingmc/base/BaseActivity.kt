package com.suntech.oee.cuttingmc.base

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onResume() {
        super.onResume()
    }
    override fun onDestroy() {
        super.onDestroy()
    }
}
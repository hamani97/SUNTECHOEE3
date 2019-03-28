package com.suntech.oee.cuttingmc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.suntech.oee.cuttingmc.base.BaseFragment
import com.suntech.oee.cuttingmc.common.AppGlobal
import kotlinx.android.synthetic.main.fragment_component_view.*
import org.joda.time.DateTime

class ComponentViewFragment: BaseFragment() {

    private var is_loop :Boolean = false
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    private val _need_to_refresh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateView()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_component_view, container, false)
    }

    override fun onResume() {
        super.onResume()
        activity.registerReceiver(_need_to_refresh, IntentFilter("need.refresh"))
        is_loop=true
        updateView()
        startHandler()
    }

    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(_need_to_refresh)
        is_loop=false
    }

    override fun onSelected() {
        val no = AppGlobal.instance.get_worker_no()
        val name = AppGlobal.instance.get_worker_name()
        if (no== "" || name == "") {
            Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
            (activity as MainActivity).changeFragment(0)
        }
        updateView()
        fetchData()
        countTarget()
    }

    override fun initViews() {
        super.initViews()

        tv_count_view_target.text = "0"
        tv_count_view_actual.text = "0"
        tv_count_view_ratio.text = "0%"

        btn_total_count_view.setOnClickListener {
            (activity as MainActivity).changeFragment(1)
        }
        btn_select_component.setOnClickListener {
            val intent = Intent(activity, ComponentInfoActivity::class.java)
            startActivity(intent)
        }
        updateView()
        fetchData()
        countTarget()
    }

    private fun updateView() {
        tv_current_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")
    }

    var handle_cnt = 0
    fun startHandler () {
        val handler = Handler()
        handler.postDelayed({
            if (is_loop) {
                updateView()
                startHandler()
            }
        }, 1000)
    }

    private fun fetchData() {
        var list = AppGlobal.instance.get_color_code()

        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            var map=hashMapOf(
                    "idx" to item.getString("idx"),
                    "snumber" to item.getString("snumber"),
                    "enumber" to item.getString("enumber"),
                    "color_name" to item.getString("color_name"),
                    "color_code" to item.getString("color_code")
            )
            _list.add(map)
        }
    }

    private fun countTarget() {

    }
}
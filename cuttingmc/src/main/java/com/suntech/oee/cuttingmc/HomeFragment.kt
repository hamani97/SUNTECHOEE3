package com.suntech.oee.cuttingmc

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.suntech.oee.cuttingmc.base.BaseFragment
import com.suntech.oee.cuttingmc.common.AppGlobal
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.layout_bottom_info.*

class HomeFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun initViews() {
        tv_app_version.text = "v "+ activity.packageManager.getPackageInfo(activity.packageName, 0).versionName

        btn_count_view.setOnClickListener {
            (activity as MainActivity).countViewType = 1
            clickCountView()
        }
        btn_component_info.setOnClickListener {
            val intent = Intent(activity, ComponentInfoActivity::class.java)
            getBaseActivity().startActivity(intent, { r, c, m, d ->
                if (r && d != null) {
                    (activity as MainActivity).countViewType = 2
                    clickCountView()
                }
            })
        }
        btn_work_info.setOnClickListener { clickWorkInfo() }
//        btn_design_info.setOnClickListener { clickDesignInfo() }
        btn_setting_view.setOnClickListener { startActivity(Intent(activity, SettingActivity::class.java)) }
        updateView()
    }

    override fun onSelected() {
        updateView()
    }

    override fun onResume() {
        super.onResume()
        updateView()
    }

    private fun updateView() {
        val machine_no = AppGlobal.instance.get_mc_no1() //+ "-" + AppGlobal.instance.get_mc_no2()
        tv_factory.text = AppGlobal.instance.get_factory()
        tv_room.text = AppGlobal.instance.get_room()
        tv_line.text = AppGlobal.instance.get_line()
        tv_mc_no.text = machine_no
        tv_mc_model.text = AppGlobal.instance.get_mc_model()
        tv_employee_no.text = AppGlobal.instance.get_worker_no()
        tv_employee_name.text = AppGlobal.instance.get_worker_name()
        tv_shift.text = AppGlobal.instance.get_current_shift_name()
    }

    private fun clickCountView() {
        val no = AppGlobal.instance.get_worker_no()
        val name = AppGlobal.instance.get_worker_name()
        if (no=="" || name=="") {
            Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
            return
        }
        (activity as MainActivity).changeFragment(1)
    }
    private fun clickWorkInfo() {
        val f = AppGlobal.instance.get_factory()
        val r = AppGlobal.instance.get_room()
        val l = AppGlobal.instance.get_line()
        if (f==""||r==""||l=="") {
            Toast.makeText(activity, getString(R.string.msg_no_setting), Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(activity, WorkInfoActivity::class.java)
        startActivity(intent)
    }

    private fun clickComponentInfo() {
        val no = AppGlobal.instance.get_worker_no()
        val name = AppGlobal.instance.get_worker_name()
        if (no=="" || name=="") {
            Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
            return
        }
        (activity as MainActivity).changeFragment(2)
    }

    private fun clickDesignInfo() {
//        val no = AppGlobal.instance.get_worker_no()
//        val name = AppGlobal.instance.get_worker_name()
//        if (no==""||name=="") {
//            Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
//            return
//        }
//        btn_design_info.isEnabled = false
//        val intent = Intent(activity, DesignInfoActivity::class.java)
//        getBaseActivity().startActivity(intent, { r, c, m, d ->
//            btn_design_info.isEnabled = true
//            if (r && d!=null) {
//                val idx = d!!["idx"]!!
//                val cycle_time = d["ct"]!!.toInt()
//                val model = d["model"]!!.toString()
//                val article = d["article"]!!.toString()
//                val material_way = d["material_way"]!!.toString()
//                val component = d["component"]!!.toString()
//                val pieces_info = AppGlobal.instance.get_pieces_info()
//
//                (activity as MainActivity).startNewProduct(idx, pieces_info, cycle_time, model, article, material_way, component)
//            }
//        })
    }
}
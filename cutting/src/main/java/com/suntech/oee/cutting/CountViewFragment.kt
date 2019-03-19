package com.suntech.oee.cutting

import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.suntech.oee.cutting.base.BaseFragment
import com.suntech.oee.cutting.common.AppGlobal
import com.suntech.oee.cutting.db.SimpleDatabaseHelper
import com.suntech.oee.cutting.util.OEEUtil
import kotlinx.android.synthetic.main.fragment_count_view.*
import kotlinx.android.synthetic.main.layout_bottom_info_2.*
import org.joda.time.DateTime
import kotlin.math.ceil

class CountViewFragment : BaseFragment() {

    private var is_loop :Boolean = false
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _list_for_db: ArrayList<HashMap<String, String>> = arrayListOf()

    private var _total_target = 0

    private val _need_to_refresh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateView()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_count_view, container, false)
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

    override fun initViews() {
        super.initViews()

        tv_count_view_target.text = "0"
        tv_count_view_actual.text = "0"
        tv_count_view_ratio.text = "0%"
        tv_count_view_time.text = "0H"
        tv_count_view_time_ms.text = "0M 0S"

        btn_exit.setOnClickListener {
            val work_idx = ""+ AppGlobal.instance.get_product_idx()
            if (work_idx=="") {
                Toast.makeText(activity, getString(R.string.msg_not_start_work), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val alertDialogBuilder = AlertDialog.Builder(activity)
            alertDialogBuilder.setTitle(getString(R.string.notice))
            alertDialogBuilder
                    .setMessage(getString(R.string.msg_exit_shift))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.confirm), DialogInterface.OnClickListener { dialog, id ->
                        (activity as MainActivity).changeFragment(0)
                        (activity as MainActivity).endWork()
                    })
                    .setNegativeButton(getString(R.string.cancel), DialogInterface.OnClickListener { dialog, id ->
                        dialog.cancel()
                    } )
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()

        }
        ib_pieces_1.setOnClickListener {
            var accumulated_count = AppGlobal.instance.get_accumulated_count()
            if (accumulated_count>0) accumulated_count-- else activity.sendBroadcast(Intent(Constants.BR_ADD_COUNT))
            AppGlobal.instance.set_accumulated_count(accumulated_count)
            updateView()
        }
        ib_pieces_2_1.setOnClickListener {
            var accumulated_count = AppGlobal.instance.get_accumulated_count()
            if (accumulated_count==0) activity.sendBroadcast(Intent(Constants.BR_ADD_COUNT))
            else accumulated_count--

            AppGlobal.instance.set_accumulated_count(accumulated_count)
            updateView()
        }
        ib_pieces_2_2.setOnClickListener {
            var accumulated_count = AppGlobal.instance.get_accumulated_count()
            activity.sendBroadcast(Intent(Constants.BR_ADD_COUNT))
            AppGlobal.instance.set_accumulated_count(accumulated_count)
            updateView()
        }
        ib_pieces_4_1.setOnClickListener {
            var accumulated_count = AppGlobal.instance.get_accumulated_count()
            if (accumulated_count==0) activity.sendBroadcast(Intent(Constants.BR_ADD_COUNT))
            else accumulated_count--
            AppGlobal.instance.set_accumulated_count(accumulated_count)
            updateView()
        }
        ib_pieces_4_2.setOnClickListener {
            var accumulated_count = AppGlobal.instance.get_accumulated_count()
            activity.sendBroadcast(Intent(Constants.BR_ADD_COUNT))
            AppGlobal.instance.set_accumulated_count(accumulated_count)
            updateView()
        }
        btn_defective_plus.setOnClickListener {
            val work_idx = ""+AppGlobal.instance.get_product_idx()
            if (work_idx=="") {
                Toast.makeText(activity, getString(R.string.msg_not_start_work), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val shift_idx = AppGlobal.instance.get_current_shift_idx()
            if (shift_idx=="") {
                Toast.makeText(activity, getString(R.string.msg_no_selected_shift), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendDefective()
        }

        updateView()
        fetchData()

    }

    override fun onSelected() {
        val no = AppGlobal.instance.get_worker_no()
        val name = AppGlobal.instance.get_worker_name()
        if (no==""||name=="") {
            Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
            (activity as MainActivity).changeFragment(0)
        }

        updateView()
        fetchData()
    }

    private fun updateView() {
        tv_design_idx.text = AppGlobal.instance.get_design_info_idx()
        tv_pieces.text = AppGlobal.instance.get_pieces_info().toString()
        tv_cycle_time.text = AppGlobal.instance.get_cycle_time().toString()

        tv_article.text = AppGlobal.instance.get_article()
        tv_model.text = AppGlobal.instance.get_model()
        tv_material.text = AppGlobal.instance.get_material_way()
        tv_component.text = AppGlobal.instance.get_component()
        tv_current_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")

        val pieces_info = AppGlobal.instance.get_pieces_info()
        tv_side_pieces.text = "= " + pieces_info + " Pieces"

        val accumulated_count = AppGlobal.instance.get_accumulated_count()

        ib_pieces_1.isSelected = false
        ib_pieces_2_1.isSelected = false
        ib_pieces_2_2.isSelected = false
        ib_pieces_4_1.isSelected = false
        ib_pieces_4_2.isSelected = false
        ib_pieces_4_1.isActivated = false
        ib_pieces_4_2.isActivated = false

        ib_pieces_1.visibility = View.GONE
        ib_pieces_2_1.visibility = View.GONE
        ib_pieces_2_2.visibility = View.GONE
        ib_pieces_4_1.visibility = View.GONE
        ib_pieces_4_2.visibility = View.GONE
        if (pieces_info==1) {
            ib_pieces_1.visibility = View.VISIBLE

            if (accumulated_count>0) ib_pieces_1.isSelected = true
        }
        if (pieces_info==2) {
            ib_pieces_2_1.visibility = View.VISIBLE
            ib_pieces_2_2.visibility = View.VISIBLE
            if (accumulated_count>0) ib_pieces_2_1.isSelected = true
            if (accumulated_count>1) ib_pieces_2_2.isSelected = true
        }
        if (pieces_info==4) {
            ib_pieces_4_1.visibility = View.VISIBLE
            ib_pieces_4_2.visibility = View.VISIBLE
            if (accumulated_count>0) ib_pieces_4_1.isSelected = true
            if (accumulated_count>1) ib_pieces_4_1.isActivated = true
            if (accumulated_count>2) ib_pieces_4_2.isSelected = true
            if (accumulated_count>3) ib_pieces_4_2.isActivated = true
        }

        var db = SimpleDatabaseHelper(activity)

        val work_idx = AppGlobal.instance.get_product_idx()
        if (work_idx=="") return

        val item = db.get(work_idx)
        if (item !=null && item.toString()!="") {

            val target = item["target"].toString()
            val actual = (item["actual"].toString().toInt()) / pieces_info

            val elapsedTime = AppGlobal.instance.get_current_shift_accumulated_time()

            val h = (elapsedTime / 3600)
            val m = ((elapsedTime - (h*3600)) / 60)
            val s = ((elapsedTime - (h*3600)) - m*60 )

            tv_count_view_target.text = ""+target
            tv_count_view_actual.text = ""+actual
            tv_count_view_time.text = "" + h + "H"
            tv_count_view_time_ms.text = "" + m  + "M " + s + "S"
        }

        // 전체
        _list_for_db = db.gets() ?: _list_for_db

        var total_target = 0
        var total_actual = 0

        for (i in 0..(_list_for_db.size - 1)) {

            val item = _list_for_db[i]
            val actual = item["actual"]?.toInt() ?: 0
            val target = item["target"]?.toInt() ?: 0
            total_actual += actual
            total_target += target
        }

        var ratio = (total_actual.toFloat() / total_target.toFloat() * 100).toInt()
        if (ratio>999) ratio=999
        var ratio_txt = "" + ratio + "%"
        if (total_target==0) ratio_txt = "N/A"
        tv_count_view_target.text = ""+total_target
        tv_count_view_actual.text = ""+total_actual
        tv_count_view_ratio.text = ratio_txt

        var maxEnumber = 0
        var color_code = "ffffff"
        for (i in 0..(_list.size - 1)) {
            val row = _list[i]
            val snumber = row["snumber"]?.toInt() ?: 0
            val enumber = row["enumber"]?.toInt() ?: 0
            color_code = row["color_code"].toString()
            if (maxEnumber<enumber) maxEnumber = enumber
            if (snumber<=ratio && enumber>=ratio) {
                tv_count_view_target.setTextColor(Color.parseColor("#"+color_code))
                tv_count_view_actual.setTextColor(Color.parseColor("#"+color_code))
                tv_count_view_ratio.setTextColor(Color.parseColor("#"+color_code))
                tv_count_view_time.setTextColor(Color.parseColor("#"+color_code))
            }
        }
        if (maxEnumber<ratio) {
            tv_count_view_target.setTextColor(Color.parseColor("#"+color_code))
            tv_count_view_actual.setTextColor(Color.parseColor("#"+color_code))
            tv_count_view_ratio.setTextColor(Color.parseColor("#"+color_code))
            tv_count_view_time.setTextColor(Color.parseColor("#"+color_code))
        }

        drawChartView2()
    }

    private fun drawChartView2() {

        var availability = AppGlobal.instance.get_availability()
        var performance = AppGlobal.instance.get_performance()
        var quality = AppGlobal.instance.get_quality()

        if (availability=="") availability = "0"
        if (performance=="") performance = "0"
        if (quality=="") quality = "0"

        var oee = availability.toFloat()*performance.toFloat()*quality.toFloat()/10000.0f
        var oee2 = String.format("%.1f", oee)
        oee2 = oee2.replace(",",".")//??

        tv_oee_1.text = oee2 + "%"
        tv_oee_2.text = availability + "%"
        tv_oee_3.text = performance + "%"
        tv_oee_4.text = quality + "%"

        line_progress1.progress = oee.toInt()
        line_progress2.progress = ceil(availability.toFloat()).toInt()
        line_progress3.progress = ceil(performance.toFloat()).toInt()
        line_progress4.progress = ceil(quality.toFloat()).toInt()
    }

    private fun drawChartView() {
        val total_planned_time = AppGlobal.instance.get_current_shift_total_time()

        var db = SimpleDatabaseHelper(activity)
        var list = db.gets() ?: return

        var total_target = 0
        var total_actual = 0
        var total_defective = 0
        var total_work_time = 0

        for (i in 0..(list.size - 1)) {

            val item = list[i]

            val start_dt_txt = item["start_dt"]
            val end_dt_txt = item["end_dt"]
            var start_dt = OEEUtil.parseDateTime(start_dt_txt)
            var end_dt = if (end_dt_txt==null) DateTime() else OEEUtil.parseDateTime(end_dt_txt)

            var dif = AppGlobal.instance.compute_work_time(start_dt, end_dt)

            val target = item["target"]?.toInt() ?: 0
            val target_no_contraint = item["target_no_contraint"]?.toInt() ?: 0
            val actual = item["actual"]?.toInt() ?: 0
            val defective = item["defective"]?.toInt() ?: 0

            total_target += target_no_contraint
            total_actual += actual
            total_defective += defective
            total_work_time += dif
        }

        val quality_factor = if (total_actual==0) 0f else (total_actual - total_defective).toFloat() / total_actual
        val performance_factor = if (total_target==0) 0f else (total_actual.toFloat() / total_target)
        val available_factor = total_work_time.toFloat() / total_planned_time

        //Log.e("test", "quality_factor = " + quality_factor)
        //Log.e("test", "performance_factor = " + performance_factor)
        //Log.e("test", "available_factor = " + available_factor)

        line_progress1.progress = 100-((available_factor*performance_factor*quality_factor)*100).toInt()
        line_progress2.progress = (available_factor*100).toInt()
        line_progress3.progress = (performance_factor*100).toInt()
        line_progress4.progress = (quality_factor*100).toInt()
    }

    private fun sendDefective() {
        val work_idx = AppGlobal.instance.get_product_idx()
        var db = SimpleDatabaseHelper(activity)
        val row = db.get(work_idx)
        val defective = row!!["defective"].toString().toInt()
        val seq = row!!["seq"].toString().toInt()
        db.updateDefective(work_idx, defective+1)

        val uri = "/defectivedata.php"
        var params = listOf("mac_addr" to AppGlobal.instance.getMACAddress(),
                "didx" to AppGlobal.instance.get_design_info_idx(),
                "shift_idx" to AppGlobal.instance.get_current_shift_idx(),
                "defective_idx" to "5", // 5고정
                "cnt" to "1",
                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                "factory_idx" to AppGlobal.instance.get_room_idx(),
                "line_idx" to AppGlobal.instance.get_line_idx(),
                "seq" to ""+seq)

        getBaseActivity().request(activity, uri, false, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){
                AppGlobal.instance.playSound(activity)
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

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
}

package com.suntech.oee.cuttingmc

import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.suntech.oee.cuttingmc.base.BaseFragment
import com.suntech.oee.cuttingmc.common.AppGlobal
import com.suntech.oee.cuttingmc.db.SimpleDatabaseHelper
import com.suntech.oee.cuttingmc.util.OEEUtil
import kotlinx.android.synthetic.main.fragment_count_view.*
import kotlinx.android.synthetic.main.layout_bottom_info_2.*
import org.joda.time.DateTime

class CountViewFragment : BaseFragment() {

    private var is_loop :Boolean = false
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()           // Color
    private var _list_for_db: ArrayList<HashMap<String, String>> = arrayListOf()

    private var _total_target = 0

    private var _list_for_wos_adapter: ListWosAdapter? = null
    private var _list_for_wos: java.util.ArrayList<java.util.HashMap<String, String>> = arrayListOf()

    private val _need_to_refresh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateView()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_count_view, container, false)

        _list_for_wos_adapter = ListWosAdapter(activity, _list_for_wos)
        lv_wos_info2.adapter = _list_for_wos_adapter
    }

    override fun onResume() {
        super.onResume()
        activity.registerReceiver(_need_to_refresh, IntentFilter("need.refresh"))
        is_loop=true
        updateView()
        fetchWosAll()
        startHandler()
    }

    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(_need_to_refresh)
        is_loop=false
    }

    override fun onSelected() {
        if ((activity as MainActivity).countViewType == 1) {
            ll_total_count.visibility = View.VISIBLE
            ll_component_count.visibility = View.GONE
        } else {
            ll_total_count.visibility = View.GONE
            ll_component_count.visibility = View.VISIBLE
        }

        // Worker info
        val no = AppGlobal.instance.get_worker_no()
        val name = AppGlobal.instance.get_worker_name()
        if (no== "" || name == "") {
            Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
            (activity as MainActivity).changeFragment(0)
        }

        updateView()
        fetchColorData()     // Get Color

        if ((activity as MainActivity).countViewType == 1) {
            countTarget()
        } else {
            countTargetComponent()
        }
    }

    override fun initViews() {
        super.initViews()

        // Total count view
        tv_count_view_target.text = "0"
        tv_count_view_actual.text = "0"
        tv_count_view_ratio.text = "0%"
        tv_count_view_time.text = "0H"

        // Component count view
        tv_component_view_target.text = "0"
        tv_component_view_actual.text = "0"
        tv_component_view_ratio.text = "0%"

        // Total count view
        btn_start.setOnClickListener {
//            (activity as MainActivity).saveRowData("barcode", value)
        }
        btn_exit.setOnClickListener {
            val work_idx = ""+ AppGlobal.instance.get_product_idx()
            if (work_idx == "") {
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

        // Component count view
        btn_total_count_view.setOnClickListener {
            (activity as MainActivity).countViewType = 1
            ll_total_count.visibility = View.VISIBLE
            ll_component_count.visibility = View.GONE
        }
        btn_select_component.setOnClickListener {
            val intent = Intent(activity, ComponentInfoActivity::class.java)
            startActivity(intent)
        }

        updateView()
        fetchColorData()     // Get Color

        if ((activity as MainActivity).countViewType == 1) {
            countTarget()
        } else {
            countTargetComponent()
        }
    }

    private fun countTarget() {

        val now_time = DateTime()
        val current_shift_time = AppGlobal.instance.get_current_shift_time()
        val work_stime = OEEUtil.parseDateTime(current_shift_time?.getString("work_stime"))
        val work_etime = OEEUtil.parseDateTime(current_shift_time?.getString("work_etime"))

        var target_type = AppGlobal.instance.get_target_type()
        if (target_type=="server_per_hourly" || target_type=="server_per_accumulate" || target_type=="server_per_day_total") {
            fetchServerTarget()
        }
        else if (target_type=="device_per_hourly") {
            var total_actual = 0
            var total_target = 0

            var target_stime = work_stime
            while (target_stime.plusHours(1).millis < now_time.millis) {
                target_stime = target_stime.plusHours(1)
            }

            for (i in 0..(_list_for_db.size - 1)) {

                val product = _list_for_db[i]
                val actual = product["actual"]?.toInt() ?: 0
                val target = product["target"]?.toInt() ?: 0
                total_actual += actual

                var product_stime = OEEUtil.parseDateTime(product["start_dt"])
                if (i==0) product_stime = work_stime
                if (product_stime.millis<target_stime.millis) product_stime = target_stime

                var product_etime = if (product["end_dt"]!=null ) OEEUtil.parseDateTime(product["end_dt"]) else now_time
                if (product_etime.millis>work_etime.millis) product_etime = work_etime

                if (product_etime.millis<target_stime.millis) continue

                val t = AppGlobal.instance.compute_work_time(product_stime, product_etime, false, false)
                val ct = product["cycle_time"]?.toInt() ?: 0
                total_target += ( t / ct )
            }
            _total_target = total_target
        }
        else if (target_type=="device_per_accumulate") {
            var total_actual = 0
            var total_target = 0

            // 쉬프트타임내의 각각의 프로덕트 작업시간을 계산하여 total target을 계산함 (프로덕트마다 cycle time이 다르므로)
            // 단, 첫 프로덕트의 경우 쉬프트 시작시간이 작업시작시간으로 계산
            // 단, 작업시간이 쉬프트 종료시간을 넘긴 경우, 쉬프트 종료시간까지만 계산
            for (i in 0..(_list_for_db.size - 1)) {

                val product = _list_for_db[i]
                val actual = product["actual"]?.toInt() ?: 0
                val target = product["target"]?.toInt() ?: 0
                total_actual += actual

                var product_stime = OEEUtil.parseDateTime(product["start_dt"])
                if (i==0) product_stime = work_stime

                var product_etime = if (product["end_dt"]!=null ) OEEUtil.parseDateTime(product["end_dt"]) else now_time
                if (product_etime.millis>work_etime.millis) product_etime = work_etime

                val t = AppGlobal.instance.compute_work_time(product_stime, product_etime, false, false)
                val ct = product["cycle_time"]?.toInt() ?: 0
                total_target += ( t / ct )
            }
            _total_target = total_target
        }
        else if (target_type=="device_per_day_total") {
            var total_actual = 0
            var total_target = 0

            // 쉬프트타임내의 각각의 프로덕트 작업시간을 계산하여 total target을 계산함 (프로덕트마다 cycle time이 다르므로)
            // 단, 첫 프로덕트의 경우 쉬프트 시작시간이 작업시작시간으로 계산
            // 단, 작업시간이 쉬프트 종료시간을 넘긴 경우, 쉬프트 종료시간까지만 계산
            for (i in 0..(_list_for_db.size - 1)) {

                val product = _list_for_db[i]
                val actual = product["actual"]?.toInt() ?: 0
                val target = product["target"]?.toInt() ?: 0
                total_actual += actual

                var product_stime = OEEUtil.parseDateTime(product["start_dt"])
                var product_etime = work_etime
                if (i==0) product_stime = work_stime

                val t = AppGlobal.instance.compute_work_time(product_stime, product_etime, false, false)
                val ct = product["cycle_time"]?.toInt() ?: 0
                total_target += ( t / ct )
            }
            _total_target = total_target
        }
    }

    private fun countTargetComponent() {

    }

    private fun updateView() {

        if ((activity as MainActivity).countViewType == 1) {
            // Total count view
            tv_design_idx.text = AppGlobal.instance.get_design_info_idx()
            tv_pieces.text = AppGlobal.instance.get_pieces_info().toString()
            tv_cycle_time.text = AppGlobal.instance.get_cycle_time().toString()

            tv_article.text = AppGlobal.instance.get_article()
            tv_model.text = AppGlobal.instance.get_model()
            tv_material.text = AppGlobal.instance.get_material_way()
            tv_component.text = AppGlobal.instance.get_component()

            tv_current_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")

            val pieces_info = AppGlobal.instance.get_pieces_info()
            //        tv_side_pieces.text = "= " + pieces_info + " Pieces"

            val accumulated_count = AppGlobal.instance.get_accumulated_count()

            var db = SimpleDatabaseHelper(activity)

            val work_idx = AppGlobal.instance.get_product_idx()
            if (work_idx == "") return
            val item = db.get(work_idx)
            if (item != null && item.toString() != "") {
                val target = item["target"].toString()
                val actual = (item["actual"].toString().toInt()) / pieces_info

                val elapsedTime = AppGlobal.instance.get_current_shift_accumulated_time()

                val h = (elapsedTime / 3600)
                val m = ((elapsedTime - (h * 3600)) / 60)
                val s = ((elapsedTime - (h * 3600)) - m * 60)

                tv_count_view_target.text = "" + target
                tv_count_view_actual.text = "" + actual
                tv_count_view_time.text = "" + h + "H"
            }

            // 전체
            _list_for_db = db.gets() ?: _list_for_db

            var total_target = 0
            var total_actual = 0

            // 쉬프트타임내의 각각의 프로덕트 작업시간을 계산하여 total target을 계산함 (프로덕트마다 cycle time이 다르므로)
            // 단, 첫 프로덕트의 경우 쉬프트 시작시간이 작업시작시간으로 계산
            // 단, 작업시간이 쉬프트 종료시간을 넘긴 경우, 쉬프트 종료시간까지만 계산
            for (i in 0..(_list_for_db.size - 1)) {
                val item = _list_for_db[i]
                val actual = item["actual"]?.toInt() ?: 0
                val target = item["target"]?.toInt() ?: 0
                total_actual += actual
                total_target += target
                /*
                val current_shift_time = AppGlobal.instance.get_current_shift_time()
                var work_stime = OEEUtil.parseDateTime(current_shift_time?.getString("work_stime"))
                var work_etime = OEEUtil.parseDateTime(current_shift_time?.getString("work_etime"))

                var start_dt = OEEUtil.parseDateTime(item["start_dt"])
                var end_dt = if (item["end_dt"]!=null ) OEEUtil.parseDateTime(item["end_dt"]) else work_etime
                if (i==0) start_dt = work_stime
                val t = AppGlobal.instance.compute_work_time(start_dt, end_dt, false, false)
                val ct = item["cycle_time"]?.toInt() ?: 0
                total_target += ( t / ct )
    */
            }

            var ratio = (total_actual.toFloat() / total_target.toFloat() * 100).toInt()
            if (ratio > 999) ratio = 999
            var ratio_txt = "" + ratio + "%"
            if (total_target == 0) ratio_txt = "N/A"

            tv_count_view_target.text = "" + total_target
            tv_count_view_actual.text = "" + total_actual
            tv_count_view_ratio.text = ratio_txt

            var maxEnumber = 0
            var color_code = "ffffff"
            for (i in 0..(_list.size - 1)) {
                val row = _list[i]
                val snumber = row["snumber"]?.toInt() ?: 0
                val enumber = row["enumber"]?.toInt() ?: 0
                color_code = row["color_code"].toString()
                if (maxEnumber < enumber) maxEnumber = enumber
                if (snumber <= ratio && enumber >= ratio) {
                    tv_count_view_target.setTextColor(Color.parseColor("#" + color_code))
                    tv_count_view_actual.setTextColor(Color.parseColor("#" + color_code))
                    tv_count_view_ratio.setTextColor(Color.parseColor("#" + color_code))
                    tv_count_view_time.setTextColor(Color.parseColor("#" + color_code))
                }
            }
            if (maxEnumber < ratio) {
                tv_count_view_target.setTextColor(Color.parseColor("#" + color_code))
                tv_count_view_actual.setTextColor(Color.parseColor("#" + color_code))
                tv_count_view_ratio.setTextColor(Color.parseColor("#" + color_code))
                tv_count_view_time.setTextColor(Color.parseColor("#" + color_code))
            }

        } else {
            tv_component_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")
        }
    }

    private fun fetchServerTarget() {
        val work_idx = AppGlobal.instance.get_product_idx()
        var db = SimpleDatabaseHelper(activity)
        val row = db.get(work_idx)
        val uri = "/getlist1.php"
        var params = listOf("code" to "target",
                "line_idx" to AppGlobal.instance.get_line_idx(),
                "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
                "date" to DateTime().toString("yyyy-MM-dd"),
                "mac_addr" to AppGlobal.instance.getMACAddress()
        )

        getBaseActivity().request(activity, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00") {
                var target = result.getString("target")
                var targetsum = result.getString("targetsum")
                var daytargetsum = result.getString("daytargetsum")
                _total_target = targetsum.toInt()

                var target_type = AppGlobal.instance.get_target_type()
                if (target_type=="server_per_hourly") _total_target = target.toInt()
                if (target_type=="server_per_accumulate") _total_target = targetsum.toInt()
                if (target_type=="server_per_day_total") _total_target = daytargetsum.toInt()

                updateView()
            } else {
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            }
        })
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

    // Get Color code
    private fun fetchColorData() {

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

    private fun fetchWosAll() {
        val uri = "/wos.php"
        var params = listOf("code" to "wos")

        getBaseActivity().request(activity, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var list = result.getJSONArray("item")
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map = hashMapOf(
                            "wosno" to item.getString("wosno"),
                            "styleno" to item.getString("styleno"),
                            "model" to item.getString("model"),
                            "size" to item.getString("size"),
                            "target" to item.getString("target")
                    )
                    _list_for_wos.add(map)
                }
                _list_for_wos_adapter?.notifyDataSetChanged()
//                filterWosData()
            } else {
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private class ListWosAdapter(context: Context, list: java.util.ArrayList<java.util.HashMap<String, String>>) : BaseAdapter() {

        private var _list: java.util.ArrayList<java.util.HashMap<String, String>>
        private val _inflator: LayoutInflater
        private var _context : Context? =null
        private var _selected_index = -1

        init {
            this._inflator = LayoutInflater.from(context)
            this._list = list
            this._context = context
        }

        fun select(index:Int) { _selected_index = index }
        fun getSelected(): Int { return _selected_index }

        override fun getCount(): Int { return _list.size }
        override fun getItem(position: Int): Any { return _list[position] }
        override fun getItemId(position: Int): Long { return position.toLong() }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view: View?
            val vh: ViewHolder
            if (convertView == null) {
                view = this._inflator.inflate(R.layout.list_wos_info, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_wosno.text = _list[position]["wosno"]
            vh.tv_item_model.text = _list[position]["model"]
            vh.tv_item_size.text = _list[position]["size"]
            vh.tv_item_target.text = _list[position]["target"]
            vh.tv_item_actual.text = "0"
            vh.tv_item_balance.text = _list[position]["target"]

            if (_selected_index==position) {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_size.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_target.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_actual.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_balance.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
            } else {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_size.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_target.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_actual.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_balance.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_wosno: TextView
            val tv_item_model: TextView
            val tv_item_size: TextView
            val tv_item_target: TextView
            val tv_item_actual: TextView
            val tv_item_balance: TextView

            init {
                this.tv_item_wosno = row?.findViewById<TextView>(R.id.tv_item_wosno) as TextView
                this.tv_item_model = row?.findViewById<TextView>(R.id.tv_item_model) as TextView
                this.tv_item_size = row?.findViewById<TextView>(R.id.tv_item_size) as TextView
                this.tv_item_target = row?.findViewById<TextView>(R.id.tv_item_target) as TextView
                this.tv_item_actual = row?.findViewById<TextView>(R.id.tv_item_actual) as TextView
                this.tv_item_balance = row?.findViewById<TextView>(R.id.tv_item_balance) as TextView
            }
        }
    }
}


package com.suntech.oee.cuttingmc

import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.suntech.oee.cuttingmc.base.BaseActivity
import com.suntech.oee.cuttingmc.common.AppGlobal
import com.suntech.oee.cuttingmc.db.DBHelperForSetting
import com.suntech.oee.cuttingmc.util.OEEUtil
import kotlinx.android.synthetic.main.activity_work_info.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*
import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONObject

class WorkInfoActivity : BaseActivity() {

    private var tab_pos : Int = 1

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    var _selected_index = -1

    private var _list_json: JSONArray? = null

    private var list_for_operator_adapter: ListOperatorAdapter? = null
    private var _list_for_operator: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _filtered_list_for_operator: ArrayList<HashMap<String, String>> = arrayListOf()

    private var list_for_last_worker_adapter: ListOperatorAdapter? = null
    private var _list_for_last_worker: ArrayList<HashMap<String, String>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work_info)
        initView()
        fetchData()
        fetchOperatorData()
        initLastWorkers()
    }

    private fun initView() {
        tv_title.text = "WORK INFO"

        list_adapter = ListAdapter(this, _list)
        lv_available_info.adapter = list_adapter

        _selected_index = AppGlobal.instance.get_current_shift_time_idx()

        list_for_operator_adapter = ListOperatorAdapter(this, _filtered_list_for_operator)
        lv_operator_info.adapter = list_for_operator_adapter

        lv_operator_info.setOnItemClickListener { adapterView, view, i, l ->
            list_for_operator_adapter?.select(i)
            list_for_operator_adapter?.notifyDataSetChanged()
        }

        list_for_last_worker_adapter = ListOperatorAdapter(this, _list_for_last_worker)
        lv_last_worker.adapter = list_for_last_worker_adapter

        lv_last_worker.setOnItemClickListener { adapterView, view, i, l ->
            et_setting_server_ip.setText("")

            list_for_last_worker_adapter?.select(i)
            list_for_last_worker_adapter?.notifyDataSetChanged()

            var list = AppGlobal.instance.get_last_workers()
            val worker = list.getJSONObject(list.length() - 1 - i)

            for (j in 0..(_list_for_operator.size-1)) {
                val item = _list_for_operator[j]
                val number = item["number"] ?: ""
                if (number == worker.getString("number")) {
                    list_for_operator_adapter?.select(j)
                    list_for_operator_adapter?.notifyDataSetChanged()
                    lv_operator_info.smoothScrollToPosition(j)
                    break
                }
            }
        }

        et_setting_server_ip.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s != "") {
                    filterOperatorData()
                }
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        // Tab button click
        btn_work_info_server.setOnClickListener { tabChange(1) }
        btn_work_info_manual.setOnClickListener { tabChange(2) }

        // Command button click
        btn_setting_confirm.setOnClickListener {
            val selected_index = list_for_operator_adapter?.getSelected() ?:-1
            if (selected_index < 0) {
                Toast.makeText(this, getString(R.string.msg_has_notselected), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (_filtered_list_for_operator != null && selected_index >= 0) {
                val no = _filtered_list_for_operator[selected_index]["number"]!!
                val name = _filtered_list_for_operator[selected_index]["name"]!!
                AppGlobal.instance.set_worker_no(no)
                AppGlobal.instance.set_worker_name(name)
                AppGlobal.instance.push_last_worker(no, name)
            }
            saveWorkTime()
        }
        btn_setting_cancel.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        updateView()
    }

    private fun updateView() {
        val work_list = AppGlobal.instance.get_today_work_time_manual()
        val work1 = work_list.getJSONObject(0)
        val work2 = work_list.getJSONObject(1)
        val work3 = work_list.getJSONObject(2)
        val work1_stime = OEEUtil.parseDateTime(work1["work_stime"].toString())
        val work1_etime = OEEUtil.parseDateTime(work1["work_etime"].toString())
        val work2_stime = OEEUtil.parseDateTime(work2["work_stime"].toString())
        val work2_etime = OEEUtil.parseDateTime(work2["work_etime"].toString())
        val work3_stime = OEEUtil.parseDateTime(work3["work_stime"].toString())
        val work3_etime = OEEUtil.parseDateTime(work3["work_etime"].toString())
        val planned1_stime_dt = OEEUtil.parseDateTime(work1["planned1_stime_dt"].toString())
        val planned1_etime_dt = OEEUtil.parseDateTime(work1["planned1_etime_dt"].toString())
        val planned2_stime_dt = OEEUtil.parseDateTime(work1["planned2_stime_dt"].toString())
        val planned2_etime_dt = OEEUtil.parseDateTime(work1["planned2_etime_dt"].toString())
        val planned3_stime_dt = OEEUtil.parseDateTime(work1["planned3_stime_dt"].toString())
        val planned3_etime_dt = OEEUtil.parseDateTime(work1["planned3_etime_dt"].toString())

        et_setting_s_1_s_h.setText(""+work1_stime.toString("HH"))
        et_setting_s_1_s_m.setText(""+work1_stime.toString("mm"))
        et_setting_s_1_e_h.setText(""+work1_etime.toString("HH"))
        et_setting_s_1_e_m.setText(""+work1_etime.toString("mm"))
        et_setting_s_2_s_h.setText(""+work2_stime.toString("HH"))
        et_setting_s_2_s_m.setText(""+work2_stime.toString("mm"))
        et_setting_s_2_e_h.setText(""+work2_etime.toString("HH"))
        et_setting_s_2_e_m.setText(""+work2_etime.toString("mm"))
        et_setting_s_3_s_h.setText(""+work3_stime.toString("HH"))
        et_setting_s_3_s_m.setText(""+work3_stime.toString("mm"))
        et_setting_s_3_e_h.setText(""+work3_etime.toString("HH"))
        et_setting_s_3_e_m.setText(""+work3_etime.toString("mm"))

        et_setting_p_1_s_h.setText(""+planned1_stime_dt.toString("HH"))
        et_setting_p_1_s_m.setText(""+planned1_stime_dt.toString("mm"))
        et_setting_p_1_e_h.setText(""+planned1_etime_dt.toString("HH"))
        et_setting_p_1_e_m.setText(""+planned1_etime_dt.toString("mm"))
        et_setting_p_2_s_h.setText(""+planned2_stime_dt.toString("HH"))
        et_setting_p_2_s_m.setText(""+planned2_stime_dt.toString("mm"))
        et_setting_p_2_e_h.setText(""+planned2_etime_dt.toString("HH"))
        et_setting_p_2_e_m.setText(""+planned2_etime_dt.toString("mm"))
        et_setting_p_3_s_h.setText(""+planned3_stime_dt.toString("HH"))
        et_setting_p_3_s_m.setText(""+planned3_stime_dt.toString("mm"))
        et_setting_p_3_e_h.setText(""+planned3_etime_dt.toString("HH"))
        et_setting_p_3_e_m.setText(""+planned3_etime_dt.toString("mm"))
    }

    private fun saveWorkTime() {
        val now_time = DateTime()
        val yesterday = now_time.plusDays(-1)

        var setting_s_1_s_h = if (et_setting_s_1_s_h.text.toString() =="") "00" else et_setting_s_1_s_h.text.toString()
        var setting_s_1_s_m = if (et_setting_s_1_s_m.text.toString() =="") "00" else et_setting_s_1_s_m.text.toString()
        var setting_s_1_e_h = if (et_setting_s_1_e_h.text.toString() =="") "00" else et_setting_s_1_e_h.text.toString()
        var setting_s_1_e_m = if (et_setting_s_1_e_m.text.toString() =="") "00" else et_setting_s_1_e_m.text.toString()
        var setting_s_2_s_h = if (et_setting_s_2_s_h.text.toString() =="") "00" else et_setting_s_2_s_h.text.toString()
        var setting_s_2_s_m = if (et_setting_s_2_s_m.text.toString() =="") "00" else et_setting_s_2_s_m.text.toString()
        var setting_s_2_e_h = if (et_setting_s_2_e_h.text.toString() =="") "00" else et_setting_s_2_e_h.text.toString()
        var setting_s_2_e_m = if (et_setting_s_2_e_m.text.toString() =="") "00" else et_setting_s_2_e_m.text.toString()
        var setting_s_3_s_h = if (et_setting_s_3_s_h.text.toString() =="") "00" else et_setting_s_3_s_h.text.toString()
        var setting_s_3_s_m = if (et_setting_s_3_s_m.text.toString() =="") "00" else et_setting_s_3_s_m.text.toString()
        var setting_s_3_e_h = if (et_setting_s_3_e_h.text.toString() =="") "00" else et_setting_s_3_e_h.text.toString()
        var setting_s_3_e_m = if (et_setting_s_3_e_m.text.toString() =="") "00" else et_setting_s_3_e_m.text.toString()

        var setting_p_1_s_h = if (et_setting_p_1_s_h.text.toString() =="") "00" else et_setting_p_1_s_h.text.toString()
        var setting_p_1_s_m = if (et_setting_p_1_s_m.text.toString() =="") "00" else et_setting_p_1_s_m.text.toString()
        var setting_p_1_e_h = if (et_setting_p_1_e_h.text.toString() =="") "00" else et_setting_p_1_e_h.text.toString()
        var setting_p_1_e_m = if (et_setting_p_1_e_m.text.toString() =="") "00" else et_setting_p_1_e_m.text.toString()
        var setting_p_2_s_h = if (et_setting_p_2_s_h.text.toString() =="") "00" else et_setting_p_2_s_h.text.toString()
        var setting_p_2_s_m = if (et_setting_p_2_s_m.text.toString() =="") "00" else et_setting_p_2_s_m.text.toString()
        var setting_p_2_e_h = if (et_setting_p_2_e_h.text.toString() =="") "00" else et_setting_p_2_e_h.text.toString()
        var setting_p_2_e_m = if (et_setting_p_2_e_m.text.toString() =="") "00" else et_setting_p_2_e_m.text.toString()
        var setting_p_3_s_h = if (et_setting_p_3_s_h.text.toString() =="") "00" else et_setting_p_3_s_h.text.toString()
        var setting_p_3_s_m = if (et_setting_p_3_s_m.text.toString() =="") "00" else et_setting_p_3_s_m.text.toString()
        var setting_p_3_e_h = if (et_setting_p_3_e_h.text.toString() =="") "00" else et_setting_p_3_e_h.text.toString()
        var setting_p_3_e_m = if (et_setting_p_3_e_m.text.toString() =="") "00" else et_setting_p_3_e_m.text.toString()

        var list = JSONArray()
        var shift1 = JSONObject()
        shift1.put("idx","1")
        shift1.put("date",now_time.toString("yyyy-MM-dd"))
        shift1.put("available_stime",""+setting_s_1_s_h+":"+setting_s_1_s_m)
        shift1.put("available_etime",""+setting_s_1_e_h+":"+setting_s_1_e_m)
        shift1.put("planned1_stime",""+setting_p_1_s_h+":"+setting_p_1_s_m)
        shift1.put("planned1_etime",""+setting_p_1_e_h+":"+setting_p_1_e_m)
        shift1.put("planned2_stime",""+setting_p_2_s_h+":"+setting_p_2_s_m)
        shift1.put("planned2_etime",""+setting_p_2_e_h+":"+setting_p_2_e_m)
        shift1.put("planned3_stime",""+setting_p_3_s_h+":"+setting_p_3_s_m)
        shift1.put("planned3_etime",""+setting_p_3_e_h+":"+setting_p_3_e_m)
        shift1.put("over_time","0")
        list.put(shift1)

        var shift2 = JSONObject()
        shift2.put("idx","2")
        shift2.put("date",now_time.toString("yyyy-MM-dd"))
        shift2.put("available_stime",""+setting_s_2_s_h+":"+setting_s_2_s_m)
        shift2.put("available_etime",""+setting_s_2_e_h+":"+setting_s_2_e_m)
        shift2.put("planned1_stime",""+setting_p_1_s_h+":"+setting_p_1_s_m)
        shift2.put("planned1_etime",""+setting_p_1_e_h+":"+setting_p_1_e_m)
        shift2.put("planned2_stime",""+setting_p_2_s_h+":"+setting_p_2_s_m)
        shift2.put("planned2_etime",""+setting_p_2_e_h+":"+setting_p_2_e_m)
        shift2.put("planned3_stime",""+setting_p_3_s_h+":"+setting_p_3_s_m)
        shift2.put("planned3_etime",""+setting_p_3_e_h+":"+setting_p_3_e_m)
        shift2.put("over_time","0")
        list.put(shift2)

        var shift3 = JSONObject()
        shift3.put("idx","3")
        shift3.put("date",now_time.toString("yyyy-MM-dd"))
        shift3.put("available_stime",""+setting_s_3_s_h+":"+setting_s_3_s_m)
        shift3.put("available_etime",""+setting_s_3_e_h+":"+setting_s_3_e_m)
        shift3.put("planned1_stime",""+setting_p_1_s_h+":"+setting_p_1_s_m)
        shift3.put("planned1_etime",""+setting_p_1_e_h+":"+setting_p_1_e_m)
        shift3.put("planned2_stime",""+setting_p_2_s_h+":"+setting_p_2_s_m)
        shift3.put("planned2_etime",""+setting_p_2_e_h+":"+setting_p_2_e_m)
        shift3.put("planned3_stime",""+setting_p_3_s_h+":"+setting_p_3_s_m)
        shift3.put("planned3_etime",""+setting_p_3_e_h+":"+setting_p_3_e_m)
        shift3.put("over_time","0")
        list.put(shift3)

        AppGlobal.instance.set_today_work_time_manual(list)

        shift1.put("date",yesterday.toString("yyyy-MM-dd"))
        shift2.put("date",yesterday.toString("yyyy-MM-dd"))
        shift3.put("date",yesterday.toString("yyyy-MM-dd"))

        AppGlobal.instance.set_prev_work_time_manual(list)

        var setting_db = DBHelperForSetting(this)

        val current_shift_time = AppGlobal.instance.get_current_shift_time_manual(1)
        val date = current_shift_time?.getString("date") ?: "2000-01-01"
        setting_db.deleteByDT(now_time.toString(date))

        setting_db.add(setting_s_1_s_h, setting_s_1_s_m, setting_s_1_e_h, setting_s_1_e_m,
                setting_s_2_s_h, setting_s_2_s_m, setting_s_2_e_h, setting_s_2_e_m,
                setting_s_3_s_h, setting_s_3_s_m, setting_s_3_e_h, setting_s_3_e_m,
                setting_p_1_s_h, setting_p_1_s_m, setting_p_1_e_h, setting_p_1_e_m,
                setting_p_2_s_h, setting_p_2_s_m, setting_p_2_e_h, setting_p_2_e_m,
                setting_p_3_s_h, setting_p_3_s_m, setting_p_3_e_h, setting_p_3_e_m,
                date)

        Log.e("test","list = " +  setting_db.gets().toString())
        finish()
    }

    private fun filterOperatorData() {
        _filtered_list_for_operator.removeAll(_filtered_list_for_operator)
        list_for_operator_adapter?.select(-1)

        val filter_text = et_setting_server_ip.text.toString()

        for (i in 0..(_list_for_operator.size-1)) {

            val item = _list_for_operator[i]
            val number = item["number"] ?: ""
            val name = item["name"] ?: ""

            val b = number.toUpperCase().contains(filter_text.toUpperCase())
            val c = name.toUpperCase().contains(filter_text.toUpperCase())
            if (filter_text=="" || b || c) {
                _filtered_list_for_operator.add(item)
            }
        }
        list_for_operator_adapter?.notifyDataSetChanged()
    }

    private fun fetchData() {
        val list = AppGlobal.instance.get_current_work_time()
        _list_json = list

        if (list == null) return

        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            var map = hashMapOf(
                    "idx" to item.getString("idx"),
                    "date" to item.getString("date"),
                    "work_stime" to item.getString("work_stime"),
                    "work_etime" to item.getString("work_etime"),
                    "available_stime" to item.getString("available_stime"),
                    "available_etime" to item.getString("available_etime"),
                    "planned1_stime" to item.getString("planned1_stime"),
                    "planned1_etime" to item.getString("planned1_etime"),
                    "planned2_stime" to item.getString("planned2_stime"),
                    "planned2_etime" to item.getString("planned2_etime"),
                    "planned3_stime" to item.getString("planned3_stime"),
                    "planned3_etime" to item.getString("planned3_etime"),
                    "over_time" to item.getString("over_time"),
                    "line_idx" to item.getString("line_idx"),
                    "line_name" to item.getString("line_name"),
                    "shift_idx" to item.getString("shift_idx"),
                    "shift_name" to item.getString("shift_name")
            )
            _list.add(map)
        }
        list_adapter?.notifyDataSetChanged()
    }

    private fun fetchOperatorData() {
        val uri = "/getlist1.php"
        var params = listOf(
                "code" to "worker",
                "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                "factory_idx" to AppGlobal.instance.get_room_idx())

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var list = result.getJSONArray("item")
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map = hashMapOf(
                            "idx" to item.getString("idx"),
                            "number" to item.getString("number"),
                            "name" to item.getString("name")
                    )
                    _list_for_operator.add(map)
                }
                filterOperatorData()
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
        filterOperatorData()
    }

    private fun initLastWorkers() {
        _list_for_last_worker.removeAll(_list_for_last_worker)
        var list = AppGlobal.instance.get_last_workers()

        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(list.length() - 1 - i)
            var worker = hashMapOf("number" to item.getString("number"), "name" to item.getString("name"))
            _list_for_last_worker.add(worker)
        }
        list_for_last_worker_adapter?.notifyDataSetChanged()
    }

    private class ListAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

        private var _list: ArrayList<HashMap<String, String>>
        private val _inflator: LayoutInflater
        private var _context : Context? =null

        init {
            this._inflator = LayoutInflater.from(context)
            this._list = list
            this._context = context
        }

        override fun getCount(): Int { return _list.size }
        override fun getItem(position: Int): Any { return _list[position] }
        override fun getItemId(position: Int): Long { return position.toLong() }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view: View?
            val vh: ViewHolder
            if (convertView == null) {
                view = this._inflator.inflate(R.layout.list_available_info, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            var work_stime = OEEUtil.parseDateTime(_list[position]["work_stime"].toString())
            var work_etime = OEEUtil.parseDateTime(_list[position]["work_etime"].toString())

            vh.tv_item_shift.text = _list[position]["shift_name"]
            vh.tv_item_work_time.text = work_stime.toString("HH:mm") + "~" + work_etime.toString("HH:mm")
            vh.tv_item_planned_time1.text = _list[position]["planned1_stime"] + "~" + _list[position]["planned1_etime"]
            vh.tv_item_planned_time2.text = _list[position]["planned2_stime"] + "~" + _list[position]["planned2_etime"]

            if ((_context as WorkInfoActivity)._selected_index==position) {
                vh.tv_item_shift.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_work_time.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_planned_time1.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_planned_time2.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
            } else {
                vh.tv_item_shift.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_work_time.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_planned_time1.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_planned_time2.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_shift: TextView
            val tv_item_work_time: TextView
            val tv_item_planned_time1: TextView
            val tv_item_planned_time2: TextView

            init {
                this.tv_item_shift = row?.findViewById<TextView>(R.id.tv_item_shift) as TextView
                this.tv_item_work_time = row?.findViewById<TextView>(R.id.tv_item_work_time) as TextView
                this.tv_item_planned_time1 = row?.findViewById<TextView>(R.id.tv_item_planned_time1) as TextView
                this.tv_item_planned_time2 = row?.findViewById<TextView>(R.id.tv_item_planned_time2) as TextView
            }
        }
    }

    private class ListOperatorAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

        private var _list: ArrayList<HashMap<String, String>>
        private val _inflator: LayoutInflater
        private var _context : Context? =null
        private var _selected_index = -1

        init {
            this._inflator = LayoutInflater.from(context)
            this._list = list
            this._context = context
        }

        fun select(index:Int) {_selected_index=index}
        fun getSelected(): Int { return _selected_index }

        override fun getCount(): Int { return _list.size }
        override fun getItem(position: Int): Any { return _list[position] }
        override fun getItemId(position: Int): Long { return position.toLong() }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view: View?
            val vh: ViewHolder
            if (convertView == null) {
                view = this._inflator.inflate(R.layout.list_operator_info, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_employee_number.text = _list[position]["number"]
            vh.tv_item_name.text = _list[position]["name"]

            if (_selected_index==position) {
                vh.tv_item_employee_number.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_name.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
            } else {
                vh.tv_item_employee_number.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_name.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_employee_number: TextView
            val tv_item_name: TextView

            init {
                this.tv_item_employee_number = row?.findViewById<TextView>(R.id.tv_item_employee_number) as TextView
                this.tv_item_name = row?.findViewById<TextView>(R.id.tv_item_name) as TextView
            }
        }
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
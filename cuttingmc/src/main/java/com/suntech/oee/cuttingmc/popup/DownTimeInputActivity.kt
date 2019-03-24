package com.suntech.oee.cuttingmc.popup

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.suntech.oee.cuttingmc.R
import com.suntech.oee.cuttingmc.base.BaseActivity
import com.suntech.oee.cuttingmc.common.AppGlobal
import com.suntech.oee.cuttingmc.db.DBHelperForDownTime
import com.suntech.oee.cuttingmc.db.SimpleDatabaseHelper
import com.suntech.oee.cuttingmc.util.OEEUtil
import kotlinx.android.synthetic.main.activity_down_time_input.*
import org.joda.time.DateTime
import java.util.*

class DownTimeInputActivity : BaseActivity() {

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _selected_idx =-1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_down_time_input)
        initView()
        updateList()
        fetchData()
    }

    private fun initView() {

        list_adapter = ListAdapter(this, _list)
        lv_downtimes.adapter = list_adapter

        lv_downtimes.setOnItemClickListener { adapterView, view, i, l ->
            _selected_idx = i
            _list.forEach { item ->
                if (i==_list.indexOf(item)) item.set("selected", "Y")
                else item.set("selected", "N")
            }
            list_adapter?.notifyDataSetChanged()
        }

        btn_confirm.setOnClickListener {
            sendEndDownTime()
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }

    private fun updateList () {
        _list.removeAll(_list);

        var list = AppGlobal.instance.get_downtime_list()
        for (i in 0..(list.length() - 1)) {

            val item = list.getJSONObject(i)
            var map=hashMapOf(
                    "idx" to item.getString("idx"),
                    "name" to item.getString("name"),
                    "color" to item.getString("color"),
                    "selected" to "N"
            )
            _list.add(map)
        }

        Collections.sort(_list, object : Comparator<HashMap<String, String>> {
            override fun compare(p0: HashMap<String, String>, p1: HashMap<String, String>): Int {
                return p0["idx"]!!.compareTo(p1["idx"]!!)
            }
        })

        list_adapter?.notifyDataSetChanged()
    }

    private fun sendEndDownTime() {

        if (AppGlobal.instance.get_server_ip()=="") {
            Toast.makeText(this, getString(R.string.msg_has_not_server_info), Toast.LENGTH_SHORT).show()
            return
        }
        if (_selected_idx<0) {
            Toast.makeText(this, getString(R.string.msg_has_notselected), Toast.LENGTH_SHORT).show()
            return
        }

        val downtime = _list[_selected_idx]["idx"]

        val uri = "/downtimedata.php"
        var params = listOf("code" to "end",
                "idx" to AppGlobal.instance.get_downtime_idx(),
                "downtime" to downtime,
                "edate" to DateTime().toString("yyyy-MM-dd"),
                "etime" to DateTime().toString("HH:mm:ss"))

        btn_confirm.isEnabled = false
        btn_cancel.isEnabled = false

        request(this, uri, true,true, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00") {

                val idx = AppGlobal.instance.get_downtime_idx()

                var db = DBHelperForDownTime(this)
                db.updateEnd(idx, _list[_selected_idx]["name"] ?: "")

                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                finish(true, 0, "ok", null)
            }else if(code == "99") {
                resendStartDownTime()
            }else {
                btn_confirm.isEnabled = true
                btn_cancel.isEnabled = true
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun resendStartDownTime() {
        if (AppGlobal.instance.get_server_ip()=="") return

        val work_idx = ""+AppGlobal.instance.get_product_idx()
        if (work_idx=="") return

        val idx = intent.getStringExtra("idx")
        var db = DBHelperForDownTime(this)
        val item = db.get(idx)
        if (item !=null) {

            val start_dt = item["start_dt"].toString()
            val didx = item["design_idx"].toString()
            val shift_idx = item["shift_id"].toString()
            val shift_name = item["shift_name"].toString()
            val dt = OEEUtil.parseDateTime(start_dt)
            db.delete(idx)

            var work_db = SimpleDatabaseHelper(this)
            val row = work_db.get(work_idx)
            val seq = row!!["seq"].toString().toInt()

            val uri = "/downtimedata.php"
            var params = listOf("code" to "start",
                    "mac_addr" to AppGlobal.instance.getMACAddress(),
                    "didx" to didx,
                    "sdate" to dt.toString("yyyy-MM-dd"),
                    "stime" to dt.toString("HH:mm:ss"),
                    "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
                    "factory_idx" to AppGlobal.instance.get_room_idx(),
                    "line_idx" to AppGlobal.instance.get_line_idx(),
                    "shift_idx" to shift_idx,
                    "seq" to seq)

            request(this, uri, true, false, params, { result ->

                var code = result.getString("code")
                var msg = result.getString("msg")
                if (code == "00") {

                    var idx = result.getString("idx")
                    AppGlobal.instance.set_downtime_idx(idx)
                    db.add(idx, work_idx, didx, shift_idx, shift_name, start_dt)

                    sendEndDownTime()
                } else {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun fetchData() {

        val uri = "/getlist1.php"
        var params = listOf("code" to "down_time",
                "factory_parent_idx" to AppGlobal.instance.get_factory_idx())

        request(this, uri, false, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){

                var list = result.getJSONArray("item")
                AppGlobal.instance.set_downtime_list(list)
                updateList()
            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    class ListAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

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
                view = this._inflator.inflate(R.layout.list_item_downtime_type, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_downtime_name.text = _list[position]["name"]
            vh.tv_item_downtime_name.setTextColor(Color.parseColor("#"+_list[position]["color"]))

            if (_list[position]["selected"]=="Y") vh.tv_item_downtime_check_box.isSelected = true
            else vh.tv_item_downtime_check_box.isSelected = false
            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_downtime_check_box: ImageView
            val tv_item_downtime_name: TextView

            init {
                this.tv_item_downtime_check_box = row?.findViewById<ImageView>(R.id.tv_item_downtime_check_box) as ImageView
                this.tv_item_downtime_name = row?.findViewById<TextView>(R.id.tv_item_downtime_name) as TextView
            }
        }
    }
}

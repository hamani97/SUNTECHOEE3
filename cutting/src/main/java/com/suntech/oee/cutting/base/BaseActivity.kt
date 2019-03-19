package com.suntech.oee.cutting.base

import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import cc.cloudist.acplibrary.ACProgressConstant
import cc.cloudist.acplibrary.ACProgressFlower
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.core.Json
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Handler
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.google.gson.JsonObject
import com.koushikdutta.ion.Ion
import com.suntech.oee.cutting.Constants
import com.suntech.oee.cutting.R
import com.suntech.oee.cutting.common.AppGlobal
import com.suntech.oee.cutting.util.UtilString
import org.json.JSONObject
import java.io.File
import java.util.*

open class BaseActivity : AppCompatActivity() {

    protected var _dialog: ACProgressFlower? = null
    private var _br_activity_callback_id = "br.base.activity.callback"
    protected var _callbackFunc: ((state: Boolean, code: Int, message: String, data: HashMap<String, String?>?) -> Unit )? = { r,c,m,d -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _br_activity_callback_id += UtilString.getRandomString(8)
        registerReceiver(_base_activity_callback, IntentFilter(_br_activity_callback_id))
    }
    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
        }
    }
    override fun onPause() {
        super.onPause()
    }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(_base_activity_callback)
    }


    // 프로그래스 다이얼로그 관련
    fun showProgressDialog(context: Context, text:String = "") {
        hideProgressDialog()
        _dialog = ACProgressFlower.Builder(context)
                .direction(ACProgressConstant.DIRECT_CLOCKWISE)
                .themeColor(Color.WHITE)
                .text(text)
                .fadeColor(Color.DKGRAY).build()
        _dialog?.show()
    }

    fun hideProgressDialog() {
        _dialog?.hide()
        _dialog?.dismiss()
    }


    // 액티비티 관련
    val _base_activity_callback = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val r = intent.getBooleanExtra("r", false)
            val c = intent.getIntExtra("c", 0)
            val m = intent.getStringExtra("m")
            val d = intent.getSerializableExtra("d") as HashMap<String, String?>?
            _callbackFunc?.invoke(r , c, m, d)
            _callbackFunc = null
        }
    }

    fun startActivity(intent: Intent, callbackFunc: (state: Boolean, code: Int, message: String, data: HashMap<String, String?>?)-> Unit) {
        this._callbackFunc = callbackFunc
        intent.putExtra("_br_activity_callback_id", _br_activity_callback_id)
        startActivity(intent)
    }

    protected fun finish(state: Boolean, code: Int, message: String, data: HashMap<String, String>?) {
        super.finish()
        val _br_activity_callback_id = intent.getStringExtra("_br_activity_callback_id")
        val intent = Intent(_br_activity_callback_id)
        intent.putExtra("r", state)
        intent.putExtra("c", code)
        intent.putExtra("m", message)
        intent.putExtra("d", data)
        sendBroadcast(intent)
    }
    override fun finish () {
        this.finish(true, 0, "", null)
    }


    // 네트워크 관련
    fun request (context: Context, uri:String, is_post:Boolean= false, is_log:Boolean = false, progress:Boolean= false,
                 params:List<Pair<String, Any?>>? = null,
                 callbackFunc: ((JSONObject)-> Unit)? = null,
                 failedCallbackFunc: (() -> Unit)? = null) {
        if (progress) showProgressDialog(context)

        val currentTimeMillisStart = System.currentTimeMillis()

        var full_url = "http://" + AppGlobal.instance.get_server_ip()
        val port = AppGlobal.instance.get_server_port()
        if (port!="") full_url += ":"+port

        // Log
        if (is_log) {
            Log.e("BaseActivity", "url = " + full_url + uri)
            if (params != null) Log.e("BaseActivity", "params = " + params.toString())
        }

        val obj = object : Handler<Json> {
            override fun success(request: Request, response: Response, value: Json) {

                if (progress) hideProgressDialog()

                val currentTimeMillisEnd = System.currentTimeMillis()
                val millis = currentTimeMillisEnd - currentTimeMillisStart
                try {
                    if (is_log) Log.d("BaseActivity", "response = " + value.obj().toString() + " , ms = "+ millis)

                    var r = value.obj().getString("code")
                    if(r == "00" || r == "99") callbackFunc?.invoke(value.obj())
                    else handle_network_error(context, "unknown error = " + uri)
                } catch (e:Exception) {
                    failedCallbackFunc?.invoke()
                    handle_network_error(context, "server parsing error = " + uri)
                }
            }
            override fun failure(request: Request, response: Response, error: FuelError) {
                failedCallbackFunc?.invoke()
                handle_network_error(context, error.toString())
            }
        }
        if (is_post)
            Fuel.post(full_url+uri, params).responseJson (obj)
        else
            Fuel.get(full_url+uri, params).responseJson (obj)
    }

    fun request (context: Context, uri:String, is_post:Boolean, is_log:Boolean, progress:Boolean, params:List<Pair<String, Any?>>?, callbackFunc: (JSONObject)-> Unit) {
        this.request(context, uri, is_post, is_log, progress, params, callbackFunc)
    }
    fun request (context: Context, uri:String, is_post:Boolean, progress:Boolean, params:List<Pair<String, Any?>>? = null, callbackFunc: (JSONObject)-> Unit) {
        this.request(context, uri, is_post, true, progress, params, callbackFunc)
    }
    fun request (context: Context, uri:String, is_post:Boolean, progress:Boolean, params:List<Pair<String, Any?>>? = null, callbackFunc: (JSONObject)-> Unit, failedCallbackFunc: (()-> Unit)? = null) {
        this.request(context, uri, is_post, true, progress, params, callbackFunc, failedCallbackFunc)
    }
    fun request (context: Context, uri:String, progress:Boolean, params:List<Pair<String, Any?>>? = null, callbackFunc: (JSONObject)-> Unit) {
        this.request(context, uri, false, true, progress, params, callbackFunc)
    }
    fun request (context: Context, uri:String, progress:Boolean, params:List<Pair<String, Any?>>? = null, callbackFunc: (JSONObject)-> Unit, failedCallbackFunc: (()-> Unit)? = null) {
        this.request(context, uri, false, true, progress, params, callbackFunc, failedCallbackFunc)
    }

    private fun handle_network_error (context: Context, error:String) {
        Log.e("BaseActivity", error)
        Toast.makeText(context, R.string.msg_connection_fail, Toast.LENGTH_SHORT).show()
        hideProgressDialog()
    }

    // 다이얼로그 관련
    fun showToast (msg:String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
    fun showConfirmDialog(title:String, message:String, callbackFunc: (Boolean)-> Unit) {
        AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.confirm), DialogInterface.OnClickListener { dialogInterface, i ->
                    callbackFunc?.invoke(true)
                })
                .setNegativeButton(getString(R.string.cancel), DialogInterface.OnClickListener { dialogInterface, i ->
                    callbackFunc?.invoke(false)
                }).show()
    }

    fun showAlertDialog(title:String, message:String, callbackFunc: ()-> Unit) {
        AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.confirm), DialogInterface.OnClickListener { dialogInterface, i ->
                    callbackFunc?.invoke()
                }).show()
    }

    fun getMediaPath(uri: Uri?): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = managedQuery(uri, projection, null, null, null)
        if (cursor != null) {
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(column_index)
        } else
            return null
    }

    fun uploadFile(uri:String, isCompress:Boolean, filePath:String, params:Map<String, List<String>>, callbackFunc: (JsonObject)-> Unit) {
        var actualImage = File(filePath)
        Log.d("BaseActivity", "uri = " + uri)
        Log.d("BaseActivity", "params = " + params.toString())
        Ion.with(this)
                .load(Constants.API_SERVER_URL + uri)
                .setMultipartParameters(params)
                .setMultipartFile("FILE", "image/jpg", actualImage)
                .asJsonObject()
                .setCallback({ error, result ->
                    if (result!= null) callbackFunc?.invoke(result)
                })
    }
}
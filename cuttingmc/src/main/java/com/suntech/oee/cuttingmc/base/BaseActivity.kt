package com.suntech.oee.cuttingmc.base

import android.content.Context
import android.graphics.Color
import android.os.Bundle
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
import com.suntech.oee.cuttingmc.R
import com.suntech.oee.cuttingmc.common.AppGlobal
import org.json.JSONObject

open class BaseActivity : AppCompatActivity() {

    protected var _dialog: ACProgressFlower? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onResume() {
        super.onResume()
    }
    override fun onPause() {
        super.onPause()
    }
    override fun onDestroy() {
        super.onDestroy()
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

    // 네트워크 관련
    fun request (context: Context, uri:String, is_post:Boolean= false, is_log:Boolean = false, progress:Boolean= false,
                 params:List<Pair<String, Any?>>? = null,
                 callbackFunc: ((JSONObject)-> Unit)? = null, failedCallbackFunc: (() -> Unit)? = null) {
        if (progress) showProgressDialog(context)

        var full_url = "http://" + AppGlobal.instance.get_server_ip()
        val port = AppGlobal.instance.get_server_port()
        if (port != "") full_url += ":" + port

        if (is_log) Log.d("BaseActivity", "url = " + full_url + uri)
        if (is_log && params!=null) Log.d("BaseActivity", "params = " + params.toString())
        val currentTimeMillisStart = System.currentTimeMillis()

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
}
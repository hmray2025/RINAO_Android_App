package dji.sampleV5.modulecommon

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dji.sampleV5.modulecommon.models.BaseMainActivityVm
import dji.sampleV5.modulecommon.models.MSDKInfoVm
import dji.sampleV5.modulecommon.util.IStreamManager
import dji.sampleV5.modulecommon.util.ITuskServiceCallback
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.datacenter.livestream.StreamQuality
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.PermissionUtil
import dji.v5.utils.common.StringUtils
import dji.v5.utils.common.ToastUtils
import kotlinx.android.synthetic.main.activity_main.*
/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/2/10
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
abstract class DJIMainActivity : AppCompatActivity(), ITuskServiceCallback, IStreamManager {

    val tag: String = LogUtils.getTag(this)
    private val permissionArray = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.KILL_BACKGROUND_PROCESSES,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    private var windowManager: WindowManager? = null
    private val baseMainActivityVm: BaseMainActivityVm by viewModels()
    protected val msdkInfoVm: MSDKInfoVm by viewModels()
    private val handler: Handler = Handler(Looper.getMainLooper())

    //    private val reconnectStream: Button = reconnect_ws
    abstract fun prepareUxActivity()

    abstract fun prepareTestingToolsActivity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("debug","Begin debug")
        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        // Set onClickListener for wsButton
        reconnect_ws.setOnClickListener {
            Log.d("TuskService", "Button Pressed?")
            setStatus(1, serverStatus)
            callReconnectWebsocket()
            if (callGetConnectionStatus()) {
                setStatus(1, serverStatus)
            }
            else {
                setStatus(-1, serverStatus)
            }
        }

        reconnect_ws_settings.setOnClickListener {
            Log.d("TuskService", "Button Pressed?")
            setStatus(1, serverStatus)
            callReconnectWebsocket()
            if (callGetConnectionStatus()) {
                setStatus(1, serverStatus)
                Log.d("TuskService", "updated status to good")
            }
            else {
                setStatus(-1, serverStatus)
                Log.d("TuskService", "updated status to error")
            }
        }

        HD1080.setOnClickListener {
            setStreamQuality(0)
            setStreamSelection()
        }
        HD720.setOnClickListener {
            setStreamQuality(1)
            setStreamSelection()
        }
        SD540.setOnClickListener {
            setStreamQuality(2)
            setStreamSelection()
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // not needed, keep here
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                setbitrate.isEnabled = p0.toString().isNotEmpty()
            }

            override fun afterTextChanged(p0: Editable?) {
                // not needed, keep here
            }
        })

        setbitrate.setOnClickListener {
            setBitrate(editText.text.toString().toInt())
            editText.text.clear()
        }

        editTextIP.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // not needed, keep here
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                setIP.isEnabled = p0.toString().isNotEmpty()
            }

            override fun afterTextChanged(p0: Editable?) {
                // not needed, keep here
            }
        })

        setIP.setOnClickListener {
            callSetIP(editTextIP.text.toString())
            editTextIP.text.clear()
        }


        // Show the settings dialog when the settingsButton is clicked
        settings_button.setOnClickListener {
//            bottomSheetDialog.show()
            settings_panel.visibility = View.VISIBLE
        }

        close_button.setOnClickListener {
            settings_panel.visibility = View.INVISIBLE
        }
        initMSDKInfoView()
        checkPermissionAndRequest()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkPermission()) {
            handleAfterPermissionPermitted()
        }
    }

    override fun onResume() {
        super.onResume()
//        addSettingsPageOverlay()
        if (checkPermission()) {
            handleAfterPermissionPermitted()
            setStreamSelection()
        }
    }

    private fun handleAfterPermissionPermitted() {
        registerApp()
        prepareTestingToolsActivity()
        startStatusCheck()
    }

    @SuppressLint("SetTextI18n")
    private fun initMSDKInfoView() {
        ToastUtils.init(this)
        msdkInfoVm.msdkInfo.observe(this) {
            text_view_version.text = StringUtils.getResStr(R.string.sdk_version, it.SDKVersion + " " + it.buildVer)
            text_view_product_name.text = StringUtils.getResStr(R.string.product_name, it.productType.name)
            text_view_package_product_category.text = StringUtils.getResStr(R.string.package_product_category, it.packageProductCategory)
            text_view_is_debug.text = StringUtils.getResStr(R.string.is_sdk_debug, it.isDebug)
            text_core_info.text = it.coreInfo.toString()
        }
        baseMainActivityVm.registerState.observe(this) {
            text_view_registered.text = StringUtils.getResStr(R.string.registration_status, it)
        }
//        baseMainActivityVm.sdkNews.observe(this) {
//            item_news_msdk.setTitle(StringUtils.getResStr(it.title))
//            item_news_msdk.setDescription(StringUtils.getResStr(it.description))
//            item_news_msdk.setDate(it.date)
//
//            item_news_uxsdk.setTitle(StringUtils.getResStr(it.title))
//            item_news_uxsdk.setDescription(StringUtils.getResStr(it.description))
//            item_news_uxsdk.setDate(it.date)
//        }
//
//        icon_sdk_forum.setOnClickListener {
//            Helper.startBrowser(this, StringUtils.getResStr(R.string.sdk_forum_url))
//        }
//        icon_release_node.setOnClickListener {
//            Helper.startBrowser(this, StringUtils.getResStr(R.string.release_node_url))
//        }
//        icon_tech_support.setOnClickListener {
//            Helper.startBrowser(this, StringUtils.getResStr(R.string.tech_support_url))
//        }
//        view_base_info.setOnClickListener {
            baseMainActivityVm.doPairing {
                ToastUtils.showToast(it)
            }
//        }
    }

    private fun registerApp() {
        baseMainActivityVm.registerApp(this, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                ToastUtils.showToast("Register Success")
                msdkInfoVm.initListener()
                handler.postDelayed({
                    setStatus(1, droneStatus)
                    prepareUxActivity()
                }, 5000)
            }

            override fun onRegisterFailure(error: IDJIError?) {
                ToastUtils.showToast("Register Failure: (errorCode: ${error?.errorCode()}, description: ${error?.description()})")
                setStatus(-1, droneStatus)
            }

            override fun onProductDisconnect(product: Int) {
                ToastUtils.showToast("Product: $product Disconnect")
                setStatus(-1, droneStatus)
            }

            override fun onProductConnect(product: Int) {
                ToastUtils.showToast("Product: $product Connect")
            }

            override fun onProductChanged(product: Int) {
                ToastUtils.showToast("Product: $product Changed")
            }

            override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {
                ToastUtils.showToast("Init Process event: ${event?.name}")
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                ToastUtils.showToast("Database Download Progress current: $current, total: $total")
            }
        })
    }


    fun <T> enableDefaultLayout(cl: Class<T>) {
        enableShowCaseButton(default_layout_button, cl)
    }

    fun <T> enableWidgetList(cl: Class<T>) {
        enableShowCaseButton(widget_list_button, cl)
    }

    fun <T> enableTestingTools(cl: Class<T>) {
        enableShowCaseButton(testing_tool_button, cl)
    }

//    fun <T> enableSettings(cl: Class<T>) {
//        enableShowCaseButton(settingsButton, cl)
//    }

//    fun <T> enableLiveStreamShortcut(cl: Class<T>){
//        enableShowCaseButton(live_stream_shortcut, cl)
//    }

    private fun <T> enableShowCaseButton(view: View, cl: Class<T>) {
        view.isEnabled = true
        view.setOnClickListener {
            Intent(this, cl).also {
                startActivity(it)
            }
        }
    }

    private fun checkPermissionAndRequest() {
        for (i in permissionArray.indices) {
            if (!PermissionUtil.isPermissionGranted(this, permissionArray[i])) {
                requestPermission()
                break
            }
        }
    }

    private fun checkPermission(): Boolean {
        for (i in permissionArray.indices) {
            if (PermissionUtil.isPermissionGranted(this, permissionArray[i])) {
                return true
            }
        }
        return false
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            result?.entries?.forEach {
                if (it.value == false) {
                    requestPermission()
                    return@forEach
                }
            }
        }

    private fun requestPermission() {
        requestPermissionLauncher.launch(permissionArray)
    }

    override fun onDestroy() {
        removeSettingsPageOverlay()
        stopStatusCheck()
        super.onDestroy()
        baseMainActivityVm.releaseSDKCallback()
        ToastUtils.destroy()
    }

    override fun onPause() {
        super.onPause()
//        stopStatusCheck() // Stop the status checking coroutine when the activity is paused
        removeSettingsPageOverlay()
    }

    private fun startStatusCheck() {
        handler.postDelayed(statusCheckRunnable, 1000.toLong())
        Log.d("TuskService", "started status check")
    }

    private fun stopStatusCheck() {
        handler.removeCallbacks(statusCheckRunnable) // Remove the scheduled status check callbacks when the activity is paused
        Log.d("TuskService", "stopped status check")
    }

    private val statusCheckRunnable = object : Runnable {
        override fun run() {
            if (callGetConnectionStatus()) {
                setStatus(1, serverStatus)
            }
            else {
                setStatus(-1, serverStatus)
            }
            streambitrate.text = "Stream Bitrate: ${getBitrate()}"
            streamquality.text = "Stream Quality: ${getStreamQuality()}"
            streamurl.text = "Stream URL: ${getStreamURL()}"
            currentlystreaming.text = "Currently Streaming: ${isStreaming()}"
            serverconnected.text = "Connection Status: ${callGetConnectionStatus()}"
            serverip.text = "Server IP: ${callGetIP()}"
            // Schedule the next status check after the interval
            handler.postDelayed(this, 1000.toLong())
        }
    }

    fun setStatus(status: Int, indicator: ImageView) {
        var dots: ImageView? = null
        if (indicator == droneStatus)
            dots = imageDot2
        else
            dots = imageDot1
        when (status) {
            1 -> {
                dots.visibility = View.INVISIBLE
                indicator.setBackgroundResource(R.drawable.uxsdk_ic_alert_good)
                indicator.visibility = View.VISIBLE
            }

            -1 -> {
                dots.visibility = View.INVISIBLE
                indicator.setBackgroundResource(R.drawable.uxsdk_ic_alert_error)
                indicator.visibility = View.VISIBLE
            }

            0 -> {
                dots.visibility = View.VISIBLE
                indicator.visibility = View.INVISIBLE
            }
        }
    }

    private fun addSettingsPageOverlay() {
        // Initialize WindowManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Create layout params for the settings page overlay
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSPARENT
        )

        // Set position of the settings page overlay
        params.gravity = Gravity.CENTER
        // Add the settings page overlay to the WindowManager
        windowManager!!.addView(settings_panel, params)
    }

    private fun removeSettingsPageOverlay() {
        // Remove the settings page overlay from the WindowManager
        windowManager?.removeView(settings_panel)
        windowManager = null
    }

    private fun setStreamSelection() {
        val buttons: List<Button> = listOf(HD1080, HD720, SD540)
        var selectionIndex = -1

        when (getStreamQuality()) {
            StreamQuality.FULL_HD -> {
                selectionIndex = 0
            }
            StreamQuality.HD -> {
                selectionIndex = 1
            }
            StreamQuality.SD -> {
                selectionIndex = 2
            }
        }

        for ((index, button) in buttons.withIndex()) {
            if (index == selectionIndex) {
                button.setBackgroundResource(R.drawable.rounded_selected)
            } else {
                button.setBackgroundResource(R.drawable.rounded_white_bg)
            }
        }
    }
}
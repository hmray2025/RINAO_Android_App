package dji.sampleV5.aircraft

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dji.sampleV5.aircraft.databinding.ActivityMainBinding
import dji.sampleV5.aircraft.models.BaseMainActivityVm
import dji.sampleV5.aircraft.models.MSDKInfoVm
import dji.sampleV5.aircraft.models.MSDKManagerVM
import dji.sampleV5.aircraft.models.globalViewModels
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sampleV5.aircraft.util.IStreamManager
import dji.sampleV5.modulecommon.settingswidgets.ISartopoWidgetModel
import dji.sampleV5.modulecommon.util.ITuskServiceCallback
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.PermissionUtil
import dji.v5.utils.common.StringUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable



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
    private val permissionArray = arrayListOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.KILL_BACKGROUND_PROCESSES,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    init {
        permissionArray.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private val baseMainActivityVm: BaseMainActivityVm by viewModels()
    private val msdkInfoVm: MSDKInfoVm by viewModels()
    private val msdkManagerVM: MSDKManagerVM by globalViewModels()
    private lateinit var binding: ActivityMainBinding
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val disposable = CompositeDisposable()

    abstract fun prepareUxActivity()

    abstract fun getSartopoWidgetModel(): ISartopoWidgetModel

    abstract fun getStreamModel(): IStreamManager

    abstract fun getTuskModel(): ITuskServiceCallback

    abstract fun prepareTestingToolsActivity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 有一些手机从系统桌面进入的时候可能会重启main类型的activity
        // 需要校验这种情况，业界标准做法，基本所有app都需要这个
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN == intent.action) {

            finish()
            return

        }

        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        binding.scrollViewSettings.setOnScrollChangeListener { v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->

            // Assuming you have a method to calculate the section based on scrollY

            val currentSection = calculateCurrentSection(scrollY)

            highlightSideListItem(currentSection)

        }.also { binding.serverScrollspy.setTypeface(null, Typeface.BOLD) }

        initOnClickListeners()
        initMSDKInfoView()
        observeSDKManager()
        checkPermissionAndRequest()
    }

    private fun initOnClickListeners() {
        // Set onClickListener for wsButton
        binding.reconnectWs.setOnClickListener {
            setStatus(1, binding.serverStatus)
            callReconnectWebsocket()
            if (callGetConnectionStatus()) {
                setStatus(1, binding.serverStatus)
            } else {
                setStatus(-1, binding.serverStatus)
            }
        }

        // Show the settings dialog when the settingsButton is clicked
        binding.settingsButton.setOnClickListener {
//            bottomSheetDialog.show()
            binding.settingsPanel.visibility = View.VISIBLE
        }

        binding.settingsPanel.setOnClickListener {
            // do nothing, leave here so that touch events are absorbed
        }

        binding.closeButton.setOnClickListener {
            binding.settingsPanel.visibility = View.INVISIBLE
        }

        binding.repairButton.setOnClickListener {
            baseMainActivityVm.doPairing {
                ToastUtils.showToast(it)
            }
        }

        // Set onClickListener for the scrollspy widgets
        binding.serverScrollspy.setOnClickListener {
            moveToClickedPortion(0)
        }
        binding.sartopoScrollspy.setOnClickListener {
            moveToClickedPortion(1)
        }
        binding.livestreamScrollspy.setOnClickListener {
            moveToClickedPortion(2)
        }
        binding.quickactionsScrollspy.setOnClickListener {
            moveToClickedPortion(3)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkPermission()) {
            handleAfterPermissionPermitted()
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkPermission()) {
            handleAfterPermissionPermitted()
        }
    }

    private fun handleAfterPermissionPermitted() {
//        registerApp()
        prepareTestingToolsActivity()
        startStatusCheck()
    }

    @SuppressLint("SetTextI18n")
    private fun initMSDKInfoView() {
        msdkInfoVm.msdkInfo.observe(this) {
            binding.textViewVersion.text =
                StringUtils.getResStr(R.string.sdk_version, it.SDKVersion + " " + it.buildVer)
            binding.textViewProductName.text =
                StringUtils.getResStr(R.string.product_name, it.productType.name)
            binding.textViewPackageProductCategory.text =
                StringUtils.getResStr(R.string.package_product_category, it.packageProductCategory)
            binding.textViewIsDebug.text = StringUtils.getResStr(R.string.is_sdk_debug, it.isDebug)
            binding.textCoreInfo.text = it.coreInfo.toString()
        }

//        binding.iconSdkForum.setOnClickListener {
//            Helper.startBrowser(this, StringUtils.getResStr(R.string.sdk_forum_url))
//        }
//
//        binding.iconReleaseNode.setOnClickListener {
//            Helper.startBrowser(this, StringUtils.getResStr(R.string.release_node_url))
//        }
//        binding.iconTechSupport.setOnClickListener {
//            Helper.startBrowser(this, StringUtils.getResStr(R.string.tech_support_url))
//        }
        binding.viewBaseInfo.setOnClickListener {
            baseMainActivityVm.doPairing {
                showToast(it)
            }
        }
    }

    private fun observeSDKManager() {
        msdkManagerVM.lvRegisterState.observe(this) { resultPair ->
            val statusText: String?
            if (resultPair.first) {
                ToastUtils.showToast("Register Success")
                statusText = StringUtils.getResStr(this, R.string.registered)
                msdkInfoVm.initListener()
                handler.postDelayed({
                    prepareUxActivity()
                    binding.sartopoWidget.setSartopoWidgetModel(getSartopoWidgetModel())
                    binding.livestreamWidget.setStreamManager(getStreamModel())
                    binding.serverWidget.setServerManager(getTuskModel())
                    binding.sartopoWidget.loadDefaults()
                }, 5000)
            } else {
                showToast("Register Failure: ${resultPair.second}")
                statusText = StringUtils.getResStr(this, R.string.unregistered)
            }
            binding.textViewRegistered.text =
                StringUtils.getResStr(R.string.registration_status, statusText)
        }

        msdkManagerVM.lvProductConnectionState.observe(this) { resultPair ->
            showToast("Product: ${resultPair.second} ,ConnectionState:  ${resultPair.first}")
        }

        msdkManagerVM.lvProductChanges.observe(this) { productId ->
            showToast("Product: $productId Changed")
        }

        msdkManagerVM.lvInitProcess.observe(this) { processPair ->
            showToast("Init Process event: ${processPair.first.name}")
        }

        msdkManagerVM.lvDBDownloadProgress.observe(this) { resultPair ->
            showToast("Database Download Progress current: ${resultPair.first}, total: ${resultPair.second}")
        }
    }

    private fun showToast(content: String) {
        ToastUtils.showToast(content)

    }


    fun <T> enableDefaultLayout(cl: Class<T>) {
        enableShowCaseButton(binding.defaultLayoutButton, cl)
    }

    fun <T> enableWidgetList(cl: Class<T>) {
        enableShowCaseButton(binding.widgetListButton, cl)
    }

    fun <T> enableTestingTools(cl: Class<T>) {
        enableShowCaseButton(binding.testingToolButton, cl)
    }

    private fun <T> enableShowCaseButton(view: View, cl: Class<T>) {
        view.isEnabled = true
        view.setOnClickListener {
            Intent(this, cl).also {
                startActivity(it)
            }
        }
    }

    private fun checkPermissionAndRequest() {
        if (!checkPermission()) {
            requestPermission()
        }
    }

    private fun checkPermission(): Boolean {
        for (i in permissionArray.indices) {
            if (!PermissionUtil.isPermissionGranted(this, permissionArray[i])) {
                return false
            }
        }
        return true
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        result?.entries?.forEach {
            if (!it.value) {
                requestPermission()
                return@forEach
            }
        }
    }

    private fun requestPermission() {
        requestPermissionLauncher.launch(permissionArray.toArray(arrayOf()))
    }

    override fun onDestroy() {
        stopStatusCheck()
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        disposable.dispose()
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
//            Log.d("TuskService", "Status Panel Height: ${status_panel.height}")
            if (callGetConnectionStatus()) {
                setStatus(1, binding.serverStatus)
            } else {
                setStatus(-1, binding.serverStatus)
            }
//            serverconnected.text = "Connection Status: ${callGetConnectionStatus()}"
//            serverip.text = "Server IP: ${callGetIP()}"
            // Schedule the next status check after the interval
            handler.postDelayed(this, 1000.toLong())
            if (binding.livestreamWidget.isInterfaceBinded()) binding.livestreamWidget.setStreamSelection()
            if (binding.serverWidget.isInterfaceBinded()) binding.serverWidget.updateData()
        }
    }

    fun setStatus(status: Int, indicator: ImageView) {
        var dots: ImageView? = null
        if (indicator == binding.droneStatus)
            dots = binding.imageDot2
        else
            dots = binding.imageDot1
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

    // Method to calculate the current section based on scrollY
    private fun calculateCurrentSection(scrollY: Int): Int {
        val h0 = binding.serverWidget.height
        val h1 = binding.sartopoWidget.height
        val h2 = binding.livestreamWidget.height
        val h3 = binding.quickactionsWidget.height

        val cumulativeHeight1 =
            h0 - 300 // Adjusted such that the top of the section is not at the top of the screen
        val cumulativeHeight2 = cumulativeHeight1 + h1
        val cumulativeHeight3 = cumulativeHeight2 + h2

        return when (scrollY) {
            in 0..cumulativeHeight1 -> 0
            in (cumulativeHeight1 + 1)..cumulativeHeight2 -> 1
            in (cumulativeHeight2 + 1)..cumulativeHeight3 -> 2
            in (cumulativeHeight3 + 1)..(cumulativeHeight3 + h3) -> 3
            else -> -1 // Consider adding a default case to handle unexpected values
        }
    }

    private fun moveToClickedPortion(sectionIndex: Int) {
        val h0 = binding.serverWidget.height
        val h1 = binding.sartopoWidget.height
        val h2 = binding.livestreamWidget.height
        val h3 = binding.quickactionsWidget.height

        val cumulativeHeight1 = h0
        val cumulativeHeight2 = cumulativeHeight1 + h1
        val cumulativeHeight3 = cumulativeHeight2 + h2

        val scrollToY = when (sectionIndex) {
            0 -> 0
            1 -> cumulativeHeight1
            2 -> cumulativeHeight2
            3 -> cumulativeHeight3
            else -> 0
        }
        binding.scrollViewSettings.smoothScrollTo(0, scrollToY)
    }

    // Method to highlight the side list item
    private fun highlightSideListItem(sectionIndex: Int) {
        val setOfScrollspyWidgets: Set<TextView> =
            setOf(binding.serverScrollspy, binding.sartopoScrollspy, binding.livestreamScrollspy, binding.quickactionsScrollspy)
        if (sectionIndex == -1) return
        for (widget in setOfScrollspyWidgets) {
            widget.setTypeface(
                null, if (widget == setOfScrollspyWidgets.elementAt(sectionIndex)) {
                    Typeface.BOLD
                } else {
                    Typeface.NORMAL
                }
            )
        }
    }
}
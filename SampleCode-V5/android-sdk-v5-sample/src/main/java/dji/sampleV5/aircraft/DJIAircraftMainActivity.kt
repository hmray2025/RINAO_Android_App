package dji.sampleV5.aircraft

import android.util.Log
import android.widget.Toast
import dji.sampleV5.aircraft.control.PachKeyManager
import dji.sampleV5.aircraft.fpvlayout.FPVLayoutActivity
import dji.sampleV5.aircraft.telemetry.SartopoService
//import dji.sampleV5.modulecommon.DJIMainActivity
import dji.v5.common.utils.GeoidManager
import dji.v5.manager.datacenter.livestream.StreamQuality
import dji.v5.ux.core.communication.DefaultGlobalPreferences
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.util.UxSharedPreferencesUtil

import dji.v5.ux.sample.showcase.widgetlist.WidgetsActivity
import dji.sampleV5.modulecommon.settingswidgets.ISartopoWidgetModel
import dji.sampleV5.aircraft.util.IStreamManager
import dji.sampleV5.modulecommon.util.ITuskServiceCallback

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/2/14
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class DJIAircraftMainActivity : DJIMainActivity() {
    private val tuskManager = PachKeyManager.getInstance()
    private val sartopoService: SartopoService = SartopoService.getInstance()
    override fun prepareUxActivity() {
        UxSharedPreferencesUtil.initialize(this)
        GlobalPreferencesManager.initialize(DefaultGlobalPreferences(this))
        GeoidManager.getInstance().init(this)

        enableDefaultLayout(FPVLayoutActivity::class.java) // important
        enableWidgetList(WidgetsActivity::class.java)
        this.tuskManager.runTesting()
        this.tuskManager.updateStatusWidget()
//        prepareConfigurationTools()
//


    }

    override fun getSartopoWidgetModel(): ISartopoWidgetModel {
        return sartopoService
    }

    override fun getStreamModel(): IStreamManager {
        return tuskManager.streamer
    }

    override fun getTuskModel(): ITuskServiceCallback {
        return tuskManager.telemService
    }

    override fun prepareTestingToolsActivity() {
        enableTestingTools(AircraftTestingToolsActivity::class.java)
    }


    override fun callReconnectWebsocket() {
        Log.d("TuskService", "Callback called successfully")
        Toast.makeText(applicationContext, "Reconnecting WS with IP:" + "\n " +
                this.tuskManager.telemService.getCurrentIP(),
            Toast.LENGTH_LONG).show()
        this.tuskManager.telemService.connectWebSocket()
    }

    override fun callSetIP(ip: String) {
        Log.d("TuskService", "Callback callSetIP() called with value $ip")
        this.tuskManager.telemService.setCurrentIP(ip)
    }

    override fun callGetIP(): String {
        return this.tuskManager.telemService.getCurrentIP()
    }

    override fun callGetConnectionStatus(): Boolean {
        return this.tuskManager.telemService.getConnectionStatus()
    }

    override fun setBitrate(rate: Int) {
        this.tuskManager.streamer.setBitrate(rate)
    }

    override fun setStreamQuality(choice: StreamQuality) {
        this.tuskManager.streamer.setStreamQuality(choice)
    }

    override fun getBitrate(): Int {
        return this.tuskManager.streamer.getBitrate()
    }

    override fun getStreamQuality(): StreamQuality {
        return this.tuskManager.streamer.getStreamQuality()
    }

    override fun getStreamURL(): String {
        return this.tuskManager.streamer.getStreamURL()
    }

    override fun isStreaming(): Boolean {
        return this.tuskManager.streamer.isStreaming()
    }

    override fun startStream() {
        this.tuskManager.streamer.startStream()
    }

}


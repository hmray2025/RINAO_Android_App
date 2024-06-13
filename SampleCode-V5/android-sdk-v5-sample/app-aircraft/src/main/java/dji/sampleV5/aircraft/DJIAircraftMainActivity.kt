package dji.sampleV5.aircraft

import android.util.Log
import android.widget.Toast
import dji.sampleV5.aircraft.control.PachKeyManager
import dji.sampleV5.aircraft.defaultlayout.DefaultLayoutActivity
import dji.sampleV5.modulecommon.DJIMainActivity
import dji.v5.common.utils.GeoidManager
import dji.v5.manager.datacenter.livestream.StreamQuality
import dji.v5.ux.core.communication.DefaultGlobalPreferences
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.util.UxSharedPreferencesUtil
import dji.v5.ux.sample.showcase.widgetlist.WidgetsActivity

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/2/14
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class DJIAircraftMainActivity : DJIMainActivity() {
    val TuskManger = PachKeyManager()
    override fun prepareUxActivity() {
        UxSharedPreferencesUtil.initialize(this)
        GlobalPreferencesManager.initialize(DefaultGlobalPreferences(this))
        GeoidManager.getInstance().init(this)

        if (TuskManger != null) {
            enableDefaultLayout(DefaultLayoutActivity::class.java) // important
        }
        else {
            enableDefaultLayout(DefaultLayoutActivity::class.java) // important
        }

        enableWidgetList(WidgetsActivity::class.java)
        this.TuskManger.runTesting()
        this.TuskManger.checkForWaypoints()
//        prepareConfigurationTools()
//


    }

    override fun prepareTestingToolsActivity() {
        enableTestingTools(AircraftTestingToolsActivity::class.java)
    }

//    override fun loadSettingsActivity() {
//        enableSettings(SettingsActivity::class.java)
//    }

//    override fun loadWidgetActivity() {
//        enableWidgetList(WidgetsActivity::class.java)
//    }

    override fun callReconnectWebsocket() {
        Log.d("TuskService", "Callback called successfully")
        Toast.makeText(applicationContext, "Reconnecting WS with IP:" + "\n " +
                "${this.TuskManger.telemService.getCurrentIP()}",
            Toast.LENGTH_LONG).show()
        this.TuskManger.telemService.connectWebSocket()
    }

    override fun callSetIP(ip: String) {
        Log.d("TuskService", "Callback callSetIP() called with value $ip")
        this.TuskManger.telemService.setCurrentIP(ip)
    }

    override fun callGetIP(): String {
        return this.TuskManger.telemService.getCurrentIP()
    }

    override fun callGetConnectionStatus(): Boolean {
        return this.TuskManger.telemService.getConnectionStatus()
    }

    override fun setBitrate(rate: Int) {
        this.TuskManger.streamer.setBitrate(rate)
    }

    override fun setStreamQuality(choice: Int) {
        this.TuskManger.streamer.setStreamQuality(choice)
    }

    override fun getBitrate(): Int {
        return this.TuskManger.streamer.getBitrate()
    }

    override fun getStreamQuality(): StreamQuality {
        return this.TuskManger.streamer.getStreamQuality()
    }

    override fun getStreamURL(): String {
        return this.TuskManger.streamer.getStreamURL()
    }

    override fun isStreaming(): Boolean {
        return this.TuskManger.streamer.isStreaming()
    }

    override fun startStream() {
        this.TuskManger.streamer.startStream()
    }


//    fun prepareConfigurationTools(){
//        enableLiveStreamShortcut(LiveStreamFragment::class.java)
//    }
}


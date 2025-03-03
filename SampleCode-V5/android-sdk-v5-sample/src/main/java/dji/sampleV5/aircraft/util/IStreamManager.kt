package dji.sampleV5.aircraft.util

import dji.v5.manager.datacenter.livestream.StreamQuality

interface IStreamManager {
    fun setBitrate(rate: Int)
    fun setStreamQuality(choice: StreamQuality)
    fun getBitrate(): Int
    fun getStreamQuality(): StreamQuality
    fun getStreamURL(): String
    fun isStreaming(): Boolean
    fun startStream()
}
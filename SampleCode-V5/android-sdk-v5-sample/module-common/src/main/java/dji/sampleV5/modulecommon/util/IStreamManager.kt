package dji.sampleV5.modulecommon.util

import dji.v5.manager.datacenter.livestream.StreamQuality

interface IStreamManager {
    fun setBitrate(rate: Int)
    fun setStreamQuality(choice: Int)
    fun getBitrate(): Int
    fun getStreamQuality(): StreamQuality
    fun getStreamURL(): String
    fun isStreaming(): Boolean
    fun startStream()
}
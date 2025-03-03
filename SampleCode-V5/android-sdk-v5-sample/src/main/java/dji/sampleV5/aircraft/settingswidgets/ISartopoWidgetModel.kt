package dji.sampleV5.modulecommon.settingswidgets

interface ISartopoWidgetModel {
    fun getAccessURL(): String
    fun getDeviceID(): String
    fun getBaseURL(): String
    fun setAccessURL(access: String)
    fun setDeviceID(ID: String)
    fun setBaseURL(base: String)
}
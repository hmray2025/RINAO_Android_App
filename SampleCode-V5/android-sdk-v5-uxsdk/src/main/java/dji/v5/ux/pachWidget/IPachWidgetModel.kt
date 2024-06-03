package dji.v5.ux.pachWidget

interface IPachWidgetModel {
    fun getConnectionStatus(): Boolean
    fun getFailedSafetyCheck(): Array<Int>
    fun getAction(): String
}
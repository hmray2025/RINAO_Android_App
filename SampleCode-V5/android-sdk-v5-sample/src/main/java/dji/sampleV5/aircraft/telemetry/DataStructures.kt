package dji.sampleV5.aircraft.telemetry

import com.google.gson.annotations.SerializedName


// Classes for storing all data.
// Aircraft state is anything related to the physical aircraft's position, velocity, and attitude
data class TuskAircraftState(
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("altitude") val altitude: Double?,
    @SerializedName("roll") val roll: Double?,
    @SerializedName("pitch") val pitch: Double?,
    @SerializedName("yaw") val yaw: Double?,
    @SerializedName("velocityX") val velocityX: Double?,
    @SerializedName("velocityY") val velocityY: Double?,
    @SerializedName("velocityZ") val velocityZ: Double?,
    @SerializedName("windSpeed") val windSpeed: Int?,
    @SerializedName("windDirection") val windDirection: String?,
    @SerializedName("isFlying") val isFlying: Boolean?,
)

// Aircraft Status is anything related to the aircraft's hardware and connectivity
data class TuskAircraftStatus(
    @SerializedName("connected") val connected: Boolean?,
    @SerializedName("battery") val battery: Int?,
    @SerializedName("gps") val gps: Int?,
    @SerializedName("gpsSignal") val gpsSignal: Int?,
    @SerializedName("signalQuality") val signalQuality: Int?,
    @SerializedName("goHomeState") val goHomeState: String?,
//    https://developer.dji.com/api-reference-v5/android-api/Components/IKeyManager/DJIValue.html#value_flightcontroller_enum_gohomestate_inline
    @SerializedName("flightMode") val flightMode: String?,
//    https://developer.dji.com/api-reference-v5/android-api/Components/IKeyManager/DJIValue.html#value_flightcontroller_enum_flightmode_inline
    @SerializedName("motorsOn") val motorsOn: Boolean?,
    @SerializedName("homeLocationLat") val homeLocationLat: Double?,
    @SerializedName("homeLocationLong") val homeLocationLong: Double?,
    @SerializedName("gimbalAngle") val gimbalAngle: Double?,
    @SerializedName("goHomeStatus") val goHomeStatus: String?,
    @SerializedName("takeoffAltitude") val takeoffAltitude: Double?,
    @SerializedName("isStreaming") val isStreaming: Boolean?,
)

data class TuskControllerStatus(
    @SerializedName("battery") val battery: Int?,
    @SerializedName("pauseButton") val pauseButton: Boolean?,
    @SerializedName("goHomeButton") val goHomeButton: Boolean?,
    @SerializedName("leftStickX") val leftStickX: Int?,
    @SerializedName("leftStickY") val leftStickY: Int?,
    @SerializedName("rightStickX") val rightStickX: Int?,
    @SerializedName("rightStickY") val rightStickY: Int?,
    @SerializedName("fiveDUp") val fiveDUp: Boolean?,
    @SerializedName("fiveDDown") val fiveDDown: Boolean?,
    @SerializedName("fiveDLeft") val fiveDLeft: Boolean?,
    @SerializedName("fiveDRight") val fiveDRight: Boolean?,
    @SerializedName("fiveDPress") val fiveDPress: Boolean?,
)

data class Event(
    @SerializedName("event") val event: String?
)

data class StreamInfo(
    @SerializedName("rtspUrl") val url: String?
)

data class Coordinate(val lat: Double, val lon: Double, var alt: Double)

data class AircraftAction(
    var action: String,
    var autonomous: Boolean
)

data class SafetyState(
    val warnings: Map<Int, String> = mapOf(
        0 to "Aircraft Not Flying",
        1 to "Go Home Button is pressed",
        2 to "Pause Button is pressed",
        3 to "GPS Signal is weak",
        4 to "Aircraft is IDLE"
    ),
    var failures: Array<Boolean> = arrayOf(false, false, false, false, false)
) {
    operator fun get(i: Int): String {
        return warnings[i] ?: "Unknown Warning"
    }
}
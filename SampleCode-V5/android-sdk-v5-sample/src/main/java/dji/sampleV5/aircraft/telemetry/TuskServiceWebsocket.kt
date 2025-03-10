package dji.sampleV5.aircraft.telemetry
import android.util.Log
import com.google.gson.Gson
import dji.sampleV5.aircraft.control.IVehicleController
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sampleV5.modulecommon.util.ITuskServiceCallback
import okhttp3.*
import okio.ByteString
import org.json.JSONObject

class TuskServiceWebsocket(private val vehicle: IVehicleController?) : ITuskServiceCallback{
    private val client: OkHttpClient = OkHttpClient()
    private lateinit var webSocket: WebSocket
    private val gson: Gson = Gson()
    // Set of Variables that system is expected to receive from server
    var maxVelocity: Double = 6.0 // m/s
    var waypointList =  listOf<Coordinate>()
    var nextWaypoint = Coordinate(0.0, 0.0, 0.0)
    var isGatherAction = false
    var isAlertAction = false
    var isStayAction = false
    var nextWaypointID = 0
    var plannerAction = "idle"
    var dwellTime = 0
    var flightMode = "idle"

    var speed = 0.0
    var gathercoordinate = Coordinate(0.0, 0.0, 0.0)

    // Establish WebSocket connection
    private val defaultIP: String = "ws://192.168.0.101:8084"
    private var currentIP: String = defaultIP
    private var connectionStatus: Boolean = false


    override fun callReconnectWebsocket() {
        connectWebSocket()
    }

    override fun callSetIP(ip: String) {
        currentIP = ip
    }

    override fun callGetIP(): String {
        return currentIP
    }

    override fun callGetConnectionStatus(): Boolean {
        return connectionStatus
    }

    fun setConnectionStatus (status: Boolean) {
        connectionStatus = status
    }

    fun getConnectionStatus(): Boolean {
        return connectionStatus
    }
    fun getCurrentIP(): String {
        return currentIP
    }
    fun setCurrentIP(ip: String) {
        currentIP = ip
    }
    fun connectWebSocket() {
//        val request = Request.Builder().url("ws://192.168.20.169:8084").build()
        var request: Request? = null
        try {
            request = Request.Builder().url(currentIP).build()
        }
        catch (e: Exception) {
            request = Request.Builder().url(defaultIP).build()
            ToastUtils.showToast("IP address invalid - resorting to default IP: $defaultIP")
            currentIP = defaultIP
        }
        webSocket = client.newWebSocket(request!!, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("TuskService", "WebSocket connection opened")
                setConnectionStatus(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleWebSocketMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Do we need this?
                Log.d("TuskService", "Received binary message: " + bytes.hex())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("TuskService", "WebSocket closing: $code $reason")
                setConnectionStatus(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("TuskService", "WebSocket connection failure: ${t.message}")
                setConnectionStatus(false)
            }
        })
    }

    // Handle received WebSocket message
    private fun handleWebSocketMessage(message: String) {
        try {
            Log.v("TuskServiceAction", "Received WebSocket message: $message")
            val jsonObject = JSONObject(message)
            val action = jsonObject.optString("action")
            val args = jsonObject.opt("args")
            when (action) {
                "FollowWaypoints" -> handleWaypointSet(args as JSONObject?)
                "FlightWaypoint" -> handleNewWaypoint(args as JSONObject?)
                "changeGimbalAngle" -> handleChangeGimbalAngle(args as JSONObject?)
                "Investigate" -> handleFlightStatusUpdate(args as JSONObject?)
                "ModeMessage" -> handleModeUpdate(args as JSONObject?)
                "modeJoystick" -> handleJoystickUpdate(args as JSONObject?)
                else -> Log.d("TuskService", "Unknown action: $action")
            }
        } catch (e: Exception) {
            Log.e("TuskService", "Failed to parse WebSocket message: ${e.message}")
        }
    }
    // Send WebSocket message
    private fun sendWebSocketMessage(action: String, args: Any) {
//        Log.v("TuskService", "Sending Websocket Message $action $args")
        val data = "{\"action\":\"$action\",\"args\":$args}"
        webSocket.send(data)
    }

    // Post aircraft state
    fun postState(state: TuskAircraftState) {
        sendWebSocketMessage("NewAircraftState", gson.toJson(state))
    }

    // Post aircraft status
    fun postStatus(status: TuskAircraftStatus) {
        sendWebSocketMessage("NewAircraftStatus", gson.toJson(status))
    }

    // Post Autonomy Status
    fun postAutonomyStatus(status: Event) {
        sendWebSocketMessage("NewFlightStatus", gson.toJson(status))
    }

    fun postStreamURL(url: StreamInfo) {
        sendWebSocketMessage("SetStream", gson.toJson(url))
    }

    // Post controller status
    fun postControlStatus(status: TuskControllerStatus) {
        sendWebSocketMessage("NewControllerStatus", gson.toJson(status))
    }

    // Reponse Handler:
    private fun handleNewControllerStatus(args: Any?) {
        // Handle the action here
        Log.d("TuskService", "Handling NewControllerStatus action")
    }

    private fun handleGetAllAircraftStatus(args: Any?) {
        // Handle the action here
        Log.d("TuskService", "Handling GetAllAircraftStatus action")
    }

    private fun handleWaypointSet(args: Any?) {
        // Handle action "FollowWaypoints" with the waypoint list
        try {
            if (args is JSONObject) {
                val flightPathArray = args.optJSONArray("flightPath")
                flightMode = "Path"
                if (flightPathArray != null) {
                    for (i in 0 until flightPathArray.length()) {
                        val waypointArray = flightPathArray.optJSONArray(i)
                        if (waypointArray != null && waypointArray.length() >= 2) {
                            val lat = waypointArray.optDouble(0)
                            val long = waypointArray.optDouble(1)
                            val alt = waypointArray.optDouble(2)
                            Log.d("TuskService", "Waypoint $i - Latitude: $lat, Longitude: $long,  Altitude: $alt")
                            // Add the waypoint to the waypoint list
                            waypointList += Coordinate(lat, long, 50.0)
                        }
                    }
                }
            } else {
                Log.d("TuskService", "Invalid args format for FollowWaypoints action")
            }
        } catch (e: Exception) {
            Log.e("TuskService", "Failed to handle FollowWaypoints action: ${e.message}")
        }
    }

    private fun handleNewWaypoint(args: Any?) {
        // Handle action "flightWaypoint" with the next waypoint
        try {
            if (args is JSONObject) {
                val lat = args.getDouble("latitude")
                val long = args.getDouble("longitude")
                val alt = args.getDouble("altitude")
                maxVelocity = args.getDouble("speed")
                maxVelocity *= (10.0 / 36.0)  // Convert from km/h to m/s
                nextWaypointID = args.getInt("waypointID")
                plannerAction = args.getString("plannerAction")
                dwellTime = args.getInt("dwellTime")
                flightMode = "Waypoint"

                Log.d(
                    "WaypointService",
                    "Next Waypoint - Latitude: $lat, Longitude: $long,  Altitude: $alt, " +
                            "Speed: $maxVelocity, WaypointID: $nextWaypointID, " +
                            "PlannerAction: $plannerAction, DwellTime: $dwellTime"
                )
                nextWaypoint = Coordinate(lat, long, alt)
            }
        } catch (e: Exception) {
            Log.e("TuskService", "Failed to handle FlightWaypoint action: ${e.message}")
        }
    }

    private fun handleChangeGimbalAngle(args: Any?) {
        // Handle action "changeGimbalAngle" with the new gimbal angle
        try {
            if (args is JSONObject) {
                val angleObject = args.getJSONObject("angle")
                val gimbalAngle = angleObject.getDouble("angle")
                vehicle!!.changeGimbalAngle(gimbalAngle)
            }
        } catch (e: Exception) {
            Log.e("TuskService", "Failed to handle changeGimbalAngle action: ${e.message}")
        }
    }

    private fun handleJoystickUpdate(args: Any?){
        // Handle action "modeJoystick" with joystick update

    }

    private fun handleModeUpdate(args: Any?){
        // Handle action "ModeMessage" with mode update
        try {
            if (args is JSONObject) {
                flightMode = args.getString("mode")
                Log.d("TuskService", "Mode: $flightMode")
                if (flightMode == "joystick") {
                    val modeInfo = args.getJSONObject("modeInfo")
                    val x = modeInfo.getDouble("x")
                    val y = modeInfo.getDouble("y")
                    val yaw = modeInfo.getInt("yaw")
                    vehicle!!.userJoystickInput(x.toFloat(), y.toFloat(), yaw)
                }
            }
        } catch (e: Exception) {
            Log.e("TuskService", "Failed to handle ModeMessage action: ${e.message}")
        }
    }

    private fun handleFlightStatusUpdate(args: Any?){
        // Handle action "FlightStatus" with decision making event
        try {
            if (args is JSONObject) {
                val event = args.getString("event")
                Log.d("TuskService", "Event: $event")
//                val flag = event?.get(0)
                if (event == "gather-info"){
                    Log.d("TuskService", "Handling GatherInfo action")
                    isGatherAction = true
                    nextWaypoint = Coordinate(
                        args.optDouble("latitude"),
                        args.optDouble("longitude"),
                        args.optDouble("altitude")
                    )
                    speed = args.optDouble("speed")
                    nextWaypointID = args.optInt("waypointID")
                    plannerAction = args.optString("plannerAction")
                    dwellTime = args.optInt("dwellTime")
                } else if (event == "alert-operator"){
                    isAlertAction = true
                } else {
                    Log.d("TuskService", "Invalid event format for FlightStatus action")
                }
            }
        } catch (e: Exception) {
            Log.e("TuskService", "Failed to handle FlightStatus action: ${e.message}")
        }
    }

    fun getActions() {
        TODO("Not yet implemented")
    }

    fun postControllerStatus(status: TuskControllerStatus) {

    }

//    override fun callReconnectWebsocket() {
//        connectWebSocket()
//    }
}
package dji.sampleV5.aircraft.control
import android.util.Log
import dji.sampleV5.aircraft.telemetry.AircraftAction
import dji.sampleV5.aircraft.telemetry.Coordinate
import dji.sampleV5.aircraft.telemetry.Event
import dji.sampleV5.aircraft.telemetry.SafetyState
import dji.sampleV5.aircraft.telemetry.SartopoService
import dji.sampleV5.aircraft.telemetry.StreamInfo
import dji.sampleV5.aircraft.telemetry.TuskAircraftState
import dji.sampleV5.aircraft.telemetry.TuskAircraftStatus
import dji.sampleV5.aircraft.telemetry.TuskControllerStatus
import dji.sampleV5.aircraft.telemetry.TuskServiceWebsocket
import dji.sampleV5.aircraft.video.StreamManager
import dji.sdk.keyvalue.key.AirLinkKey
import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.DJIKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.GimbalKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.camera.ThermalAreaMetersureTemperature
import dji.sdk.keyvalue.value.camera.ThermalTemperatureMeasureMode
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotation
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotationMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.utils.RxUtil
import dji.v5.manager.KeyManager
import dji.v5.ux.mapkit.core.models.DJILatLng
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.processors.PublishProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PachKeyManager() : IVehicleController {
    /*
    * The companion object brackets are used to essentially create a singleton object,
    * where when the PachKeyManager class is called the first time, it creates an instance
    * of the class. But, when PachKeyManager is called again, it will return the same
    * instance of that object, effectively making it so that there is only one instance
    * of PachKeyManager running at anytime, and can be referenced from any activity.
    *
    * Explanation of code:
    * - @Volitile indicates that when something about the object is changed, it is reflected
    * in all other threads.
    * - getInstance() initially checks if the instance is null, and if it's not,
    * create an instance.
    *
    * - synchronized(this){} prevents multiple threads from excecuting the block of code at
    * at the same time. That means, only one thread can create the instance.
    *
    * - inside the synchronized block it checks again if instance is null, and if not, creates
    * an instance of PachKeyManager, and sets instance equal to it.
    *
    * This code was a ChatGPT suggestion - while it works for the testing I've done with it,
    * I'm wondering if this is in accordance with some of the best practices for android dev.
    *
    * */
    companion object {
        @Volatile
        private var instance: PachKeyManager? = null
        fun getInstance(): PachKeyManager {
            return instance ?: synchronized(this) {
                instance ?: PachKeyManager().also { instance = it }
            }
        }
    }
    // Initialize necessary classes
    private var sartopo: SartopoService = SartopoService.getInstance()
    private val waypointDataProcessor = PublishProcessor.create<DJILatLng>() // for publishing waypoints to map
    private val connectionDataProcessor = PublishProcessor.create<Boolean>() // for publishing connection status to status indicator
    private val autonamousDataProcessor = PublishProcessor.create<Boolean>() // for publishing messages to status indicator
    private val actionDataProcessor = PublishProcessor.create<String>() // for publishing action status to status indicator
    private val warningDataProcessor = PublishProcessor.create<String>() // for publishing warnings to status indicator
    private val messageDataProcessor = PublishProcessor.create<String>() // for publishing messages to status indicator
    private val streamDataProcessor = PublishProcessor.create<Boolean>() // for publishing stream URL to status indicator
    val telemService = TuskServiceWebsocket(this)

    /**
     * Safety Failures, action, autonomous, and safety warnings are all used for the PachWidget
     * to display the current status of the aircraft. These need to be moved to the datastructures
     * file, in order to keep the code consistent and clean.
     */
    private val safetyState = SafetyState()
    private var actionState = AircraftAction("", false)
    private var controller = VirtualStickControl()
    private val kp = 0.23f // was 0.4 // .6
    private val ki = 0.0012f // was 0.05 // 0.003
    private val kd = 0.27f // was 0.9 // 0.23
    private var pidController = PidController(kp, ki, kd)
    val mainScope = CoroutineScope(Dispatchers.Main)
    val streamer = StreamManager()
    var stateData = TuskAircraftState( 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0, windDirection = null, false)
    var statusData = TuskAircraftStatus( connected = false, battery = 0, gpsSignal = 0, gps = 0,
        signalQuality =  0,  goHomeState = null, flightMode = null, motorsOn = false, homeLocationLat = null,
            homeLocationLong = null, gimbalAngle = 0.0, goHomeStatus = null, takeoffAltitude = null, isStreaming = false)
    var controllerStatus = TuskControllerStatus( battery = 0, pauseButton = false, goHomeButton = false,
            leftStickX = 0,leftStickY=0,rightStickX=0,rightStickY=0, fiveDUp = false, fiveDDown = false,
            fiveDRight = false, fiveDLeft = false, fiveDPress = false)
    val R = 6378.137 // Radius of earth in KM

    // Create variables here
    private var keyDisposables: CompositeDisposable? = null


    private val fiveDKey = KeyTools.createKey(RemoteControllerKey.KeyFiveDimensionPressedStatus)
    var fiveDUp = false
    var fiveDDown = false
    var fiveDRight = false
    var fiveDLeft = false
    var fiveDPress = false

    init {
        telemService.connectWebSocket()
        initializeFlightParameters()
        keyDisposables = CompositeDisposable()
//        streamer.startStream()
    }

    fun updateStatusWidget() {
        // Function updates the status widget with the current status of the aircraft, including
        // connection status, autonomous/manual status, and any warnings that may be present
        mainScope.launch {
            var status = ""
            var prevWaypoint = Coordinate(0.0,0.0,0.0)
            while(isActive) {
                this@PachKeyManager.safetyChecks()
                var warnings = ""
                for (i in safetyState.failures.indices) {
                    if (safetyState.failures[i]) {
                        warnings += safetyState[i] + "; "
                    }
                }
                if (this@PachKeyManager.statusData.goHomeStatus == "RETURNING_TO_HOME") {
                    status = "Returning Home"
                }
                if (this@PachKeyManager.telemService.nextWaypoint != prevWaypoint && !this@PachKeyManager.actionState.autonomous) {
                    status = if (warnings.isNotEmpty()) "Manual | Flight Info Received | $warnings" else "Manual | Flight Info Received"
                }
                if (this@PachKeyManager.telemService.nextWaypoint == prevWaypoint && !this@PachKeyManager.actionState.autonomous) {
                    status = if (warnings.isNotEmpty()) "Manual | $warnings" else "Manual"
                }
                if (this@PachKeyManager.actionState.autonomous) {
                    status = if (this@PachKeyManager.actionState.action != "") "Autonomous | ${this@PachKeyManager.actionState.action}" else "Autonomous"
                    prevWaypoint = this@PachKeyManager.telemService.nextWaypoint
                }
//                this@PachKeyManager.sendWaypointToMap(wp)
//                pachModel.updateConnection(this@PachKeyManager.telemService.getConnectionStatus())
//                pachModel.updateMsg(status)

                sendDataToStatusWidget(status, this@PachKeyManager.telemService.getConnectionStatus())
                delay(1000)
            }
        }
    }

//    fun setWaypointListener(listener: WaypointListener) {
//        this.listener = listener
//    }
//
//    fun isWaypointListenerSet(): Boolean {
//        return this.listener != null
//    }
    fun runTesting() {
        KeyManager.getInstance().listen(fiveDKey, this) { _, newValue ->
            mainScope.launch {
                Log.v("PachKeyManager", "FiveD: $newValue")
                if (newValue != null) {
                    fiveDUp = newValue.upwards
                    fiveDDown = newValue.downwards
                    fiveDRight = newValue.rightwards
                    fiveDLeft = newValue.leftwards
                    fiveDPress = newValue.middlePressed
                }

                // If motors are on, start mission
                if (fiveDUp) {
                    controller.startTakeOff()
                }

                if (fiveDDown) {
//                    streamer.startStream()
//                    sendStreamURL(this@PachKeyManager, streamer.getStreamURL())
                    sendStreamURL(streamer.getStreamURL())
//                    streamer.initChannelStateListener()
//                    controller.startLanding()
//                    controller.endVirtualStick()
                }
                if (fiveDPress) {
                    Log.v("PachKeyManager", "FiveD Pressed")
                    engageAutonomy()
//                    followWaypoints(backyardCoordinatesComplexChangingAlt)
//                    Log.v("PachKeyManager", "Following Waypoint List: $HIPPOWaypoints")
//                    HIPPOWaypoints = telemService.waypointList
//                    followWaypoints(HIPPOWaypoints)
//                    flyOrbitPath(
//                        Coordinate(stateData.latitude!!, stateData.longitude!!,stateData.altitude!!),
//                        10.0)
//                    diveAndYaw(60.0, 20.0)
//                    controller.endVirtualStick()
                    Log.v("PachKeyManager", "Finished fiveDPress Execution")
                }
                if (fiveDLeft) {
                    // test orbit
                    delay(500)
                    if (!actionState.autonomous) {
                        val coord = Coordinate(
                            stateData.latitude!!,
                            stateData.longitude!!,
                            stateData.altitude!!
                        )
                        flyOrbitPath(coord)
                    }
                    else Log.d("PachKeyManager", "Cannot fly orbit while in autonomous mode")
                }
            }

        }
        registerKeys()

    }
    private suspend fun engageAutonomy() {
        // Function evaluates the current received flight mode and engages the appropriate
        // autonomy mode.
        // If the flight mode is "Waypoint", the function will follow the received waypoint list
        // If the flight mode is "Path", the function will follow the received path

//        // If drone is not flying, then takeoff
        if (stateData.isFlying!=true){
            controller.startTakeOff()
        }
        this@PachKeyManager.actionState.autonomous = true
        when (telemService.flightMode) {
            "Waypoint" -> flyHippo()
            "Path" -> followWaypoints(telemService.waypointList)
            else -> {
                Log.v("PachKeyManager", "No valid flight mode detected")
            }
        }
        this@PachKeyManager.sendWaypointToMap(DJILatLng(0.0,0.0))
        this@PachKeyManager.actionState.autonomous = false
    }

    private fun sendState(telemetry: TuskAircraftState) {
        telemService.postState(telemetry)
    }

    private fun sendStatus(status: TuskAircraftStatus) {
        telemService.postStatus(status)
    }

    private fun sendAutonomyStatus(status: String) {
        telemService.postAutonomyStatus(Event(status))
        Log.v("PachKeyManager", "Autonomy Status: $status")
    }

    fun sendStreamURL(url: String) {
        telemService.postStreamURL(StreamInfo(url))
        Log.v("PachKeyManager", "Stream URL: $url")
    }

    private fun setActionInfo() {
        if (this.actionState.action != "AUTONOMOUS") {
            if (telemService.waypointList.isNotEmpty()) {
                this.actionState.action = "INFO RECEIVED"
            }
        }
    }

//    companion object {
//        fun sendStreamURL(instance: PachKeyManager, url: String) {
//            instance.telemService.postStreamURL(StreamInfo(url))
//        }
//    }
    private fun sendControllerStatus(status: TuskControllerStatus) {
        telemService.postControllerStatus(status)
    }

    // Holder function that registers all necessary keys and handles their operation during changes
    private fun registerKeys(){
        // Setup PachKeyManager and define the keys that we want to listen to
        Log.v("PachKeyManager", "Registering Keys")
        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D)
        ) {
            stateData = stateData.copy(
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = it.altitude
            )
            sendState(stateData)
            Log.v("PachTelemetry", "KeyAircraftLocation $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAircraftAttitude)
        ) {
            stateData = stateData.copy(
                roll = it.roll,
                pitch = it.pitch,
                yaw = it.yaw)
            sendState(stateData)
            Log.d("PachTelemetry", "KeyAircraftAttitude $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity)
        ) {
            stateData = stateData.copy(
                velocityX = it.x,
                velocityY = it.y,
                velocityZ = it.z)
            sendState(stateData)
            Log.d("PachTelemetry", "AircraftVelocity $it")
            if (sartopo.isURLValid()) {
                sartopo.sendGetRequest(stateData.longitude!!, stateData.latitude!!)
            } else {
                Log.e("Sartopo", "Sartopo URL is not valid\n" +
                        "Base: ${sartopo.getBaseURL()}\n" +
                        "Access: ${sartopo.getAccessURL()}\n" +
                        "ID: ${sartopo.getDeviceID()}\n")
            }
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyWindSpeed)
        ) {
            stateData = stateData.copy(windSpeed = it)
            sendState(stateData)
            Log.d("PachTelemetry", "WindSpeed $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyWindDirection)
        ) {
            stateData = stateData.copy(windDirection = it.toString())
            sendState(stateData)
            Log.d("PachTelemetry", "WindDirection $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyIsFlying)
        ) {
            stateData = stateData.copy(isFlying = it)
            sendState(stateData)
            Log.d("PachTelemetry", "IsFlying $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyConnection)
        ) {
            statusData = statusData.copy(connected = it)
            sendStatus(statusData)
            Log.d("PachTelemetry", "Connection $it")
        }

        registerKey(
            KeyTools.createKey(BatteryKey.KeyChargeRemainingInPercent)
        ) {
            statusData = statusData.copy(battery = it)
            sendStatus(statusData)
            Log.d("PachTelemetry", "Battery Level $it")
        }


        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyGPSSatelliteCount)
        )    {
                statusData = statusData.copy(gps = it)
                sendStatus(statusData)
                Log.d("PachTelemetry", "GPSSatelliteCount $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel)
        ) {
            statusData = statusData.copy(gpsSignal = it.value())
            sendStatus(statusData)
            Log.d("PachTelemetry", "GPSSignalLevel $it")
        }


        registerKey(
            KeyTools.createKey(AirLinkKey.KeySignalQuality)
        ) {
            statusData = statusData.copy(signalQuality = it)
            sendStatus(statusData)
            Log.d("PachTelemetry", "SignalQuality $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyGoHomeState)
        ) {
            statusData = statusData.copy(goHomeState = it.toString())
            sendStatus(statusData)
            Log.d("PachTelemetry", "GoHomeState $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyFlightModeString)
        ) {
            statusData = statusData.copy(flightMode = it)
            sendStatus(statusData)
            Log.d("PachTelemetry", "FlightMode $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAreMotorsOn)
        ) {
            statusData = statusData.copy(motorsOn = it)
            sendStatus(statusData)
            Log.d("PachTelemetry", "MotorsOn $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyHomeLocation)
        ) {
            statusData =
                statusData.copy(
                    homeLocationLat = it.latitude,
                    homeLocationLong = it.longitude)
            sendStatus(statusData)
            Log.d("PachTelemetry", "HomeLocation $it")
        }

        registerKey(
            KeyTools.createKey(GimbalKey.KeyGimbalAttitude)
        ) {
            statusData = statusData.copy(gimbalAngle = it.pitch)
            sendStatus(statusData)
            Log.d("PachTelemetry", "GimbalPitch $it")
        }
        registerKey(
                KeyTools.createKey(FlightControllerKey.KeyGoHomeState)
        ){
            statusData = statusData.copy(goHomeStatus = it.toString())
            sendStatus(statusData)
            Log.d("PachTelemetry", "GoHomeStatus $it")
        }

        registerKey(
                KeyTools.createKey(FlightControllerKey.KeyTakeoffLocationAltitude)
        ){
            statusData = statusData.copy(takeoffAltitude = it)
            sendStatus(statusData)
            Log.d("PachTelemetry", "TakeoffAltitude $it")
        }

        // TuskControllerKeys Setup
        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyBatteryInfo)
        ) {
            controllerStatus = controllerStatus.copy(battery = it.batteryPercent)
            sendControllerStatus(controllerStatus)
            Log.d("PachTelemetry", "ControllerBattery $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyPauseButtonDown)
        ){
            controllerStatus = controllerStatus.copy(pauseButton = it)
            sendControllerStatus(controllerStatus)
            Log.d("PachTelemetry", "PauseButton $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyGoHomeButtonDown)
        ){
            controllerStatus = controllerStatus.copy(goHomeButton = it)
            sendControllerStatus(controllerStatus)
            Log.d("PachTelemetry", "GoHomeButton $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyStickLeftHorizontal)
        ){
            controllerStatus = controllerStatus.copy(leftStickX = it)
            sendControllerStatus(controllerStatus)
            Log.d("PachTelemetry", "StickLeftHorizontal $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyStickLeftVertical)
        ){
            controllerStatus = controllerStatus.copy(leftStickY = it)
            sendControllerStatus(controllerStatus)
            Log.d("PachTelemetry", "StickLeftVertical $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyStickRightHorizontal)
        ){
            controllerStatus = controllerStatus.copy(rightStickX = it)
            sendControllerStatus(controllerStatus)
            Log.d("PachTelemetry", "StickRightHorizontal $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyStickLeftVertical)
        ){
            controllerStatus = controllerStatus.copy(rightStickY = it)
            sendControllerStatus(controllerStatus)
            Log.d("PachTelemetry", "StickRightVertical $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyFiveDimensionPressedStatus)
        ){
            controllerStatus = controllerStatus.copy(
                    fiveDUp = it.upwards,
                    fiveDDown = it.downwards,
                    fiveDLeft = it.leftwards,
                    fiveDRight = it.rightwards,
                    fiveDPress = it.middlePressed)
            sendControllerStatus(controllerStatus)
            Log.d("PachKeyManager", "FiveDButton $it")
        }

        keyDisposables?.add( // dji key does not work in this instance
            streamDataProcessor
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.v("PachKeyManager", "is Streaming: $it")
                    statusData = statusData.copy(isStreaming = it)
                    sendStatus(statusData)
                }, {
                    Log.e("PachKeyManager", "Stream URL Error: $it")
                })

        )
        // Continue to do this for the other required keys...
    }

    private fun initializeFlightParameters() {
        // Function initializes any static parameters prior to flight
        // Set battery warning threshold to 30%
        val batteryWarningValue = 20
        val batteryThresh = KeyTools.createKey(FlightControllerKey.KeyLowBatteryWarningThreshold)
        KeyManager.getInstance().setValue(batteryThresh, batteryWarningValue, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.v("PachKeyManager", "Battery threshold set to $batteryWarningValue%")
            }

            override fun onFailure(error: IDJIError) {
                Log.e("PachKeyManager", "Error: $error")
            }
        })
    }

    private fun <T : Any> registerKey(
        djiKey: DJIKey<T>,
        consumer: Consumer<T>,
        ): CompositeDisposable? {
        keyDisposables?.add(
            RxUtil.addListener(djiKey, this)
                .subscribe(consumer) { error ->
                    Log.e("PachKeyManager", "Error: $error")
                }

        )
        return keyDisposables
    }

    // Make a function to rotate the gimbal by a certain angle
    override fun changeGimbalAngle(angle: Double){
        // Gimbal can be rotated by a range of [-90, 35]
        // The provided angle sets the pitch of the gimbal to the given angle
        val gimbalKey = KeyTools.createKey(GimbalKey.KeyRotateByAngle)
        KeyManager.getInstance().performAction(gimbalKey,
                GimbalAngleRotation(
                        GimbalAngleRotationMode.ABSOLUTE_ANGLE,
                        angle, // Pitch
                        0.0,   // Roll
                        0.0,   // Yaw
                        false, // Pitch ignored
                        false, // Roll ignored
                        false, // Yaw ignored
                        2.0,   // Rotation time
                        false, // Joint reference
                        0      // Timeout
                ),
                object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                    override fun onSuccess(t: EmptyMsg?) {
                        Log.v("PachKeyManager", "Gimbal Rotated")
                    }

                    override fun onFailure(error: IDJIError) {
                        Log.e("PachKeyManager", "Gimbal Rotation Error: ,$error")
                    }
                }
        )
    }

    fun getThermalVideo(){
        // When function is called, the thermal video will be set as primary stream
        // Get Thermal Camera Max Temperature
        // Set camera to thermal mode first by calling KeyCameraVideoStreamSource --> INFRARED_CAMERA
        val cameraSourceKey = KeyTools.createKey(CameraKey.KeyCameraVideoStreamSource)
        KeyManager.getInstance().setValue(cameraSourceKey,
            CameraVideoStreamSourceType.INFRARED_CAMERA,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.v("PachKeyManager", "Camera Source Set to Thermal")
                    val thermalMeasureMode =
                            KeyTools.createKey(CameraKey.KeyThermalTemperatureMeasureMode)
                    object : CommonCallbacks.CompletionCallbackWithParam<Boolean> {
                        override fun onSuccess(value: Boolean?) {
                            Log.v(
                                    "PachKeyManager",
                                    "Thermal Measurement Mode: ,$value"
                            )
                        }
                        override fun onFailure(error: IDJIError) {
                            Log.e(
                                    "PachKeyManager",
                                    "Thermal Measurement Mode: ,$error"
                            )
                        }
                    }

                    //                                    KeyTools.createKey(CameraKey.KeyThermalTemperatureMeasureMode)
                    KeyManager.getInstance().setValue(thermalMeasureMode,
                            ThermalTemperatureMeasureMode.REGION,
                            object : CommonCallbacks.CompletionCallback {
                                override fun onSuccess() {
                                    Log.v(
                                            "PachKeyManager",
                                            "Thermal Measure Mode Set to REGION"
                                    )
                                }

                                override fun onFailure(error: IDJIError) {
                                    Log.v(
                                            "PachKeyManager",
                                            "Thermal Measure Mode Error: ,$error"
                                    )
                                }
                            }
                    )
                    // Set CameraLensType to CAMERA_LENS_THERMAL
                    val thermalLensTypeKey = KeyTools.createCameraKey(
                            CameraKey.KeyThermalRegionMetersureTemperature,
                            ComponentIndexType.FPV,
                            CameraLensType.CAMERA_LENS_THERMAL
                    )
                    //                    CameraLensType.CAMERA_LENS_THERMAL
                    KeyManager.getInstance().getValue(thermalLensTypeKey,
                            object :
                                    CommonCallbacks.CompletionCallbackWithParam<ThermalAreaMetersureTemperature> {
                                // Call KeyThermalTemperatureMeasureMode to set ThermalTemperatureMeasureMode to REGION
                                override fun onSuccess(t: ThermalAreaMetersureTemperature?) {
                                    Log.v(
                                            "PachKeyManager",
                                            "Thermal Area Measure Mode Set to REGION"
                                    )
                                }

                                override fun onFailure(error: IDJIError) {
                                    Log.v(
                                            "PachKeyManager",
                                            "Thermal Area Measure Mode Error: ,$error"
                                    )
                                }
                            }
                    )
                }

                override fun onFailure(error: IDJIError) {
                    Log.v("PachKeyManager", "Camera Source Set to Thermal")
                }
            }
        )
    }

    fun getWideVideo(){
        // When function is called, the rgb video will be set as primary stream
        val cameraSourceKey = KeyTools.createKey(CameraKey.KeyCameraVideoStreamSource)
        KeyManager.getInstance().setValue(cameraSourceKey,
                CameraVideoStreamSourceType.WIDE_CAMERA,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        Log.v("PachKeyManager", "Camera Source Set to Wide")
                    }
                    override fun onFailure(error: IDJIError) {
                        Log.v("PachKeyManager", "Camera Failed to Set to Wide $error")
                    }
                })
    }

    fun getZoomVideo(){
        // When function is called, the rgb video will be set as primary stream
        val cameraSourceKey = KeyTools.createKey(CameraKey.KeyCameraVideoStreamSource)
        KeyManager.getInstance().setValue(cameraSourceKey,
                CameraVideoStreamSourceType.ZOOM_CAMERA,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        Log.v("PachKeyManager", "Camera Source Set to Zoom")
                    }
                    override fun onFailure(error: IDJIError) {
                        Log.v("PachKeyManager", "Camera Failed to Set to Zoom $error")
                    }
                })
    }
    private fun decisionChecks(): Boolean {
        // Function checks to see if any key decision points have been reached
        if (telemService.isAlertAction) {
            Log.v("PachKeyManager", "Alert Action")
            return false
        }
        if (telemService.isGatherAction) {
            Log.v("PachKeyManager", "Gather Action")
            return false
        }

        return true
    }

    private suspend fun engageGatherAction(){
        // Function handles the gathering of information.
        // Called from multiple locations, so this allows for a standardized execution
        val orbitRadius = 10.0
        this@PachKeyManager.actionState.action = "Gathering Info"
        sendAutonomyStatus("GatheringInfo")
//                diveAndYaw(waypoint.alt-10, 30.0)
        Log.v("PackKeyManagerHIPPO", "Gathering coordinate: ${telemService.nextWaypoint}")
        flyOrbitPath(
            telemService.nextWaypoint,
            orbitRadius)
    }

    private fun safetyChecks(): Boolean {
        // Function checks all safety information before returning a boolean for proceeding.
        // Checks:
        // 1. Is the aircraft flying?
        safetyState.failures[0] = (if (!stateData.isFlying!!) true.also {
            Log.v("PachKeyManager", safetyState[0])} else false)
        safetyState.failures[1] = (if (controllerStatus.goHomeButton!!) true.also {
            Log.v("PachKeyManager", safetyState[1])} else false)
        safetyState.failures[2] = (if (controllerStatus.pauseButton!!) true.also {
            Log.v("PachKeyManager", safetyState[2])} else false)
        safetyState.failures[3] = (if (statusData.gps!! <3) true.also {
            Log.v("PachKeyManager", safetyState[3])} else false)
        safetyState.failures[4] = (if (statusData.goHomeStatus != "IDLE") true.also {
            Log.v("PachKeyManager", safetyState[4])} else false)

        for (i in safetyState.failures){
            if (i){
                return false
            }
        }
        return true
    }
    suspend fun goToAltitude(alt: Double){
        // When called, this function will make the aircraft go to a certain altitude
        while (stateData.altitude!! != alt) {
            Log.v("PachKeyManager", "Altitude Set: ${alt}. Current Altitude: ${stateData.altitude}")
            // command drone x velocity to move to target location
            if (safetyChecks()) {
                controller.sendVirtualStickVelocityBody(0.0, 0.0, stateData.yaw!!, alt)
            } else {
                Log.v("PachKeyManager", "Safety Check Failed")
                break
            }

            delay(100L)
        }
    }

    private fun adjustAltForTerrain(alt: Double): Double {
        // Function expects the input to be in MSL altitude.
        // Using the takeoff altitude, we then adjust to account for relative altitude in meters
        return alt - statusData.takeoffAltitude!!
    }

    suspend fun goToYawAngle(angle: Double){
        // When called aircraft will rotate to a given yaw angle
        while (stateData.yaw!! != angle) {
            Log.v("PachKeyManager", "Yaw Set: ${stateData.yaw}")
            // command drone x velocity to move to target location
            if (safetyChecks()) {
                controller.sendVirtualStickVelocityBody(0.0, 0.0, angle, stateData.altitude!!)
            } else {
                Log.v("PachKeyManager", "Safety Check Failed")
                break
            }
            delay(100L)
        }
    }

    suspend fun goToLocationForward(lat: Double, lon: Double, alt: Double){
        // When called, this function will make the aircraft go to a certain location
        // Setting the yaw to a negative number will cause the yaw to be dynamically set. Helpful
        // cases when the aircraft needs to adjust its heading to reach a location
        // Edge Cases:
        // What if drone is already flying?
        // What if drone loses connection or GPS signal?
        // What if remote controller is disconnected?
        // What if drone is already at the location?
        // What if the operator takes control of the aircraft?

        // compute distance to target location using lat and lon
        var distance = computeLatLonDistance(lat, lon)
        var yawAngle = computeYawAngle(lat, lon)
        var xVel = pidController.getControl(distance)
        while (distance > pidController.posTolerance) {
            // ((distance > pidController.posTolerance) and (stateData.velocityX!! > pidController.velTolerance))
            //What if we overshoot the target location? Will the aircraft back up or turn around?
            Log.v("PachControlAction", "Distance: $distance")
            xVel = pidController.getControl(distance)
            val clippedXvel = xVel.coerceIn(-pidController.maxVelocity, pidController.maxVelocity)
            Log.v("PachControlAction", "Commanded X Velocity: $xVel, Clipped Velocity  $clippedXvel")
            distance = computeLatLonDistance(lat, lon)

            // Update Yaw
            yawAngle = computeYawAngle(lat, lon)

            Log.v("PachControlAction", "Commanded Yaw: $yawAngle | Commanded Altitude: $alt | xvel: $xVel | clippedXvel: $clippedXvel")

            // command drone x velocity to move to target location
            if (decisionChecks()) {
                if (safetyChecks()) {
                    controller.sendVirtualStickVelocityBody(clippedXvel, 0.0, yawAngle, alt)
                } else {
                    Log.v("PachKeyManager", "Safety Check Failed")
                    break
                }
            } else {
                Log.v("PachKeyManager", "Decision Check failed while proceeding to waypoint")
                if (this@PachKeyManager.telemService.isAlertAction) {
                    Log.v("PachKeyManager", "Alerted Operator")
                    break
                } else if (this@PachKeyManager.telemService.isGatherAction) {
                    Log.v("PachKeyManager", "Gather Action")
                    engageGatherAction()
                    break
                }
            }
            delay(100L)
        }
        pidController.resetIntegral()
    }

    suspend fun goToLocationFixedYaw(lat: Double, lon: Double, alt: Double, yaw: Double, tolerence: Double = pidController.posTolerance){
        // Function goes to a coordinate location assuming a fixed yaw angle
        // compute distance to target location using lat and lon
        // TODO: There seems to be a control issue with this implementation.
        //  Aircraft doesn't seem to reach the waypoint in the expected manner. Potential coordinate frame issue.
        //  Can try checking basic flight control
        var distance = computeLatLonDistance(lat, lon)
        while (distance > tolerence) {

            // ((distance > pidController.posTolerance) and (stateData.velocityX!! > pidController.velTolerance))
            //What if we overshoot the target location? Will the aircraft back up or turn around?
            Log.d("PachControlAction", "wp: $lat, $lon")
            Log.v("PachControlAction", "Distance: $distance")
            val yError = computeLatDistance(lat)
            val xError = computeLonDistance(lon)
            Log.v("PachControlAction", "Y Error: $yError | X Error: $xError | Distance: $distance")
            val xVel = pidController.getControl(xError)
            val yVel = pidController.getControl(yError)
            val clippedXvel = xVel.coerceIn(-pidController.maxVelocity, pidController.maxVelocity)
            val clippedYvel = yVel.coerceIn(-pidController.maxVelocity, pidController.maxVelocity)

            Log.v("PachControlAction", "Commanded Yaw: $yaw | Commanded Altitude: $alt | xvel: $xVel | yvel: $yVel")
            // command drone x & y velocity to move to target location with a defined yaw
            if (!telemService.isAlertAction) {
                if (safetyChecks()) {
                    controller.sendVirtualStickVelocityGround(clippedYvel, clippedXvel, yaw, alt)
                } else {
                    Log.v("PachKeyManager", "Safety Check Failed")
                    break
                }
            } else {
                Log.v("PachKeyManager", "Alerted Operator")
                break
            }
            delay(50L)
            distance = computeLatLonDistance(lat, lon)
        }
        pidController.resetIntegral()
    }

    suspend fun flyHippo() {
        // Function will fly using the HIPPO decision making framework.
        // This will fly to a given waypoint and continue along to the following waypoint unless
        // a specific decision making flag has been raised.
        // When called, this function will make the aircraft go to a series of locations
        // Edge Cases:
        // What if drone is already flying?
        // What if drone loses connection or GPS signal?
        // What if remote controller is disconnected?
        // What if drone is already at the location?
        // What if the operator takes control of the aircraft?

        // compute distance to target location using lat and lon
        var waypoint = getNewDirection()
        var waypointID = telemService.nextWaypointID
        sendAutonomyStatus("waypoint-reached")
        // Check to see that advanced virtual stick is enabled
        controller.ensureAdvancedVirtualStickMode()

        while (safetyChecks()) {
            // Handle logic for action execution
            if (this@PachKeyManager.decisionChecks()) {
                Log.v("PachKeyManagerHIPPO", "Going to Waypoint: $waypoint")
                this@PachKeyManager.sendWaypointToMap(DJILatLng(waypoint.lat, waypoint.lon))
                this@PachKeyManager.actionState.action = "Following waypoints"
                goToLocationForward(
                    waypoint.lat,
                    waypoint.lon,
                    waypoint.alt)
            } else if (this@PachKeyManager.telemService.isAlertAction) {
                // Alert action stops the aircraft's movement
                telemService.isAlertAction = false
                Log.v("PachKeyManagerHIPPO", "Alert Action")
                this@PachKeyManager.actionState.action = "Flight Paused - person?"
                sendAutonomyStatus("AlertedOperator")
                break
            } else if (this@PachKeyManager.telemService.isGatherAction) {
                // Send Gather Confirmation
                Log.v("PachKeyManagerHIPPO", "Gather Action")
                engageGatherAction()
            }
             else {
                Log.v("PachKeyManagerHIPPO", "Unknown Decision Check Failed")
                break
            }
            this@PachKeyManager.actionState.action = "" // if no action is taken, reset action to empty string

            // Handle logic for updating waypoint
            if (telemService.isGatherAction){
                telemService.isGatherAction = false
                Log.v("PachKeyManagerHIPPO", "Continuing to Waypoint")
            }
            else if (telemService.isAlertAction){
                telemService.isAlertAction = false
                Log.v("PachKeyManagerHIPPO", "Alerted Operator")
                break
            }
            else {
                if (telemService.plannerAction == "stay" && telemService.isStayAction){
                    Log.v("PachKeyManagerHIPPO", "Staying at Waypoint: $waypoint")
                    this@PachKeyManager.actionState.action = "Holding position"
//                    sendAutonomyStatus("waypoint-reached")
                    delay(telemService.dwellTime.toLong())
                    telemService.isStayAction = false // reset flag to false so stay action is not taken
                }
                else if (waypointID != telemService.nextWaypointID) {
                    sendAutonomyStatus("waypoint-reached")
                    waypoint = getNewDirection()
                    waypointID = telemService.nextWaypointID
                    Log.v("PachKeyManagerHIPPO", "Waypoint Updated: ID$waypointID with action ${telemService.plannerAction}")
//                    delay(100L)
                } else {
                    delay(100L)
                    Log.v("PachKeyManagerHIPPO", "Waypoint Not Updated")
                }
            }
            this@PachKeyManager.actionState.action = "" // if no action is taken, reset action to empty string
        }
        controller.endVirtualStick()
    }

    private fun getNewDirection(): Coordinate {
        // Function will update flight parameters based on the external information
        // Copy next waypoint variable to new variable
        val newAlt : Double = adjustAltForTerrain(telemService.nextWaypoint.alt)
        val waypoint = Coordinate(
            telemService.nextWaypoint.lat,
            telemService.nextWaypoint.lon,
            newAlt
        )
        if (telemService.plannerAction == "stay"){
            telemService.isStayAction = true
        }
        pidController.maxVelocity = telemService.maxVelocity
        return waypoint
    }

    private suspend fun followWaypoints(wpList: List<Coordinate>){
        // When called, this function will make the aircraft follow a list of waypoints
        // Figure out if the latest state is given

        // Check to see that advanced virtual stick is enabled
        controller.ensureAdvancedVirtualStickMode()

        for (wp in wpList){
            if (safetyChecks()) {
                this@PachKeyManager.sendWaypointToMap(DJILatLng(wp.lat, wp.lon))
                goToLocationForward(wp.lat, wp.lon, wp.alt)

            } else{
                Log.v("SafetyChecks", "Safety Check Failed")
                break
            }
        }

        controller.endVirtualStick()
    }
    

    suspend fun flyOrbitPath(center:Coordinate, radius:Double=10.0) {
        // When called, this function will make the aircraft fly in a circle around a point

        // Check to see that advanced virtual stick is enabled
        controller.ensureAdvancedVirtualStickMode()
        val topOfCircle = Coordinate(radius/(111111) + center.lat, center.lon, center.alt)
        var leftCircleOrigin = false
        val yawVel = 2 * Math.PI / radius * -1 // rad/s
        val tanVel = yawVel * radius * -1 // m/s
        // Head to the top of the circle
        goToLocationForward(
            topOfCircle.lat,
            topOfCircle.lon,
            topOfCircle.alt)
        // go do some location north of the current location with yaw directed towards the center
//        goToLocationFixedYaw(topOfCircle.lat, topOfCircle.lon, topOfCircle.alt, 180.0, tolerence = pidController.posTolerance / 3) // go to the top of the circle
        while (!leftCircleOrigin || (computeLatLonDistance(topOfCircle.lat, topOfCircle.lon) > pidController.posTolerance)) {
            if (telemService.isAlertAction)
            {
                Log.v("PachKeyManager", "Alerted Operator")
                break
            }
            if (!safetyChecks()) {
                Log.v("PachKeyManager", "Safety Check Failed")
                break
            }
            if (computeLatLonDistance(topOfCircle.lat, topOfCircle.lon) > pidController.posTolerance) {
                if (!leftCircleOrigin) {
                    leftCircleOrigin = true
                    Log.v("PachKeyManager", "Left Circle Origin")
                }
            }
            // the drone takes time to ramp up its tangental velocty, so we need to proportionally adjust the yaw
            // to keep the drone facing the center of the circle
            val proportion = calculateXYVel() / tanVel
            val yaw = Math.toDegrees(yawVel * proportion)
            controller.sendVirtualStickAnglularVelocityBody(0.0, tanVel, yaw, stateData.altitude!!)
            delay(10L)
        }
        // calculate the yaw to send to go2LocationForward
    }

    fun calculateXYVel(): Double {
        return sqrt(stateData.velocityX!!*stateData.velocityX!! + stateData.velocityY!!*stateData.velocityY!!)
    }

    suspend fun diveAndYaw(alt: Double, yawDiff: Double) {
        // Function will make the aircraft descend to a certain altitude and change yaw angles
        // Check to see that advanced virtual stick is enabled
        controller.ensureAdvancedVirtualStickMode()
        Log.v("PachKeyManager", "Diving to $alt")
        goToAltitude(alt)
        // Controller yaw angle has a range of [-180, 180] with 0 at North. Positive is CW
        val yawL = if (stateData.yaw!! - yawDiff<-180.0) {
            stateData.yaw!! - yawDiff + 360.0
        } else {
            stateData.yaw!! - yawDiff
        }
        val yawR = if (stateData.yaw!! + yawDiff > 180.0) {
            stateData.yaw!! + yawDiff - 360.0
        } else {
            stateData.yaw!! + yawDiff
        }
        Log.v("PachKeyManager", "Yawing Left to $yawL")
        goToYawAngle(yawL)
        delay(500L)
        Log.v("PachKeyManager", "Yawing Right to $yawR")
        goToYawAngle(yawR)
        delay(500L)
    }



    override fun userJoystickInput(x: Float, y: Float, yaw: Int) {
        // Function will take in user joystick input and send it to the aircraft
        // Check to see that advanced virtual stick is enabled
        val yawDiff = 5.0
        controller.ensureAdvancedVirtualStickMode()
        when (yaw) {
            1 -> {
                val yawR = if (stateData.yaw!! + yawDiff > 180.0) {
                    stateData.yaw!! + yawDiff - 360.0
                } else {
                    stateData.yaw!! + yawDiff
                }
                controller.sendVirtualStickVelocityBody(
                    x.toDouble(),
                    y.toDouble(),
                    yawR,
                    stateData.altitude!!
                )
            }
            -1 -> {
                val yawL = if (stateData.yaw!! - yawDiff<-180.0) {
                    stateData.yaw!! - yawDiff + 360.0
                } else {
                    stateData.yaw!! - yawDiff
                }
                controller.sendVirtualStickVelocityBody(
                    x.toDouble(),
                    y.toDouble(),
                    yawL,
                    stateData.altitude!!
                )
            }
            else -> {
                controller.sendVirtualStickVelocityBody(
                    x.toDouble(),
                    y.toDouble(),
                    stateData.yaw!!,
                    stateData.altitude!!
                )
            }
        }
    }

    // compute distance to target location using lat and lon
    private fun computeLatLonDistance(lat1 : Double, lon1: Double): Double {
        // generally used geo measurement function
        val lat2 = stateData.latitude!!
        val lon2 = stateData.longitude!!
        val dLat = lat2 * Math.PI / 180.0 - lat1 * Math.PI / 180.0;
        val dLon = lon2 * Math.PI / 180.0 - lon1 * Math.PI / 180.0
        val a = sin(dLat/2) * sin(dLat/2) +
                cos(lat1 * Math.PI / 180) * cos(lat2 * Math.PI / 180) *
                Math.sin(dLon/2) * Math.sin(dLon/2)
        val d = 2.0 * atan2(Math.sqrt(a), sqrt(1-a)) * R * 1000.0
        return d // meters
    }

    private fun computeLatDistance(lat1 : Double) : Double{
        // Computes distance in meters between current latitude and target latitude
        val lat2 = stateData.latitude!!
        val dLat = lat2 * Math.PI / 180.0 - lat1 * Math.PI / 180.0;
        val a = sin(dLat/2) * sin(dLat/2)
        val d =  2.0 * atan2(Math.sqrt(a), sqrt(1-a))* R * 1000.0
        return if (lat2>lat1) {
            -d
        } else {
            d
        }
    }

    private fun computeLonDistance(lon1: Double) : Double{
        // Computes distance in meters between current longitude and target longitude.
        // Assumes a constant latitude taken as the current aircraft position
        // Use East as positive direction
        val lat2 = stateData.latitude!!
        val lon2 = stateData.longitude!!
        val dLon = lon2 * Math.PI / 180.0 - lon1 * Math.PI / 180.0
        val a = cos(lat2 * Math.PI / 180) * cos(lat2 * Math.PI / 180) *
                Math.sin(dLon/2) * Math.sin(dLon/2)
        val d = 2.0 * atan2(Math.sqrt(a), sqrt(1-a)) * R * 1000.0
        return if (lon2>lon1) {
            -d
        } else{
            d
        }
    }

    private fun computeYawAngle(lat: Double, lon: Double)
            : Double {
        // Computes yaw angle to target location to keep aircraft facing forward
        val yDiff = lat - stateData.latitude!!
        val xDiff = lon - stateData.longitude!!
        val res = atan2(yDiff, xDiff) *(180 / PI)
        if ((yDiff<0.0) && (xDiff<0.0)) {
            return -(270.0+res)
        }else{
            return 90-res
        }
    }

    fun getConnectionFlowable(): Flowable<Boolean> {
        return connectionDataProcessor.onBackpressureBuffer()
    }

    fun getMessageFlowable(): Flowable<String> {
        return messageDataProcessor.onBackpressureBuffer()
    }

    fun sendStreamDataToServer(stream: Boolean) {
        streamDataProcessor.offer(stream)
    }

    private fun sendDataToStatusWidget(message: String, connection: Boolean) {
        messageDataProcessor.offer(message)
        connectionDataProcessor.offer(connection)
    }


    fun getWaypointFlowable(): Flowable<DJILatLng> {
        return waypointDataProcessor.onBackpressureBuffer()
    }

    fun getStreamerFlowable(): Flowable<Boolean> {
        return streamDataProcessor
    }

    // Function to send data to the data processor
    fun sendWaypointToMap(Data: DJILatLng?) {
        if (Data != null) {
            waypointDataProcessor.offer(Data)
        }
    }
}
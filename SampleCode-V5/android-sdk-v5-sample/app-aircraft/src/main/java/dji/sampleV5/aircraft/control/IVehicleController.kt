package dji.sampleV5.aircraft.control

interface IVehicleController {
    fun changeGimbalAngle(angle: Double)
    fun userJoystickInput(x: Float, y: Float)
}
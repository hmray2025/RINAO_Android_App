package dji.sampleV5.aircraft.telemetry
import android.util.Log
import dji.sampleV5.aircraft.control.PachKeyManager
import dji.sampleV5.modulecommon.settingswidgets.ISartopoWidgetModel
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback
import okhttp3.Call
import okhttp3.Response
import java.io.IOException


class SartopoService : ISartopoWidgetModel{
    companion object {
        @Volatile
        private var instance: SartopoService? = null
        fun getInstance(): SartopoService {
            return instance ?: synchronized(this) {
                instance ?: SartopoService().also { instance = it }
            }
        }
    }
    private var baseUrl = ""
    private var accessCode = ""
    private var deviceID = ""
    fun sendGetRequest(lng: Double, lat: Double) {
        val url = generateURL(lng, lat)
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle the error
                Log.e("SARTopo","Error sending GET Request:\n${e}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle the response
                    val responseBody = response.body()?.string()
                    Log.v("SARTopo","Response:\n${responseBody}")
                }
            }
        })
    }


    override fun setAccessURL(access: String) {
        // Set the URL for the SARTopo service
        accessCode = access
    }
    override fun setDeviceID(ID: String) {
        // Set the device ID for the SARTopo service
        deviceID = ID
    }
    override fun setBaseURL(base: String) {
        // Set the base URL for the SARTopo service
        baseUrl = base
    }
    override fun getAccessURL(): String {
        // Get the URL for the SARTopo service
        return accessCode
    }
    override fun getDeviceID(): String {
        // Get the device ID for the SARTopo service
        return deviceID
    }
    override fun getBaseURL(): String {
        // Get the base URL for the SARTopo service
        return baseUrl
    }

    fun isURLValid(): Boolean {
        // Check if the URL is valid
        if (baseUrl.isEmpty() || accessCode.isEmpty() || deviceID.isEmpty()) {
            Log.e("SarTopo", "base: ${baseUrl}, access: ${accessCode}, device: ${deviceID}")
            return false
        }
        if (!baseUrl.startsWith("https://") && !baseUrl.startsWith("http://")){
            return false
        }
        return true
    }

    private fun generateURL(lng: Double, lat: Double): String {
        // Generate the URL for the SARTopo service
        return "$baseUrl$accessCode?id=$deviceID&lat=${lat}&lng=${lng}"
    }
}
package com.chunkytofustudios.native_geofence

import android.content.Context
import android.util.Log
import com.chunkytofustudios.native_geofence.api.NativeGeofenceApiImpl
import com.chunkytofustudios.native_geofence.generated.NativeBeaconApi
import com.chunkytofustudios.native_geofence.generated.NativeGeofenceApi
import io.flutter.embedding.engine.plugins.FlutterPlugin

class NativeGeofencePlugin : FlutterPlugin {
    private var context: Context? = null

    companion object {
        @JvmStatic
        private val TAG = "NativeGeofencePlugin"
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val apiImpl = NativeGeofenceApiImpl(binding.applicationContext)
        NativeGeofenceApi.setUp(
            binding.binaryMessenger,
            apiImpl
        )
        NativeBeaconApi.setUp(
            binding.binaryMessenger,
            apiImpl
        )
        Log.d(TAG, "NativeGeofenceApi and NativeBeaconApi setup complete.")
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = null
    }
}

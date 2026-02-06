package com.chunkytofustudios.native_geofence

import android.content.Context
import android.util.Log
import com.chunkytofustudios.native_geofence.api.NativeGeofenceApiImpl
import com.chunkytofustudios.native_geofence.generated.NativeBeaconApi
import com.chunkytofustudios.native_geofence.generated.NativeGeofenceApi
import com.chunkytofustudios.native_geofence.util.BeaconNotifier
import com.chunkytofustudios.native_geofence.util.NativeBeaconPersistence
import io.flutter.embedding.engine.plugins.FlutterPlugin
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser

class NativeGeofencePlugin : FlutterPlugin {
    private var context: Context? = null

    companion object {
        @JvmStatic
        private val TAG = "NativeGeofencePlugin"
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val context = binding.applicationContext
        
        // Initialize BeaconNotifier
        val beaconNotifier = BeaconNotifier(context)
        
        // Restore scanner settings if available
        val initialScannerSettings = NativeBeaconPersistence.getScannerSettings(context)
        
        // Configure BeaconManager once
        val beaconManager = BeaconManager.getInstanceForApplication(context).apply {
            // Support iBeacon
            beaconParsers.apply {
                clear()
                add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
            }
            // Register BeaconNotifier as a monitor notifier to receive enter/exit events
            removeAllMonitorNotifiers()
            addMonitorNotifier(beaconNotifier)
            // Register BeaconNotifier as a range notifier to receive RSSI events
            removeAllRangeNotifiers()
            addRangeNotifier(beaconNotifier)

            if (initialScannerSettings != null) {
                foregroundScanPeriod = initialScannerSettings.foregroundScanPeriodMillis
                foregroundBetweenScanPeriod = initialScannerSettings.foregroundBetweenScanPeriodMillis
                backgroundScanPeriod = initialScannerSettings.backgroundScanPeriodMillis
                backgroundBetweenScanPeriod = initialScannerSettings.backgroundBetweenScanPeriodMillis
                try {
                    updateScanPeriods()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed into updateScanPeriods during init: $e")
                }
                Log.d(TAG, "Restored Android scan periods from storage.")
            }
        }
        
        val apiImpl = NativeGeofenceApiImpl(context, beaconManager)
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

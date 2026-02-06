package com.chunkytofustudios.native_geofence.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chunkytofustudios.native_geofence.api.NativeGeofenceApiImpl
import com.chunkytofustudios.native_geofence.util.BeaconNotifier
import com.chunkytofustudios.native_geofence.util.NativeBeaconPersistence
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser

class NativeGeofenceRebootBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "NativeGeofenceRebootBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Boot completed broadcast received. Re-creating geofences and beacons!")
        
        // Initialize BeaconNotifier
        val beaconNotifier = BeaconNotifier(context)
        
        // Restore scanner settings if available
        val initialScannerSettings = NativeBeaconPersistence.getScannerSettings(context)
        
        // Configure BeaconManager
        val beaconManager = BeaconManager.getInstanceForApplication(context).apply {
            // Support iBeacon
            beaconParsers.apply {
                clear()
                add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
            }
            // Register BeaconNotifier
            removeAllMonitorNotifiers()
            addMonitorNotifier(beaconNotifier)
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
        
        NativeGeofenceApiImpl(context, beaconManager).reCreateAfterReboot()
    }
}

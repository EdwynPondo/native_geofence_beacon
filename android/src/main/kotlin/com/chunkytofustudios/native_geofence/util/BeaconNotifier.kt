package com.chunkytofustudios.native_geofence.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.chunkytofustudios.native_geofence.receivers.NativeGeofenceBroadcastReceiver
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region
import java.io.Serializable

class BeaconNotifier(private val context: Context): MonitorNotifier, RangeNotifier  {
    companion object {
        @JvmStatic
        private val TAG = "BeaconNotifier"
    }

    private val beaconManager = BeaconManager.getInstanceForApplication(context)
    // --- MonitorNotifier Implementation ---

    override fun didEnterRegion(region: Region) {
        Log.d(TAG, "didEnterRegion: ${region.uniqueId}")
        // Match iOS: Start ranging to get RSSI, do not broadcast Enter yet.
        beaconManager.startRangingBeacons(region)
    }

    override fun didExitRegion(region: Region) {
        Log.d(TAG, "didExitRegion: ${region.uniqueId}")
        // Match iOS: Stop ranging and broadcast Exit.
        beaconManager.stopRangingBeacons(region)
        triggerBeaconBroadcast(region, MonitorNotifier.OUTSIDE)
    }

    override fun didDetermineStateForRegion(state: Int, region: Region) {
        Log.d(TAG, "didDetermineStateForRegion: $state for ${region.uniqueId}")
    }

    // --- RangeNotifier Implementation ---

    override fun didRangeBeaconsInRegion(beacons: MutableCollection<org.altbeacon.beacon.Beacon>?, region: Region?) {
        if (beacons != null && region != null && beacons.isNotEmpty()) {
            val beacon = beacons.first()
            Log.d(TAG, "didRangeBeaconsInRegion: ${region.uniqueId}, rssi=${beacon.rssi}")
            // Match iOS: Send Enter event with RSSI, then stop ranging immediately.
            triggerBeaconBroadcast(region, MonitorNotifier.INSIDE, beacon.rssi)
            beaconManager.stopRangingBeacons(region)
        }
    }

    private fun triggerBeaconBroadcast(region: Region, state: Int, rssi: Int? = null) {
        val intent = Intent(context, NativeGeofenceBroadcastReceiver::class.java).apply {
            putExtra("state", state)
            putExtra("org.altbeacon.beacon.Region", region as Serializable)
            if (rssi != null) {
                putExtra("rssi", rssi)
            }
        }
        context.sendBroadcast(intent)
    }
}
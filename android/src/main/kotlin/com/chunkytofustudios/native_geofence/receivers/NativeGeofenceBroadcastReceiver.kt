package com.chunkytofustudios.native_geofence.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.chunkytofustudios.native_geofence.Constants
import com.chunkytofustudios.native_geofence.NativeGeofenceBackgroundWorker
import com.chunkytofustudios.native_geofence.generated.ActiveBeaconWire
import com.chunkytofustudios.native_geofence.generated.BeaconCallbackParamsWire
import com.chunkytofustudios.native_geofence.generated.BeaconEvent
import com.chunkytofustudios.native_geofence.generated.GeofenceCallbackParamsWire
import com.chunkytofustudios.native_geofence.model.BeaconCallbackParamsStorage
import com.chunkytofustudios.native_geofence.model.GeofenceCallbackParamsStorage
import com.chunkytofustudios.native_geofence.util.ActiveGeofenceWires
import com.chunkytofustudios.native_geofence.util.GeofenceEvents
import com.chunkytofustudios.native_geofence.util.LocationWires
import com.chunkytofustudios.native_geofence.util.NativeBeaconPersistence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.serialization.json.Json
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.Region

class NativeGeofenceBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NativeGeofenceBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Broadcast received.")

        // 1. Check if it's a Geofence event
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent != null && !geofencingEvent.hasError()) {
            handleGeofenceEvent(context, intent, geofencingEvent)
            return
        }

        // 2. Check if it's a Beacon event
        // Using string literals for keys to avoid issues with missing Constants
        // "state" and "org.altbeacon.beacon.Region" are the standard extras for AltBeacon Monitoring
        if (intent.hasExtra("state") && intent.hasExtra("org.altbeacon.beacon.Region")) {
            handleBeaconEvent(context, intent)
            return
        }

        Log.w(TAG, "Broadcast received but no geofence or beacon data found. Extras: ${intent.extras?.keySet()}")
    }

    private fun handleGeofenceEvent(context: Context, intent: Intent, geofencingEvent: GeofencingEvent) {
        val params = getGeofenceCallbackParams(intent, geofencingEvent) ?: return
        val jsonData = Json.encodeToString(GeofenceCallbackParamsStorage.fromWire(params))
        enqueueWork(context, jsonData, Constants.GEOFENCE_CALLBACK_WORK_GROUP)
    }

    private fun handleBeaconEvent(context: Context, intent: Intent) {
        val params = getBeaconCallbackParams(context, intent) ?: return
        val jsonData = Json.encodeToString(BeaconCallbackParamsStorage.fromWire(params))
        enqueueWork(context, jsonData, Constants.BEACON_CALLBACK_WORK_GROUP)
    }

    private fun enqueueWork(context: Context, jsonData: String, workGroup: String) {
        val workRequest = OneTimeWorkRequestBuilder<NativeGeofenceBackgroundWorker>()
            .setInputData(Data.Builder().putString(Constants.WORKER_PAYLOAD_KEY, jsonData).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
 
        WorkManager.getInstance(context).beginUniqueWork(
            workGroup,
            ExistingWorkPolicy.APPEND,
            workRequest
        ).enqueue()
    }

    private fun getGeofenceCallbackParams(intent: Intent, geofencingEvent: GeofencingEvent): GeofenceCallbackParamsWire? {
        val callbackHandle = intent.getLongExtra(Constants.CALLBACK_HANDLE_KEY, 0)
        if (callbackHandle == 0L) {
            Log.e(TAG, "GeofencingEvent callback handle is missing.")
            return null
        }

        val geofenceEvent = GeofenceEvents.fromInt(geofencingEvent.geofenceTransition) ?: return null
        val triggeringGeofences = geofencingEvent.triggeringGeofences?.map {
            ActiveGeofenceWires.fromGeofence(it)
        } ?: return null

        return GeofenceCallbackParamsWire(
            triggeringGeofences,
            geofenceEvent,
            geofencingEvent.triggeringLocation?.let { LocationWires.fromLocation(it) },
            callbackHandle
        )
    }

    private fun getBeaconCallbackParams(context: Context, intent: Intent): BeaconCallbackParamsWire? {
        val state = intent.getIntExtra("state", -1)
        val region = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("org.altbeacon.beacon.Region", Region::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("org.altbeacon.beacon.Region") as? Region
        }
        
        if (region == null || state == -1) {
            Log.e(TAG, "Beacon event region or state missing. Region: $region, State: $state")
            return null
        }

        val event = when (state) {
            MonitorNotifier.INSIDE -> BeaconEvent.ENTER
            MonitorNotifier.OUTSIDE -> BeaconEvent.EXIT
            else -> {
                Log.w(TAG, "Unknown beacon state: $state")
                return null
            }
        }

        val rssi = intent.getIntExtra("rssi", 0).takeIf { it != 0 }?.toLong()

        val beaconWire = NativeBeaconPersistence.getAllBeacons(context).find { it.id == region.uniqueId } ?: return null
        if (!beaconWire.triggers.contains(event)) return null

        return BeaconCallbackParamsWire(
            listOf(ActiveBeaconWire(
                beaconWire.id, beaconWire.uuid, beaconWire.major, beaconWire.minor,
                rssi,
                beaconWire.triggers, beaconWire.androidSettings
            )),
            event,
            beaconWire.callbackHandle
        )
    }
}

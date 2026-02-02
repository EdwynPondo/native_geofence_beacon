package com.chunkytofustudios.native_geofence.api

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.chunkytofustudios.native_geofence.Constants
import com.chunkytofustudios.native_geofence.generated.ActiveBeaconWire
import com.chunkytofustudios.native_geofence.generated.ActiveGeofenceWire
import com.chunkytofustudios.native_geofence.generated.BeaconWire
import com.chunkytofustudios.native_geofence.generated.FlutterError
import com.chunkytofustudios.native_geofence.generated.GeofenceWire
import com.chunkytofustudios.native_geofence.generated.NativeBeaconApi
import com.chunkytofustudios.native_geofence.generated.NativeGeofenceApi
import com.chunkytofustudios.native_geofence.generated.NativeGeofenceErrorCode
import com.chunkytofustudios.native_geofence.util.GeofenceEvents
import com.chunkytofustudios.native_geofence.receivers.NativeGeofenceBroadcastReceiver
import com.chunkytofustudios.native_geofence.util.ActiveBeaconWires
import com.chunkytofustudios.native_geofence.util.ActiveGeofenceWires
import com.chunkytofustudios.native_geofence.util.GeofenceWires
import com.chunkytofustudios.native_geofence.util.NativeBeaconPersistence
import com.chunkytofustudios.native_geofence.util.NativeGeofencePersistence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.Region
import java.io.Serializable

class NativeGeofenceApiImpl(private val context: Context) : NativeGeofenceApi, NativeBeaconApi, MonitorNotifier, org.altbeacon.beacon.RangeNotifier {
    companion object {
        @JvmStatic
        private val TAG = "NativeGeofenceApiImpl"
    }

    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val enteredBeaconRegions = mutableSetOf<String>()
    private val beaconManager = BeaconManager.getInstanceForApplication(context).apply {
        // Support iBeacon
        beaconParsers.add(org.altbeacon.beacon.BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
        
        // Register this instance as a monitor notifier to receive enter/exit events
        addMonitorNotifier(this@NativeGeofenceApiImpl)
        // Register this instance as a range notifier to receive RSSI events
        addRangeNotifier(this@NativeGeofenceApiImpl)
    }

    override fun initialize(callbackDispatcherHandle: Long) {
        context.getSharedPreferences(Constants.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            .edit()
            .putLong(Constants.CALLBACK_DISPATCHER_HANDLE_KEY, callbackDispatcherHandle)
            .putLong(Constants.BEACON_CALLBACK_DISPATCHER_HANDLE_KEY, callbackDispatcherHandle)
            .apply()
        Log.d(TAG, "Initialized consolidated NativeGeofenceApi and NativeBeaconApi.")
    }

    // --- NativeGeofenceApi Implementation ---

    override fun createGeofence(
        geofence: GeofenceWire,
        callback: (Result<Unit>) -> Unit
    ) {
        createGeofenceHelper(geofence, true, callback)
    }

    override fun reCreateAfterReboot() {
        val geofences = NativeGeofencePersistence.getAllGeofences(context)
        for (geofence in geofences) {
            createGeofenceHelper(geofence, false, null)
        }
        
        val beacons = NativeBeaconPersistence.getAllBeacons(context)
        for (beacon in beacons) {
            createBeaconHelper(beacon, false, null)
        }
        
        Log.d(TAG, "${geofences.size} geofences and ${beacons.size} beacons re-created.")
    }

    override fun getGeofenceIds(): List<String> {
        return NativeGeofencePersistence.getAllGeofenceIds(context)
    }

    override fun getGeofences(): List<ActiveGeofenceWire> {
        val geofences = NativeGeofencePersistence.getAllGeofences(context)
        return geofences.map { ActiveGeofenceWires.fromGeofenceWire(it) }.toList()
    }

    override fun removeGeofenceById(id: String, callback: (Result<Unit>) -> Unit) {
        geofencingClient.removeGeofences(listOf(id)).run {
            addOnSuccessListener {
                NativeGeofencePersistence.removeGeofence(context, id)
                Log.d(TAG, "Removed Geofence ID=$id.")
                callback.invoke(Result.success(Unit))
            }
            addOnFailureListener {
                val existingIds = NativeGeofencePersistence.getAllGeofenceIds(context)
                val errorCode =
                    if (existingIds.contains(id)) NativeGeofenceErrorCode.PLUGIN_INTERNAL else NativeGeofenceErrorCode.GEOFENCE_NOT_FOUND
                Log.e(TAG, "Failure when removing Geofence ID=$id: $it")
                callback.invoke(
                    Result.failure(
                        FlutterError(
                            errorCode.raw.toString(),
                            it.toString()
                        )
                    )
                )
            }
        }
    }

    override fun removeAllGeofences(callback: (Result<Unit>) -> Unit) {
        geofencingClient.removeGeofences(getGeofencePendingIndent(context, null)).run {
            addOnSuccessListener {
                NativeGeofencePersistence.removeAllGeofences(context)
                Log.d(TAG, "Removed all geofences (if any).")
                callback.invoke(Result.success(Unit))
            }
            addOnFailureListener {
                Log.e(TAG, "Failed to remove all geofences: $it")
                callback.invoke(
                    Result.failure(
                        FlutterError(
                            NativeGeofenceErrorCode.PLUGIN_INTERNAL.raw.toString(),
                            it.toString()
                        )
                    )
                )
            }
        }
    }

    // --- NativeBeaconApi Implementation ---

    override fun createBeacon(
        beacon: BeaconWire,
        callback: (Result<Unit>) -> Unit
    ) {
        createBeaconHelper(beacon, true, callback)
    }

    override fun getBeaconIds(): List<String> {
        return NativeBeaconPersistence.getAllBeaconIds(context)
    }

    override fun getBeacons(): List<ActiveBeaconWire> {
        val beacons = NativeBeaconPersistence.getAllBeacons(context)
        return beacons.map { ActiveBeaconWires.fromBeaconWire(it) }.toList()
    }

    override fun removeBeaconById(id: String, callback: (Result<Unit>) -> Unit) {
        try {
            // Find the region to stop monitoring. AltBeacon uses Region objects.
            val allBeacons = NativeBeaconPersistence.getAllBeacons(context)
            val beaconToRemove = allBeacons.find { it.id == id }
            
            if (beaconToRemove == null) {
                callback.invoke(Result.failure(FlutterError(NativeGeofenceErrorCode.BEACON_NOT_FOUND.raw.toString(), "Beacon not found")))
                return
            }

            // AltBeacon stopMonitoring
            // NOTE: We'll need a way to rebuild the Region object or store it.
            // For now, we'll just remove it from persistence and stopMonitoring if we can.
            // Better implementation would keep a mapping of ID to Region.
            val region = Region(id, null, null, null)
            beaconManager.stopMonitoring(region)
            beaconManager.stopRangingBeacons(region)
            
            NativeBeaconPersistence.removeBeacon(context, id)
            Log.d(TAG, "Removed Beacon ID=$id.")
            callback.invoke(Result.success(Unit))
        } catch (e: Exception) {
            callback.invoke(Result.failure(FlutterError(NativeGeofenceErrorCode.PLUGIN_INTERNAL.raw.toString(), e.toString())))
        }
    }

    override fun removeAllBeacons(callback: (Result<Unit>) -> Unit) {
        try {
            val ids = NativeBeaconPersistence.getAllBeaconIds(context)
            for (id in ids) {
                val region = Region(id, null, null, null)
                beaconManager.stopMonitoring(region)
                beaconManager.stopRangingBeacons(region)
            }
            NativeBeaconPersistence.removeAllBeacons(context)
            Log.d(TAG, "Removed all beacons.")
            callback.invoke(Result.success(Unit))
        } catch (e: Exception) {
            callback.invoke(Result.failure(FlutterError(NativeGeofenceErrorCode.PLUGIN_INTERNAL.raw.toString(), e.toString())))
        }
    }

    // --- Helper Methods ---

    private fun getGeofencePendingIndent(
        context: Context,
        callbackHandle: Long?
    ): PendingIntent {
        val intent = Intent(context, NativeGeofenceBroadcastReceiver::class.java)
        if (callbackHandle != null) {
            intent.putExtra(Constants.CALLBACK_HANDLE_KEY, callbackHandle)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun createGeofenceHelper(
        geofence: GeofenceWire,
        cache: Boolean,
        callback: ((Result<Unit>) -> Unit)?
    ) {
        // We try to create the Geofence without checking for permissions.
        // Only if creation fails we will alert the Flutter plugin of the permission issue.
        geofencingClient.addGeofences(
            GeofencingRequest.Builder().apply {
                setInitialTrigger(GeofenceEvents.createMask(geofence.androidSettings.initialTriggers))
                addGeofence(GeofenceWires.toGeofence(geofence))
            }.build(),
            getGeofencePendingIndent(context, geofence.callbackHandle)
        ).run {
            addOnSuccessListener {
                if (cache) {
                    NativeGeofencePersistence.saveGeofence(context, geofence)
                }
                Log.d(TAG, "Successfully added Geofence ID=${geofence.id}.")
                callback?.invoke(Result.success(Unit))
            }
            addOnFailureListener {
                Log.e(TAG, "Failed to add Geofence ID=${geofence.id}: $it")

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Lacking permission: ACCESS_FINE_LOCATION")
                    callback?.invoke(
                        Result.failure(
                            FlutterError(
                                NativeGeofenceErrorCode.MISSING_LOCATION_PERMISSION.raw.toString(),
                                "The ACCESS_FINE_LOCATION needs to be granted in order to setup geofences."
                            )
                        )
                    )
                    return@addOnFailureListener
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.e(TAG, "Running on API ${Build.VERSION.SDK_INT} and lacking permission: ACCESS_BACKGROUND_LOCATION")
                        callback?.invoke(
                            Result.failure(
                                FlutterError(
                                    NativeGeofenceErrorCode.MISSING_BACKGROUND_LOCATION_PERMISSION.raw.toString(),
                                    "The ACCESS_BACKGROUND_LOCATION needs to be granted in order to setup geofences.",
                                    "Running on Android API ${Build.VERSION.SDK_INT}."
                                )
                            )
                        )
                        return@addOnFailureListener
                    }
                }

                callback?.invoke(
                    Result.failure(
                        FlutterError(
                            NativeGeofenceErrorCode.PLUGIN_INTERNAL.raw.toString(),
                            it.toString()
                        )
                    )
                )
            }
        }
    }

    private fun createBeaconHelper(
        beacon: BeaconWire,
        cache: Boolean,
        callback: ((Result<Unit>) -> Unit)?
    ) {
        try {
            // Check Bluetooth permissions
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // For simplified implementation, we'll just check Manifest.permission.BLUETOOTH on older versions
                // and BLUETOOTH_SCAN on 31+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                     if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                         callback?.invoke(Result.failure(FlutterError(NativeGeofenceErrorCode.MISSING_BLUETOOTH_PERMISSION.raw.toString(), "Missing BLUETOOTH_SCAN permission")))
                         return
                     }
                }
            }

            // AltBeacon monitoring
            // We need a Region object
            // uuid major minor are nullable in BeaconWire? 
            // In iBeacon: Region(id, uuid, major, minor)
            // If major is null, it's a wildcard.
            val region = Region(
                beacon.id,
                org.altbeacon.beacon.Identifier.parse(beacon.uuid),
                beacon.major?.let { org.altbeacon.beacon.Identifier.fromInt(it.toInt()) },
                beacon.minor?.let { org.altbeacon.beacon.Identifier.fromInt(it.toInt()) }
            )

            beaconManager.startMonitoring(region)
            // Remove startRangingBeacons here to match iOS behavior:
            // Ranging starts only when we actually enter the region.
            
            if (cache) {
                NativeBeaconPersistence.saveBeacon(context, beacon)
            }
            
            Log.d(TAG, "Successfully started monitoring Beacon ID=${beacon.id}.")
            callback?.invoke(Result.success(Unit))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start monitoring Beacon ID=${beacon.id}: $e")
            callback?.invoke(Result.failure(FlutterError(NativeGeofenceErrorCode.PLUGIN_INTERNAL.raw.toString(), e.toString())))
        }
    }

    // --- MonitorNotifier Implementation ---

    override fun didEnterRegion(region: Region) {
        if (enteredBeaconRegions.contains(region.uniqueId)) {
            Log.d(TAG, "Ignoring duplicate didEnterRegion for ${region.uniqueId}")
            return
        }
        enteredBeaconRegions.add(region.uniqueId)
        Log.d(TAG, "didEnterRegion: ${region.uniqueId}")
        // Match iOS: Start ranging to get RSSI, do not broadcast Enter yet.
        beaconManager.startRangingBeacons(region)
    }

    override fun didExitRegion(region: Region) {
        enteredBeaconRegions.remove(region.uniqueId)
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

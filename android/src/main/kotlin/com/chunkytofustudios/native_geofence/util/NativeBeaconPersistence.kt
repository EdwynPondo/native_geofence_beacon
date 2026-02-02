package com.chunkytofustudios.native_geofence.util

import android.content.Context
import android.util.Log
import com.chunkytofustudios.native_geofence.Constants
import com.chunkytofustudios.native_geofence.generated.AndroidScannerSettingsWire
import com.chunkytofustudios.native_geofence.generated.BeaconWire
import com.chunkytofustudios.native_geofence.model.AndroidScannerSettingsStorage
import com.chunkytofustudios.native_geofence.model.BeaconStorage
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class NativeBeaconPersistence {
    companion object {
        @JvmStatic
        private val TAG = "NativeBeaconPersistence"

        @JvmStatic
        private val sharedPreferencesLock = Object()

        @JvmStatic
        private fun getBeaconKey(id: String): String {
            return Constants.PERSISTENT_BEACON_KEY_PREFIX + id
        }

        @JvmStatic
        fun saveBeacon(context: Context, beacon: BeaconWire) {
            synchronized(sharedPreferencesLock) {
                val p = context.getSharedPreferences(
                    Constants.SHARED_PREFERENCES_KEY,
                    Context.MODE_PRIVATE
                )
                val jsonData = Json.encodeToString(BeaconStorage.fromWire(beacon))
                var persistentBeacons =
                    p.getStringSet(Constants.PERSISTENT_BEACONS_IDS_KEY, null)
                persistentBeacons = if (persistentBeacons == null) {
                    HashSet<String>()
                } else {
                    HashSet<String>(persistentBeacons)
                }
                persistentBeacons.add(beacon.id)
                context.getSharedPreferences(Constants.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                    .edit()
                    .putStringSet(Constants.PERSISTENT_BEACONS_IDS_KEY, persistentBeacons)
                    .putString(getBeaconKey(beacon.id), jsonData)
                    .apply()
                Log.d(TAG, "Saved Beacon ID=${beacon.id} to storage.")
            }
        }

        @JvmStatic
        fun getAllBeaconIds(context: Context): List<String> {
            synchronized(sharedPreferencesLock) {
                val p = context.getSharedPreferences(
                    Constants.SHARED_PREFERENCES_KEY,
                    Context.MODE_PRIVATE
                )
                val persistentBeacons =
                    p.getStringSet(Constants.PERSISTENT_BEACONS_IDS_KEY, null)
                        ?: return emptyList()
                Log.d(TAG, "There are ${persistentBeacons.size} Beacons saved.")
                return persistentBeacons.toList()
            }
        }

        @JvmStatic
        fun getAllBeacons(context: Context): List<BeaconWire> {
            synchronized(sharedPreferencesLock) {
                val p = context.getSharedPreferences(
                    Constants.SHARED_PREFERENCES_KEY,
                    Context.MODE_PRIVATE
                )
                val persistentBeacons =
                    p.getStringSet(Constants.PERSISTENT_BEACONS_IDS_KEY, null)
                        ?: return emptyList()

                val result = mutableListOf<BeaconWire>()
                for (id in persistentBeacons) {
                    val jsonData = p.getString(getBeaconKey(id), null)
                    if (jsonData == null) {
                        Log.e(TAG, "No data found for Beacon ID=${id} in storage.")
                        continue
                    }
                    try {
                        val beaconStorage = Json.decodeFromString<BeaconStorage>(jsonData)
                        result.add(beaconStorage.toWire())
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Failed to parse Beacon ID=${id} from storage. Data=${jsonData}"
                        )
                    }
                }
                Log.d(TAG, "Retrieved ${result.size} Beacons from storage.")
                return result
            }
        }

        @JvmStatic
        fun removeBeacon(context: Context, beaconId: String) {
            synchronized(sharedPreferencesLock) {
                val p = context.getSharedPreferences(
                    Constants.SHARED_PREFERENCES_KEY,
                    Context.MODE_PRIVATE
                )
                var persistentBeacons =
                    p.getStringSet(Constants.PERSISTENT_BEACONS_IDS_KEY, null)
                persistentBeacons = if (persistentBeacons == null) {
                    HashSet<String>()
                } else {
                    HashSet<String>(persistentBeacons)
                }
                persistentBeacons.remove(beaconId)
                context.getSharedPreferences(Constants.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                    .edit()
                    .putStringSet(Constants.PERSISTENT_BEACONS_IDS_KEY, persistentBeacons)
                    .remove(getBeaconKey(beaconId))
                    .apply()
                Log.d(TAG, "Removed Beacon ID=${beaconId} from storage.")
            }
        }

        @JvmStatic
        fun removeAllBeacons(context: Context) {
            synchronized(sharedPreferencesLock) {
                val p = context.getSharedPreferences(
                    Constants.SHARED_PREFERENCES_KEY,
                    Context.MODE_PRIVATE
                )
                var persistentBeacons =
                    p.getStringSet(Constants.PERSISTENT_BEACONS_IDS_KEY, null)
                persistentBeacons = if (persistentBeacons == null) {
                    HashSet<String>()
                } else {
                    HashSet<String>(persistentBeacons)
                }
                val editor = context.getSharedPreferences(
                    Constants.SHARED_PREFERENCES_KEY,
                    Context.MODE_PRIVATE
                )
                    .edit()
                    .remove(Constants.PERSISTENT_BEACONS_IDS_KEY)
                for (id in persistentBeacons) {
                    editor.remove(getBeaconKey(id))
                }
                editor.apply()
                Log.d(TAG, "Removed ${persistentBeacons.size} Beacons from storage.")
                editor.apply()
                Log.d(TAG, "Removed ${persistentBeacons.size} Beacons from storage.")
            }
        }

        @JvmStatic
        fun saveScannerSettings(context: Context, settings: AndroidScannerSettingsWire) {
            synchronized(sharedPreferencesLock) {
                val jsonData = Json.encodeToString(AndroidScannerSettingsStorage.fromWire(settings))
                context.getSharedPreferences(Constants.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                    .edit()
                    .putString(Constants.PERSISTENT_SCANNER_SETTINGS_KEY, jsonData)
                    .apply()
                Log.d(TAG, "Saved Scanner Settings to storage.")
            }
        }

        @JvmStatic
        fun getScannerSettings(context: Context): AndroidScannerSettingsWire? {
            synchronized(sharedPreferencesLock) {
                val p = context.getSharedPreferences(
                    Constants.SHARED_PREFERENCES_KEY,
                    Context.MODE_PRIVATE
                )
                val jsonData = p.getString(Constants.PERSISTENT_SCANNER_SETTINGS_KEY, null)
                if (jsonData == null) {
                    return null
                }
                return try {
                    val settingsStorage = Json.decodeFromString<AndroidScannerSettingsStorage>(jsonData)
                    settingsStorage.toWire()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse Scanner Settings from storage. Data=${jsonData}")
                    null
                }
            }
        }
    }
}

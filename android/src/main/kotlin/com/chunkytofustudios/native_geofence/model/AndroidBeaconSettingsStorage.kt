package com.chunkytofustudios.native_geofence.model

import com.chunkytofustudios.native_geofence.generated.AndroidBeaconSettingsWire
import com.chunkytofustudios.native_geofence.generated.BeaconEvent
import kotlinx.serialization.Serializable

@Serializable
class AndroidBeaconSettingsStorage(
    private val initialTriggers: List<Int>,
    private val scanPeriodMillis: Long,
    private val betweenScanPeriodMillis: Long
) {
    companion object {
        fun fromWire(e: AndroidBeaconSettingsWire): AndroidBeaconSettingsStorage {
            return AndroidBeaconSettingsStorage(
                e.initialTriggers.map { it.raw },
                e.scanPeriodMillis,
                e.betweenScanPeriodMillis
            )
        }
    }

    fun toWire(): AndroidBeaconSettingsWire {
        return AndroidBeaconSettingsWire(
            initialTriggers.map { BeaconEvent.ofRaw(it)!! },
            scanPeriodMillis,
            betweenScanPeriodMillis
        )
    }
}

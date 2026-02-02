package com.chunkytofustudios.native_geofence.model

import com.chunkytofustudios.native_geofence.generated.AndroidBeaconSettingsWire
import com.chunkytofustudios.native_geofence.generated.BeaconEvent
import kotlinx.serialization.Serializable

@Serializable
class AndroidBeaconSettingsStorage(
    private val initialTriggers: List<Int>
) {
    companion object {
        fun fromWire(e: AndroidBeaconSettingsWire): AndroidBeaconSettingsStorage {
            return AndroidBeaconSettingsStorage(
                e.initialTriggers.map { it.raw }
            )
        }
    }

    fun toWire(): AndroidBeaconSettingsWire {
        return AndroidBeaconSettingsWire(
            initialTriggers.map { BeaconEvent.ofRaw(it)!! }
        )
    }
}

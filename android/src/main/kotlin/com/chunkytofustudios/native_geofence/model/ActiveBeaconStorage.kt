package com.chunkytofustudios.native_geofence.model

import com.chunkytofustudios.native_geofence.generated.ActiveBeaconWire
import com.chunkytofustudios.native_geofence.generated.BeaconEvent
import kotlinx.serialization.Serializable

@Serializable
class ActiveBeaconStorage(
    private val id: String,
    private val uuid: String,
    private val major: Long? = null,
    private val minor: Long? = null,
    private val rssi: Long? = null,
    private val triggers: List<Int>,
    private val androidSettings: AndroidBeaconSettingsStorage? = null
) {
    companion object {
        fun fromWire(e: ActiveBeaconWire): ActiveBeaconStorage {
            return ActiveBeaconStorage(
                e.id,
                e.uuid,
                e.major,
                e.minor,
                e.rssi,
                e.triggers.map { it.raw },
                e.androidSettings?.let { AndroidBeaconSettingsStorage.fromWire(it) }
            )
        }
    }

    fun toWire(): ActiveBeaconWire {
        return ActiveBeaconWire(
            id,
            uuid,
            major,
            minor,
            rssi,
            triggers.map { BeaconEvent.ofRaw(it)!! },
            androidSettings?.toWire()
        )
    }
}

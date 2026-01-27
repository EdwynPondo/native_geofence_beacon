package com.chunkytofustudios.native_geofence.model

import com.chunkytofustudios.native_geofence.generated.BeaconWire
import com.chunkytofustudios.native_geofence.generated.BeaconEvent
import kotlinx.serialization.Serializable

@Serializable
class BeaconStorage(
    private val id: String,
    private val uuid: String,
    private val major: Long? = null,
    private val minor: Long? = null,
    private val triggers: List<Int>,
    private val iosSettings: IosBeaconSettingsStorage,
    private val androidSettings: AndroidBeaconSettingsStorage,
    private val callbackHandle: Long
) {
    companion object {
        fun fromWire(e: BeaconWire): BeaconStorage {
            return BeaconStorage(
                e.id,
                e.uuid,
                e.major,
                e.minor,
                e.triggers.map { it.raw },
                IosBeaconSettingsStorage.fromWire(e.iosSettings),
                AndroidBeaconSettingsStorage.fromWire(e.androidSettings),
                e.callbackHandle
            )
        }
    }

    fun toWire(): BeaconWire {
        return BeaconWire(
            id,
            uuid,
            major,
            minor,
            triggers.map { BeaconEvent.ofRaw(it)!! },
            iosSettings.toWire(),
            androidSettings.toWire(),
            callbackHandle
        )
    }
}

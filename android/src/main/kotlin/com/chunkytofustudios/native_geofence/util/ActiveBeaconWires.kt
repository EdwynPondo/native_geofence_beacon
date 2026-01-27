package com.chunkytofustudios.native_geofence.util

import com.chunkytofustudios.native_geofence.generated.ActiveBeaconWire
import com.chunkytofustudios.native_geofence.generated.BeaconWire

class ActiveBeaconWires {
    companion object {
        fun fromBeaconWire(e: BeaconWire): ActiveBeaconWire {
            return ActiveBeaconWire(
                e.id,
                e.uuid,
                e.major,
                e.minor,
                e.triggers,
                e.androidSettings
            )
        }
    }
}

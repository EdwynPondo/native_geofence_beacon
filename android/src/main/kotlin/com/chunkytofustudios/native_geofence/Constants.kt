package com.chunkytofustudios.native_geofence

class Constants {
    companion object {
        private const val PACKAGE_NAME = "com.chunkytofustudios.native_geofence"

        const val SHARED_PREFERENCES_KEY = "native_geofence_plugin_cache"
        const val PERSISTENT_GEOFENCES_IDS_KEY = "persistent_geofences_ids"
        const val PERSISTENT_GEOFENCE_KEY_PREFIX = "persistent_geofence/"
        const val PERSISTENT_BEACONS_IDS_KEY = "persistent_beacons_ids"
        const val PERSISTENT_BEACON_KEY_PREFIX = "persistent_beacon/"
        const val PERSISTENT_SCANNER_SETTINGS_KEY = "persistent_scanner_settings"

        const val CALLBACK_HANDLE_KEY = "$PACKAGE_NAME.callback_handle"
        const val CALLBACK_DISPATCHER_HANDLE_KEY = "callback_dispatch_handler"
        const val BEACON_CALLBACK_DISPATCHER_HANDLE_KEY = "beacon_callback_dispatch_handler"

        const val ACTION_SHUTDOWN = "SHUTDOWN"

        const val WORKER_PAYLOAD_KEY = "$PACKAGE_NAME.worker_payload"
        const val GEOFENCE_CALLBACK_WORK_GROUP = "geofence_callback_work_group"
        const val BEACON_CALLBACK_WORK_GROUP = "beacon_callback_work_group"

        const val ISOLATE_HOLDER_WAKE_LOCK_TAG = "$PACKAGE_NAME:wake_lock"
    }
}

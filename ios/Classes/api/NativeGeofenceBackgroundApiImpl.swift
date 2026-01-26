import CoreLocation
import Flutter
import OSLog

class NativeGeofenceBackgroundApiImpl: NativeGeofenceBackgroundApi, NativeBeaconBackgroundApi {
    private let log = Logger(subsystem: Constants.PACKAGE_NAME, category: "NativeGeofenceBackgroundApiImpl")
    
    private let binaryMessenger: FlutterBinaryMessenger
    
    private var geofenceEventQueue: [GeofenceCallbackParamsWire] = .init()
    private var beaconEventQueue: [BeaconCallbackParamsWire] = .init()
    private var isClosed: Bool = false
    private var nativeGeofenceTriggerApi: NativeGeofenceTriggerApi? = nil
    private var nativeBeaconTriggerApi: NativeBeaconTriggerApi? = nil
    private var cleanup: (() -> Void)? = nil
    
    init(binaryMessenger: FlutterBinaryMessenger) {
        self.binaryMessenger = binaryMessenger
    }
    
    // MARK: - NativeGeofenceBackgroundApi
    
    func geofenceTriggered(params: GeofenceCallbackParamsWire, cleanup: @escaping () -> Void) {
        objc_sync_enter(self)
        
        geofenceEventQueue.append(params)
        self.cleanup = cleanup
        
        objc_sync_exit(self)
        
        guard let nativeGeofenceTriggerApi else {
            log.debug("Waiting for NativeGeofenceTriggerApi to become available...")
            return
        }
        processQueues()
    }
    
    func triggerApiInitialized() throws {
        objc_sync_enter(self)
        
        if (nativeGeofenceTriggerApi == nil) {
            nativeGeofenceTriggerApi = NativeGeofenceTriggerApi(binaryMessenger: binaryMessenger)
            log.debug("NativeGeofenceTriggerApi setup complete.")
        }
        if (nativeBeaconTriggerApi == nil) {
            nativeBeaconTriggerApi = NativeBeaconTriggerApi(binaryMessenger: binaryMessenger)
            log.debug("NativeBeaconTriggerApi setup complete.")
        }
        
        objc_sync_exit(self)
        
       if geofenceEventQueue.isEmpty && beaconEventQueue.isEmpty {
            log.debug("Waiting for geofence or beacon event...")
            return
        }
        processQueues()
    }
    
    func promoteToForeground() throws {
        log.info("promoteToForeground called. iOS does not distinguish between foreground and background, nothing to do here.")
    }
    
    func demoteToBackground() throws {
        log.info("demoteToBackground called. iOS does not distinguish between foreground and background, nothing to do here.")
    }
    
    // MARK: - NativeBeaconBackgroundApi
    
    func beaconTriggered(params: BeaconCallbackParamsWire, cleanup: @escaping () -> Void) {
        objc_sync_enter(self)
        
        beaconEventQueue.append(params)
        self.cleanup = cleanup
        
        objc_sync_exit(self)
        
        guard let nativeBeaconTriggerApi else {
            log.debug("Waiting for NativeBeaconTriggerApi to become available...")
            return
        }
        processQueues()
    }
    
    // MARK: - Private Methods
    
    private func processQueues() {
        objc_sync_enter(self)
        defer { objc_sync_exit(self) }
        
        if isClosed {
            log.error("NativeGeofenceBackgroundApi already closed, ignoring additional events.")
            return
        }
        
        // Process geofence events first
        if !geofenceEventQueue.isEmpty {
            let params = geofenceEventQueue.removeFirst()
            log.debug("Queue dispatch: sending geofence trigger event for IDs=[\(NativeGeofenceBackgroundApiImpl.geofenceIds(params))].")
            callGeofenceTriggerApi(params: params)
            return
        }
        
        // Then process beacon events
        if !beaconEventQueue.isEmpty {
            let params = beaconEventQueue.removeFirst()
            log.debug("Queue dispatch: sending beacon trigger event for IDs=[\(NativeGeofenceBackgroundApiImpl.beaconIds(params))].")
            callBeaconTriggerApi(params: params)
            return
        }
        
        // Now that both event queues are empty we can cleanup and de-allocate this class.
        cleanup?()
        isClosed = true
    }
    
    private func callGeofenceTriggerApi(params: GeofenceCallbackParamsWire) {
        guard let api = nativeGeofenceTriggerApi else {
            log.error("NativeGeofenceTriggerApi was nil, this should not happen.")
            return
        }
        log.debug("Calling Dart callback to process geofence trigger for IDs=[\(NativeGeofenceBackgroundApiImpl.geofenceIds(params))] event=\(String(describing: params.event)).")
        api.geofenceTriggered(params: params, completion: { result in
            if case .success = result {
                self.log.debug("Geofence trigger event for IDs=[\(NativeGeofenceBackgroundApiImpl.geofenceIds(params))] processed successfully.")
            } else {
                self.log.error("Geofence trigger event for IDs=[\(NativeGeofenceBackgroundApiImpl.geofenceIds(params))] failed.")
            }
            // Now that the callback is complete we can process the next item in the queue, if any.
            self.processQueues()
        })
    }
    
    private func callBeaconTriggerApi(params: BeaconCallbackParamsWire) {
        guard let api = nativeBeaconTriggerApi else {
            log.error("NativeBeaconTriggerApi was nil, this should not happen.")
            return
        }
        log.debug("Calling Dart callback to process beacon trigger for IDs=[\(NativeGeofenceBackgroundApiImpl.beaconIds(params))] event=\(String(describing: params.event)).")
        api.beaconTriggered(params: params, completion: { result in
            if case .success = result {
                self.log.debug("Beacon trigger event for IDs=[\(NativeGeofenceBackgroundApiImpl.beaconIds(params))] processed successfully.")
            } else {
                self.log.error("Beacon trigger event for IDs=[\(NativeGeofenceBackgroundApiImpl.beaconIds(params))] failed.")
            }
            // Now that the callback is complete we can process the next item in the queue, if any.
            self.processQueues()
        })
    }
    
    private static func geofenceIds(_ params: GeofenceCallbackParamsWire) -> String {
        let ids: [String] = params.geofences.map(\.id)
        return ids.joined(separator: ",")
    }
    
    private static func beaconIds(_ params: BeaconCallbackParamsWire) -> String {
        let ids: [String] = params.beacons.map(\.id)
        return ids.joined(separator: ",")
    }
}

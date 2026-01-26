import CoreLocation
import Flutter
import OSLog

// Singleton class
class LocationManagerDelegate: NSObject, CLLocationManagerDelegate {
    // Prevent multiple instances of CLLocationManager to avoid duplicate triggers.
    private static var sharedLocationManager: CLLocationManager?
    
    private let log = Logger(subsystem: Constants.PACKAGE_NAME, category: "LocationManagerDelegate")
    
    private let flutterPluginRegistrantCallback: FlutterPluginRegistrantCallback?
    let locationManager: CLLocationManager
    
    private var headlessFlutterEngine: FlutterEngine? = nil
    private var nativeBackgroundApi: NativeGeofenceBackgroundApiImpl? = nil
    
    init(flutterPluginRegistrantCallback: FlutterPluginRegistrantCallback?) {
        self.flutterPluginRegistrantCallback = flutterPluginRegistrantCallback
        locationManager = LocationManagerDelegate.sharedLocationManager ?? CLLocationManager()
        LocationManagerDelegate.sharedLocationManager = locationManager
        
        super.init()
        locationManager.delegate = self
        
        log.debug("LocationManagerDelegate created with instance ID=\(Int.random(in: 1 ... 1000000)).")
    }
    
    func locationManager(_ manager: CLLocationManager, didDetermineState state: CLRegionState, for region: CLRegion) {
        // Check if this is a beacon region
        if region is CLBeaconRegion {
            handleBeaconRegionStateChange(state: state, region: region)
        } else {
            handleGeofenceRegionStateChange(state: state, region: region)
        }
    }
    
    private func handleGeofenceRegionStateChange(state: CLRegionState, region: CLRegion) {
        log.debug("didDetermineState: \\(String(describing: state)) for geofence ID: \\(region.identifier)")
        
        guard let event: GeofenceEvent = switch state {
        case .unknown: nil
        case .inside: .enter
        case .outside: .exit
        } else {
            log.error("Unknown CLRegionState: \\(String(describing: state))")
            return
        }
        
        guard let activeGeofence = ActiveGeofenceWires.fromRegion(region) else {
            log.error("Unknown CLRegion type: \\(String(describing: type(of: region)))")
            return
        }

        if !activeGeofence.triggers.contains(event) {
            return
        }
        
        guard let callbackHandle = NativeGeofencePersistence.getRegionCallbackHandle(id: activeGeofence.id) else {
            log.error("Callback handle for region \\(activeGeofence.id) not found.")
            return
        }
        
        let params = GeofenceCallbackParamsWire(geofences: [activeGeofence], event: event, callbackHandle: callbackHandle)
        
        guard let backgroundApi = nativeBackgroundApi ?? createFlutterEngine() else {
            return
        }
        
        // Shutdown the engine once the event is handled
        func cleanup() {
            nativeBackgroundApi = nil
            headlessFlutterEngine?.destroyContext()
            headlessFlutterEngine = nil
            log.debug("Flutter engine cleanup complete.")
        }
        
        nativeBackgroundApi!.geofenceTriggered(params: params, cleanup: cleanup)
        log.debug("Geofence trigger event sent.")
    }
    
    private func handleBeaconRegionStateChange(state: CLRegionState, region: CLRegion) {
        log.debug("didDetermineState: \\(String(describing: state)) for beacon ID: \\(region.identifier)")
        
        guard let event: BeaconEvent = switch state {
        case .unknown: nil
        case .inside: .enter
        case .outside: .exit
        } else {
            log.error("Unknown CLRegionState: \\(String(describing: state))")
            return
        }
        
        guard let activeBeacon = ActiveBeaconWires.fromRegion(region) else {
            log.error("Unknown CLRegion type: \\(String(describing: type(of: region)))")
            return
        }

        if !activeBeacon.triggers.contains(event) {
            return
        }
        
        guard let callbackHandle = NativeBeaconPersistence.getRegionCallbackHandle(id: activeBeacon.id) else {
            log.error("Callback handle for beacon \\(activeBeacon.id) not found.")
            return
        }
        
        let params = BeaconCallbackParamsWire(beacons: [activeBeacon], event: event, callbackHandle: callbackHandle)
        
        guard let backgroundApi = nativeBackgroundApi ?? createFlutterEngine() else {
            return
        }
        
        // Shutdown the engine once the event is handled
        func cleanup() {
            nativeBackgroundApi = nil
            headlessFlutterEngine?.destroyContext()
            headlessFlutterEngine = nil
            log.debug("Flutter engine cleanup complete.")
        }
        
        nativeBackgroundApi!.beaconTriggered(params: params, cleanup: cleanup)
        log.debug("Beacon trigger event sent.")
    }
    
    func locationManager(_ manager: CLLocationManager, monitoringDidFailFor region: CLRegion?, withError error: any Error) {
        log.error("monitoringDidFailFor: \\(region?.identifier ?? \"nil\") withError: \\(error)")
    }
    
    private func createFlutterEngine() -> NativeGeofenceBackgroundApiImpl? {
        // Create a Flutter engine
        headlessFlutterEngine = FlutterEngine(name: Constants.HEADLESS_FLUTTER_ENGINE_NAME, project: nil, allowHeadlessExecution: true)
        log.debug("A new headless Flutter engine has been created.")
        
        // Use geofence callback dispatcher (both geofence and beacon use same dispatcher)
        guard let callbackDispatcherHandle = NativeGeofencePersistence.getCallbackDispatcherHandle() else {
            log.error("Callback dispatcher not found in UserDefaults.")
            return nil
        }
        
        guard let callbackDispatcherInfo = FlutterCallbackCache.lookupCallbackInformation(callbackDispatcherHandle) else {
            log.error("Callback dispatcher not found.")
            return nil
        }
        
        // Start the engine at the specified callback method.
        headlessFlutterEngine!.run(withEntrypoint: callbackDispatcherInfo.callbackName, libraryURI: callbackDispatcherInfo.callbackLibraryPath)
        flutterPluginRegistrantCallback?(headlessFlutterEngine!)
        log.debug("Flutter engine started and plugins registered.")
        
        // Setup unified background API that handles both geofence and beacon events
        nativeBackgroundApi = NativeGeofenceBackgroundApiImpl(binaryMessenger: headlessFlutterEngine!.binaryMessenger)
        NativeGeofenceBackgroundApiSetup.setUp(binaryMessenger: headlessFlutterEngine!.binaryMessenger, api: nativeBackgroundApi)
        NativeBeaconBackgroundApiSetup.setUp(binaryMessenger: headlessFlutterEngine!.binaryMessenger, api: nativeBackgroundApi)
        log.debug("NativeGeofenceBackgroundApi and NativeBeaconBackgroundApi initialized.")

        return nativeBackgroundApi
    }
}

import CoreLocation

class ActiveBeaconWires {
    static func fromRegion(_ region: CLRegion, rssi: Int? = nil) -> ActiveBeaconWire? {
        guard let beaconRegion = region as? CLBeaconRegion else { return nil }
        
        return ActiveBeaconWire(
            id: beaconRegion.identifier,
            uuid: beaconRegion.uuid.uuidString,
            major: beaconRegion.major?.int64Value,
            minor: beaconRegion.minor?.int64Value,
            rssi: rssi != nil ? Int64(rssi!) : nil,
            triggers: [
                beaconRegion.notifyOnEntry ? .enter : nil,
                beaconRegion.notifyOnExit ? .exit : nil
            ].compactMap { $0 },
            androidSettings: nil
        )
    }
}

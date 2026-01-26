import CoreLocation

class ActiveBeaconWires {
    static func fromRegion(_ region: CLRegion) -> ActiveBeaconWire? {
        guard let beaconRegion = region as? CLBeaconRegion else { return nil }
        
        return ActiveBeaconWire(
            id: beaconRegion.identifier,
            uuid: beaconRegion.uuid.uuidString,
            major: beaconRegion.major?.int64Value,
            minor: beaconRegion.minor?.int64Value,
            triggers: [
                beaconRegion.notifyOnEntry ? .enter : nil,
                beaconRegion.notifyOnExit ? .exit : nil
            ].compactMap { $0 }
        )
    }
}

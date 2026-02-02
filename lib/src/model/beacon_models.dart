import 'package:native_geofence/src/generated/platform_bindings.g.dart';

/// iOS specific Beacon settings.
class IosBeaconSettings {
  /// Whether a beacon event should trigger immediately when the beacon is
  /// added.
  /// For example, setting this to true will trigger a [BeaconEvent.enter]
  /// event if the user is already in range of the beacon.
  /// Don't worry: This initial trigger only happens when the beacon is
  /// created and NOT every time the plugin is initialized.
  final bool initialTrigger;

  /// Whether to notify when the device's display is on and the user is in
  /// range of the beacon.
  final bool notifyEntryStateOnDisplay;

  const IosBeaconSettings({
    this.initialTrigger = false,
    this.notifyEntryStateOnDisplay = false,
  });

  @override
  String toString() {
    return 'IosBeaconSettings(initialTrigger: $initialTrigger, '
        'notifyEntryStateOnDisplay: $notifyEntryStateOnDisplay)';
  }
}

/// Android specific Beacon settings.
class AndroidBeaconSettings {
  /// Sets the beacon behavior at the moment when the beacons are added.
  /// For example, listing [BeaconEvent.enter] here will trigger the Beacon
  /// immediately if the user is already in range of the beacon.
  /// Don't worry: This initial trigger only happens when the beacon is
  /// created and NOT every time the plugin is initialized.
  final Set<BeaconEvent> initialTriggers;

  const AndroidBeaconSettings({
    required this.initialTriggers,
  });

  @override
  String toString() {
    return 'AndroidBeaconSettings('
        'initialTriggers: [${initialTriggers.map((e) => e.name).join(',')}])';
  }
}

/// Android specific Scanner settings.
class AndroidScannerSettings {
  /// The duration to scan for beacons when the app is in the foreground.
  final Duration foregroundScanPeriod;

  /// The duration to wait between beacon scans when the app is in the foreground.
  final Duration foregroundBetweenScanPeriod;

  /// The duration to scan for beacons when the app is in the background.
  final Duration backgroundScanPeriod;

  /// The duration to wait between beacon scans when the app is in the background.
  final Duration backgroundBetweenScanPeriod;

  const AndroidScannerSettings({
    this.foregroundScanPeriod = const Duration(milliseconds: 1100),
    this.foregroundBetweenScanPeriod = const Duration(seconds: 0),
    this.backgroundScanPeriod = const Duration(milliseconds: 1100),
    this.backgroundBetweenScanPeriod = const Duration(seconds: 0),
  });

  @override
  String toString() {
    return 'AndroidScannerSettings('
        'foregroundScanPeriod: ${foregroundScanPeriod.inMilliseconds}ms, '
        'foregroundBetweenScanPeriod: ${foregroundBetweenScanPeriod.inMilliseconds}ms, '
        'backgroundScanPeriod: ${backgroundScanPeriod.inMilliseconds}ms, '
        'backgroundBetweenScanPeriod: ${backgroundBetweenScanPeriod.inMilliseconds}ms)';
  }
}

/// A Bluetooth beacon region.
class Beacon {
  /// The ID associated with the beacon.
  ///
  /// This ID is used to identify the beacon and is required to delete a
  /// specific beacon.
  /// Creating two beacons with the same ID will result in the first beacon
  /// being overwritten.
  final String id;

  /// The UUID of the beacon.
  ///
  /// This is the proximity UUID for iBeacon format.
  final String uuid;

  /// The major value of the beacon (optional).
  ///
  /// If null, all beacons with the specified UUID will be monitored.
  final int? major;

  /// The minor value of the beacon (optional).
  ///
  /// If null, all beacons with the specified UUID and major value will be
  /// monitored. Requires [major] to be set.
  final int? minor;

  /// The types of beacon events to listen for.
  final Set<BeaconEvent> triggers;

  /// iOS specific settings.
  final IosBeaconSettings iosSettings;

  /// Android specific settings.
  final AndroidBeaconSettings androidSettings;

  const Beacon({
    required this.id,
    required this.uuid,
    this.major,
    this.minor,
    required this.triggers,
    required this.iosSettings,
    required this.androidSettings,
  });

  @override
  String toString() {
    return 'Beacon('
        'id: $id, '
        'uuid: $uuid, '
        'major: $major, '
        'minor: $minor, '
        'triggers: [${triggers.map((e) => e.name).join(',')}], '
        'iosSettings: $iosSettings, '
        'androidSettings: $androidSettings)';
  }
}

/// A Beacon that is registered and is actively being tracked.
///
/// This type is a subset of [Beacon] that is returned by the plugin for GET
/// calls.
///
/// See the [Beacon] class for field details.
///
/// Note: [IosBeaconSettings] is not provided due to platform constraints.
class ActiveBeacon {
  /// The ID associated with the beacon.
  final String id;

  /// The UUID of the beacon.
  final String uuid;

  /// The major value of the beacon (optional).
  final int? major;

  /// The minor value of the beacon (optional).
  final int? minor;

  /// The RSSI value of the beacon (optional).
  final int? rssi;

  /// The types of beacon events to listen for.
  final Set<BeaconEvent> triggers;

  /// Only available on Android.
  ///
  /// The [initialTriggers] field will always be an empty set because Android
  /// does not provide this information when a Beacon triggers.
  final AndroidBeaconSettings? androidSettings;

  ActiveBeacon({
    required this.id,
    required this.uuid,
    this.major,
    this.minor,
    this.rssi,
    required this.triggers,
    required this.androidSettings,
  });

  @override
  String toString() {
    return 'ActiveBeacon('
        'id: $id, '
        'uuid: $uuid, '
        'major: $major, '
        'minor: $minor, '
        'rssi: $rssi, '
        'triggers: [${triggers.map((e) => e.name).join(',')}], '
        'androidSettings: $androidSettings)';
  }
}

/// The parameters passed to the beacon callback handler.
class BeaconCallbackParams {
  /// The beacons that triggered the event.
  /// The list might contain multiple elements on Android.
  final List<ActiveBeacon> beacons;

  /// The type of beacon event.
  final BeaconEvent event;

  const BeaconCallbackParams({
    required this.beacons,
    required this.event,
  });

  @override
  String toString() {
    return 'BeaconCallbackParams('
        'beacons: [${beacons.map((e) => e.toString()).join(', ')}], '
        'event: ${event.name})';
  }
}

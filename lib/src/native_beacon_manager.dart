import 'dart:async';
import 'dart:ui';

import 'package:native_geofence/src/callback_dispatcher.dart';
import 'package:native_geofence/src/generated/platform_bindings.g.dart';
import 'package:native_geofence/src/model/beacon_models.dart';
import 'package:native_geofence/src/model/model_mapper.dart';
import 'package:native_geofence/src/model/native_geofence_exception.dart';
import 'package:native_geofence/src/typedefs.dart';

class NativeBeaconManager {
  /// Cached instance of [NativeBeaconManager]
  static NativeBeaconManager? _instance;

  /// The singleton instance of [NativeBeaconManager].
  ///
  /// Throws [NativeGeofenceException].
  static NativeBeaconManager get instance {
    try {
      _instance ??= NativeBeaconManager._();
    } catch (e, stackTrace) {
      throw NativeGeofenceExceptionMapper.fromError(e, stackTrace);
    }
    return _instance!;
  }

  final NativeBeaconApi _api;

  NativeBeaconManager._() : _api = NativeBeaconApi();

  /// Initialize the plugin.
  ///
  /// Must be called before any other method.
  ///
  /// Throws [NativeGeofenceException].
  Future<void> initialize() async {
    final CallbackHandle? callback;
    try {
      callback = PluginUtilities.getCallbackHandle(callbackDispatcher);
    } catch (e, stackTrace) {
      throw NativeGeofenceExceptionMapper.fromError(e, stackTrace);
    }
    if (callback == null) {
      throw NativeGeofenceException.internal(
          message: 'Callback dispatcher is invalid.');
    }
    return _api
        .initialize(callbackDispatcherHandle: callback.toRawHandle())
        .catchError(NativeGeofenceExceptionMapper.catchError<void>);
  }

  /// Register for beacon events for a [Beacon].
  ///
  /// [beacon] is the beacon region to register with the system.
  /// [callback] is the method to be called when a beacon event associated
  /// with [beacon] occurs.
  ///
  /// Throws [NativeGeofenceException].
  Future<void> createBeacon(Beacon beacon, BeaconCallback callback) async {
    if (beacon.id.isEmpty) {
      throw NativeGeofenceException.invalidArgument(
          message: 'Beacon ID cannot be empty.');
    }
    if (beacon.triggers.isEmpty) {
      throw NativeGeofenceException.invalidArgument(
          message: 'Beacon triggers cannot be empty.');
    }
    if (beacon.uuid.isEmpty) {
      throw NativeGeofenceException.invalidArgument(
          message: 'Beacon UUID cannot be empty.');
    }
    // Validate UUID format (basic check)
    final uuidRegex = RegExp(
        r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$');
    if (!uuidRegex.hasMatch(beacon.uuid)) {
      throw NativeGeofenceException.invalidArgument(
          message: 'Beacon UUID format is invalid. Expected format: '
              'XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX');
    }
    if (beacon.major != null && (beacon.major! < 0 || beacon.major! > 65535)) {
      throw NativeGeofenceException.invalidArgument(
          message: 'Beacon major value must be between 0 and 65535.');
    }
    if (beacon.minor != null && (beacon.minor! < 0 || beacon.minor! > 65535)) {
      throw NativeGeofenceException.invalidArgument(
          message: 'Beacon minor value must be between 0 and 65535.');
    }
    if (beacon.minor != null && beacon.major == null) {
      throw NativeGeofenceException.invalidArgument(
          message: 'Beacon minor value requires major value to be set.');
    }
    final CallbackHandle? callbackHandle;
    try {
      callbackHandle = PluginUtilities.getCallbackHandle(callback);
    } catch (e, stackTrace) {
      throw NativeGeofenceExceptionMapper.fromError(e, stackTrace);
    }
    if (callbackHandle == null) {
      throw NativeGeofenceException.invalidArgument(
          message: 'Callback is invalid.');
    }
    return _api
        .createBeacon(beacon: beacon.toWire(callbackHandle.toRawHandle()))
        .catchError(NativeGeofenceExceptionMapper.catchError<void>);
  }

  /// Re-register beacons after reboot.
  ///
  /// Optional: This function can be called when the autostart feature is not
  /// working as it should (e.g. for some Android OEMs). This way you can ensure
  /// all Beacons are re-created at app launch.
  ///
  /// Throws [NativeGeofenceException].
  Future<void> reCreateAfterReboot() async => _api
      .reCreateAfterReboot()
      .catchError(NativeGeofenceExceptionMapper.catchError<void>);

  /// Get all registered [Beacon] IDs.
  ///
  /// If there are no beacons registered it returns an empty list.
  ///
  /// Throws [NativeGeofenceException].
  Future<List<String>> getRegisteredBeaconIds() async => _api
      .getBeaconIds()
      .catchError(NativeGeofenceExceptionMapper.catchError<List<String>>);

  /// Get all [Beacon] regions and their properties.
  ///
  /// If there are no beacons registered it returns an empty list.
  ///
  /// Throws [NativeGeofenceException].
  Future<List<ActiveBeacon>> getRegisteredBeacons() async => _api
      .getBeacons()
      .then((value) => value.map((e) => e.fromWire()).toList())
      .catchError(NativeGeofenceExceptionMapper.catchError<List<Beacon>>);

  /// Stop receiving beacon events for a given [Beacon].
  ///
  /// If the [Beacon] is not registered, this method does nothing.
  ///
  /// Throws [NativeGeofenceException]. Might throw
  /// [NativeGeofenceErrorCode.beaconNotFound] on Android.
  Future<void> removeBeacon(Beacon beacon) async => removeBeaconById(beacon.id);

  /// Stop receiving beacon events for an identifier associated with a
  /// beacon region.
  ///
  /// If a [Beacon] with the given ID is not registered, this method does
  /// nothing.
  ///
  /// Throws [NativeGeofenceException]. Might throw
  /// [NativeGeofenceErrorCode.beaconNotFound] on Android.
  Future<void> removeBeaconById(String id) async => _api
      .removeBeaconById(id: id)
      .catchError(NativeGeofenceExceptionMapper.catchError<void>);

  /// Stop receiving beacon events for all registered beacons.
  ///
  /// If there are no beacons registered, this method does nothing.
  ///
  /// Throws [NativeGeofenceException].
  Future<void> removeAllBeacons() async => _api
      .removeAllBeacons()
      .catchError(NativeGeofenceExceptionMapper.catchError<void>);

  /// Configure Android beacon scanner settings.
  ///
  /// This method is only available on Android. On iOS it does nothing.
  ///
  /// Throws [NativeGeofenceException].
  Future<void> configureAndroidMonitor(
    AndroidScannerSettings settings,
  ) async =>
      _api
          .configureAndroidMonitor(settings: settings.toWire())
          .catchError(NativeGeofenceExceptionMapper.catchError<void>);
}

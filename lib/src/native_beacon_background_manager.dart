import 'dart:async';

import 'package:native_geofence/src/generated/platform_bindings.g.dart';
import 'package:native_geofence/src/model/model_mapper.dart';
import 'package:native_geofence/src/model/native_geofence_exception.dart';

class NativeBeaconBackgroundManager {
  static NativeBeaconBackgroundManager? _instance;

  /// The singleton instance of [NativeBeaconBackgroundManager].
  ///
  /// WARNING: Can only be accessed within Beacon callbacks. Trying to access
  /// this anywhere else will throw an [AssertionError].
  static NativeBeaconBackgroundManager get instance {
    assert(
        _instance != null,
        'NativeBeaconBackgroundManager has not been initialized yet; '
        'Are you running within a Beacon callback?');
    return _instance!;
  }

  final NativeBeaconBackgroundApi _api;

  NativeBeaconBackgroundManager._(this._api);

  /// Promote the beacon callback to an Android foreground service.
  ///
  /// Android only, has no effect on iOS (but is safe to call).
  ///
  /// Throws [NativeGeofenceException].
  Future<void> promoteToForeground() async => _api
      .promoteToForeground()
      .catchError(NativeGeofenceExceptionMapper.catchError<void>);

  /// Demote the beacon service from an Android foreground service to a
  /// background service.
  ///
  /// Android only, has no effect on iOS (but is safe to call).
  ///
  /// Throws [NativeGeofenceException].
  Future<void> demoteToBackground() async => _api
      .demoteToBackground()
      .catchError(NativeGeofenceExceptionMapper.catchError<void>);
}

/// Private method internal to plugin, do not use.
Future<void> createNativeBeaconBackgroundManagerInstance() async {
  final api = NativeBeaconBackgroundApi();
  NativeBeaconBackgroundManager._instance =
      NativeBeaconBackgroundManager._(api);
  await api.triggerApiInitialized();
}

import 'package:native_geofence/src/generated/platform_bindings.g.dart';
import 'package:native_geofence/src/model/model_mapper.dart';
import 'package:native_geofence/src/model/native_geofence_exception.dart';

class NativeBeaconBackgroundManager {
  /// Cached instance of [NativeBeaconBackgroundManager]
  static NativeBeaconBackgroundManager? _instance;

  /// The singleton instance of [NativeBeaconBackgroundManager].
  ///
  /// Throws [NativeGeofenceException].
  static NativeBeaconBackgroundManager get instance {
    try {
      _instance ??= NativeBeaconBackgroundManager._();
    } catch (e, stackTrace) {
      throw NativeGeofenceExceptionMapper.fromError(e, stackTrace);
    }
    return _instance!;
  }

  final NativeBeaconBackgroundApi _api;

  NativeBeaconBackgroundManager._() : _api = NativeBeaconBackgroundApi();

  /// Initialize the background API.
  ///
  /// This should be called from within the callback dispatcher.
  ///
  /// Throws [NativeGeofenceException].
  Future<void> initialize() async {
    try {
      _api.triggerApiInitialized();
    } catch (e, stackTrace) {
      throw NativeGeofenceExceptionMapper.fromError(e, stackTrace);
    }
  }

  /// Promote the background isolate to foreground.
  ///
  /// This can be used to show a foreground notification when a beacon event
  /// occurs in the background.
  ///
  /// Throws [NativeGeofenceException].
  Future<void> promoteToForeground() async {
    try {
      _api.promoteToForeground();
    } catch (e, stackTrace) {
      throw NativeGeofenceExceptionMapper.fromError(e, stackTrace);
    }
  }

  /// Demote the foreground isolate to background.
  ///
  /// This should be called after [promoteToForeground] when you no longer
  /// need the foreground notification.
  ///
  /// Throws [NativeGeofenceException].
  Future<void> demoteToBackground() async {
    try {
      _api.demoteToBackground();
    } catch (e, stackTrace) {
      throw NativeGeofenceExceptionMapper.fromError(e, stackTrace);
    }
  }
}

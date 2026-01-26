import 'dart:ui';

import 'package:flutter/material.dart';

import 'package:native_geofence/src/generated/platform_bindings.g.dart';
import 'package:native_geofence/src/model/model_mapper.dart';
import 'package:native_geofence/src/model/native_geofence_exception.dart';
import 'package:native_geofence/src/typedefs.dart';

class NativeBeaconTriggerImpl implements NativeBeaconTriggerApi {
  /// Cached instance of [NativeBeaconTriggerImpl]
  static NativeBeaconTriggerImpl? _instance;

  static void ensureInitialized() {
    _instance ??= NativeBeaconTriggerImpl._();
  }

  NativeBeaconTriggerImpl._() {
    NativeBeaconTriggerApi.setUp(this);
  }

  @override
  Future<void> beaconTriggered(BeaconCallbackParamsWire params) async {
    final Function? callback = PluginUtilities.getCallbackFromHandle(
        CallbackHandle.fromRawHandle(params.callbackHandle));
    if (callback == null) {
      throw NativeGeofenceException(
          code: NativeGeofenceErrorCode.callbackNotFound);
    }
    if (callback is! BeaconCallback) {
      throw NativeGeofenceException(
          code: NativeGeofenceErrorCode.callbackInvalid,
          message: 'Invalid callback type: ${callback.runtimeType.toString()}',
          details: 'Expected: BeaconCallback');
    }
    await callback(params.fromWire());
    debugPrint('Beacon trigger callback completed.');
  }
}

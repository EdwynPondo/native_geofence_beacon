import 'package:native_geofence/src/model/beacon_models.dart';
import 'package:native_geofence/src/model/model.dart';

typedef GeofenceCallback = Future<void> Function(GeofenceCallbackParams params);

typedef BeaconCallback = Future<void> Function(BeaconCallbackParams params);

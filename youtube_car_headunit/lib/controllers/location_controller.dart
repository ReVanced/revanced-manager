import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';

enum LocationStatus { unknown, serviceOff, denied, deniedForever, granted }

/// The car's position for the maps: permission state and a live position stream.
///
/// Permission is only requested when the driver taps "Izinkan", never on startup.
/// Tracking stops while the app is in the background, because nothing shows the map
/// then and GPS costs battery, unless [setBackgroundTracking] is on (navigation).
class LocationController extends ChangeNotifier with WidgetsBindingObserver {
  LocationController({this.enabled = true}) {
    if (enabled) WidgetsBinding.instance.addObserver(this);
  }

  /// False where the location plugin is absent (widget tests).
  final bool enabled;

  LocationStatus _status = LocationStatus.unknown;
  LatLng? _position;
  double? _heading;
  double _speed = 0;
  double? _accuracy;
  StreamSubscription<Position>? _subscription;
  bool _keepInBackground = false;
  bool _injected = false;
  bool _disposed = false;

  LocationStatus get status => _status;

  LatLng? get position => _position;

  /// Travel direction in degrees from north; null when standing still or unknown.
  double? get heading => _heading;

  /// Ground speed in m/s; 0 when unknown.
  double get speed => _speed;

  /// Horizontal accuracy radius of the last fix in meters; null when unknown.
  double? get accuracy => _accuracy;

  /// Keeps tracking while the app is in the background, behind a "Navigasi aktif"
  /// notification: Android stops a backgrounded app's location updates otherwise, and
  /// guidance must go on while another app is in front.
  Future<void> setBackgroundTracking(bool on) async {
    if (_keepInBackground == on) return;
    _keepInBackground = on;
    // The notification belongs to the stream's settings, so the stream is restarted.
    if (_subscription != null) {
      _stopTracking();
      await _startTracking();
    }
  }

  /// Reads the current permission without prompting and starts tracking if granted.
  Future<void> init() => _refresh(request: false);

  /// Prompts for permission (the driver asked for it), then starts tracking.
  Future<void> requestPermission() => _refresh(request: true);

  /// Opens the system screen that can fix the current blocker.
  Future<void> openSettings() async {
    if (!enabled) return;
    try {
      if (_status == LocationStatus.serviceOff) {
        await Geolocator.openLocationSettings();
      } else {
        await Geolocator.openAppSettings();
      }
    } catch (e) {
      debugPrint('Cannot open location settings: $e');
    }
  }

  Future<void> _refresh({required bool request}) async {
    if (!enabled) return;
    try {
      if (!await Geolocator.isLocationServiceEnabled()) {
        _setStatus(LocationStatus.serviceOff);
        return;
      }
      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied && request) {
        permission = await Geolocator.requestPermission();
      }
      switch (permission) {
        case LocationPermission.always:
        case LocationPermission.whileInUse:
          _setStatus(LocationStatus.granted);
          await _startTracking();
        case LocationPermission.deniedForever:
          _setStatus(LocationStatus.deniedForever);
        case LocationPermission.denied:
        case LocationPermission.unableToDetermine:
          _setStatus(LocationStatus.denied);
      }
    } catch (e) {
      debugPrint('Location unavailable: $e');
      _setStatus(LocationStatus.denied);
    }
  }

  Future<void> _startTracking() async {
    if (_subscription != null) return;
    final last = await Geolocator.getLastKnownPosition();
    if (last != null) _onPosition(last);
    _subscription = Geolocator.getPositionStream(
      // Without an interval Android delivers a fix only every 5 s, so the car would jump
      // across the map while driving; navigation apps use one per second.
      locationSettings: AndroidSettings(
        accuracy: LocationAccuracy.high,
        distanceFilter: 5,
        intervalDuration: const Duration(seconds: 1),
        foregroundNotificationConfig: _keepInBackground
            ? const ForegroundNotificationConfig(
                notificationTitle: 'Navigasi aktif',
                notificationText: 'Panduan rute tetap berjalan.',
                notificationChannelName: 'Navigasi',
                enableWakeLock: true,
                setOngoing: true,
              )
            : null,
      ),
    ).listen(
      _onPosition,
      onError: (Object e) => debugPrint('Position stream error: $e'),
    );
  }

  void _stopTracking() {
    _subscription?.cancel();
    _subscription = null;
  }

  void _onPosition(Position p) {
    if (_injected) return;
    _position = LatLng(p.latitude, p.longitude);
    _speed = p.speed > 0 ? p.speed : 0;
    _accuracy = p.accuracy > 0 ? p.accuracy : null;
    // GPS heading is noise below walking speed.
    _heading = p.speed > 1.5 && p.heading >= 0 ? p.heading : null;
    _notify();
  }

  /// Feeds a position as if GPS had delivered it, for tests and the dev-only drive
  /// simulator. Real fixes are ignored until [stopInjecting], so a phone standing still
  /// on the desk cannot pull the simulated car back.
  void injectPosition(
    LatLng position, {
    double? heading,
    double speed = 0,
    double accuracy = 5,
  }) {
    _injected = true;
    _position = position;
    _heading = heading;
    _speed = speed;
    _accuracy = accuracy;
    _status = LocationStatus.granted;
    _notify();
  }

  /// Returns to real GPS fixes after [injectPosition].
  void stopInjecting() => _injected = false;

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused) {
      if (!_keepInBackground) _stopTracking();
    } else if (state == AppLifecycleState.resumed) {
      // Also picks up a permission granted in system settings meanwhile.
      _refresh(request: false);
    }
  }

  void _setStatus(LocationStatus status) {
    if (_status == status) return;
    _status = status;
    _notify();
  }

  void _notify() {
    if (!_disposed) notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    _stopTracking();
    if (enabled) WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }
}

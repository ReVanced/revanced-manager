import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Android-side helpers on one method channel, implemented in `MainActivity.kt`:
/// picture-in-picture and sending the app to the background.
///
/// A singleton because the channel accepts one Dart handler; a second instance would
/// replace the first one's PiP-state callback.
class PlatformBridge {
  PlatformBridge._internal() {
    _channel.setMethodCallHandler(_handleMethodCall);
  }

  static final PlatformBridge _instance = PlatformBridge._internal();

  factory PlatformBridge() => _instance;

  static const MethodChannel _channel =
      MethodChannel('com.carheadunit.youtube_car_headunit/pip');

  final ValueNotifier<bool> isInPip = ValueNotifier<bool>(false);

  Future<void> _handleMethodCall(MethodCall call) async {
    if (call.method == 'onPipModeChanged' && call.arguments is bool) {
      isInPip.value = call.arguments as bool;
    }
  }

  Future<bool> enterPip() async {
    try {
      return await _channel.invokeMethod<bool>('enterPip') ?? false;
    } catch (_) {
      return false;
    }
  }

  Future<void> setAutoPip(bool enabled) async {
    try {
      await _channel.invokeMethod('setAutoPip', {'enabled': enabled});
    } catch (_) {}
  }

  /// Moves the task behind other apps without finishing it, like Home does, so playback
  /// and the app state survive. Back on the dashboard uses this instead of closing.
  Future<void> minimize() async {
    try {
      await _channel.invokeMethod('minimize');
    } catch (_) {
      await SystemNavigator.pop();
    }
  }
}

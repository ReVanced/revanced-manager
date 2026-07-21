import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

class PipService {
  static final PipService _instance = PipService._internal();
  factory PipService() => _instance;
  PipService._internal() {
    _channel.setMethodCallHandler(_handleMethodCall);
  }

  static const MethodChannel _channel =
      MethodChannel('com.carheadunit.youtube_car_headunit/pip');

  final ValueNotifier<bool> isInPip = ValueNotifier<bool>(false);

  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onPipModeChanged':
        if (call.arguments is bool) {
          isInPip.value = call.arguments as bool;
        }
        break;
      default:
        break;
    }
  }

  Future<bool> isPipSupported() async {
    try {
      final bool supported =
          await _channel.invokeMethod('isPipSupported') ?? false;
      return supported;
    } catch (_) {
      return false;
    }
  }

  Future<bool> enterPip() async {
    try {
      final bool success = await _channel.invokeMethod('enterPip') ?? false;
      return success;
    } catch (_) {
      return false;
    }
  }

  Future<void> setAutoPip(bool enabled) async {
    try {
      await _channel.invokeMethod('setAutoPip', {'enabled': enabled});
    } catch (_) {}
  }
}

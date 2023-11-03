import 'package:flutter/services.dart';

void hapticSwitch(bool value) {
  if (value) {
    HapticFeedback.mediumImpact();
  } else {
    HapticFeedback.lightImpact();
  }
}

void hapticCheckbox() {
  HapticFeedback.lightImpact();
}

void hapticRadio() {
  HapticFeedback.lightImpact();
}

void hapticButton() {
  HapticFeedback.lightImpact();
}

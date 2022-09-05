import 'package:flutter/material.dart';
import 'package:injectable/injectable.dart';
import 'package:revanced_manager/ui/views/home/home_view.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_view.dart';
import 'package:revanced_manager/ui/views/settings/settings_view.dart';
import 'package:stacked/stacked.dart';

@lazySingleton
class NavigationViewModel extends IndexTrackingViewModel {
  Widget getViewForIndex(int index) {
    switch (index) {
      case 0:
        return const HomeView();
      case 1:
        return const PatcherView();
      case 2:
        return SettingsView();
      default:
        return const HomeView();
    }
  }
}

// GENERATED CODE - DO NOT MODIFY BY HAND

// **************************************************************************
// StackedRouterGenerator
// **************************************************************************

// ignore_for_file: no_leading_underscores_for_library_prefixes
import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart' as _i1;
import 'package:stacked_services/stacked_services.dart' as _i8;

import '../ui/views/app_selector/app_selector_view.dart' as _i3;
import '../ui/views/contributors/contributors_view.dart' as _i7;
import '../ui/views/home/home_view.dart' as _i2;
import '../ui/views/patcher/patcher_view.dart' as _i4;
import '../ui/views/patches_selector/patches_selector_view.dart' as _i5;
import '../ui/views/settings/settings_view.dart' as _i6;

class Routes {
  static const homeView = '/home-view';

  static const appSelectorView = '/app-selector-view';

  static const patcherView = '/patcher-view';

  static const patchesSelectorView = '/patches-selector-view';

  static const settingsView = '/settings-view';

  static const contributorsView = '/contributors-view';

  static const all = <String>{
    homeView,
    appSelectorView,
    patcherView,
    patchesSelectorView,
    settingsView,
    contributorsView
  };
}

class StackedRouter extends _i1.RouterBase {
  final _routes = <_i1.RouteDef>[
    _i1.RouteDef(Routes.homeView, page: _i2.HomeView),
    _i1.RouteDef(Routes.appSelectorView, page: _i3.AppSelectorView),
    _i1.RouteDef(Routes.patcherView, page: _i4.PatcherView),
    _i1.RouteDef(Routes.patchesSelectorView, page: _i5.PatchesSelectorView),
    _i1.RouteDef(Routes.settingsView, page: _i6.SettingsView),
    _i1.RouteDef(Routes.contributorsView, page: _i7.ContributorsView)
  ];

  final _pagesMap = <Type, _i1.StackedRouteFactory>{
    _i2.HomeView: (data) {
      return MaterialPageRoute<dynamic>(
        builder: (context) => const _i2.HomeView(),
        settings: data,
      );
    },
    _i3.AppSelectorView: (data) {
      return MaterialPageRoute<dynamic>(
        builder: (context) => const _i3.AppSelectorView(),
        settings: data,
      );
    },
    _i4.PatcherView: (data) {
      return MaterialPageRoute<dynamic>(
        builder: (context) => const _i4.PatcherView(),
        settings: data,
      );
    },
    _i5.PatchesSelectorView: (data) {
      return MaterialPageRoute<dynamic>(
        builder: (context) => const _i5.PatchesSelectorView(),
        settings: data,
      );
    },
    _i6.SettingsView: (data) {
      return MaterialPageRoute<dynamic>(
        builder: (context) => const _i6.SettingsView(),
        settings: data,
      );
    },
    _i7.ContributorsView: (data) {
      return MaterialPageRoute<dynamic>(
        builder: (context) => const _i7.ContributorsView(),
        settings: data,
      );
    }
  };

  @override
  List<_i1.RouteDef> get routes => _routes;
  @override
  Map<Type, _i1.StackedRouteFactory> get pagesMap => _pagesMap;
}

extension NavigatorStateExtension on _i8.NavigationService {
  Future<dynamic> navigateToHomeView(
      [int? routerId,
      bool preventDuplicates = true,
      Map<String, String>? parameters,
      Widget Function(
              BuildContext, Animation<double>, Animation<double>, Widget)?
          transition]) async {
    navigateTo(Routes.homeView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToAppSelectorView(
      [int? routerId,
      bool preventDuplicates = true,
      Map<String, String>? parameters,
      Widget Function(
              BuildContext, Animation<double>, Animation<double>, Widget)?
          transition]) async {
    navigateTo(Routes.appSelectorView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToPatcherView(
      [int? routerId,
      bool preventDuplicates = true,
      Map<String, String>? parameters,
      Widget Function(
              BuildContext, Animation<double>, Animation<double>, Widget)?
          transition]) async {
    navigateTo(Routes.patcherView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToPatchesSelectorView(
      [int? routerId,
      bool preventDuplicates = true,
      Map<String, String>? parameters,
      Widget Function(
              BuildContext, Animation<double>, Animation<double>, Widget)?
          transition]) async {
    navigateTo(Routes.patchesSelectorView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToSettingsView(
      [int? routerId,
      bool preventDuplicates = true,
      Map<String, String>? parameters,
      Widget Function(
              BuildContext, Animation<double>, Animation<double>, Widget)?
          transition]) async {
    navigateTo(Routes.settingsView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToContributorsView(
      [int? routerId,
      bool preventDuplicates = true,
      Map<String, String>? parameters,
      Widget Function(
              BuildContext, Animation<double>, Animation<double>, Widget)?
          transition]) async {
    navigateTo(Routes.contributorsView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }
}

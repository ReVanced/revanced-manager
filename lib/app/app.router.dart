// GENERATED CODE - DO NOT MODIFY BY HAND

// **************************************************************************
// StackedRouterGenerator
// **************************************************************************

// ignore_for_file: no_leading_underscores_for_library_prefixes, implementation_imports
import 'package:flutter/material.dart';
import 'package:flutter/src/foundation/key.dart' as _i7;
import 'package:stacked/stacked.dart' as _i1;
import 'package:stacked_services/stacked_services.dart' as _i8;

import '../ui/views/app_selector/app_selector_view.dart' as _i2;
import '../ui/views/contributors/contributors_view.dart' as _i6;
import '../ui/views/installer/installer_view.dart' as _i4;
import '../ui/views/patches_selector/patches_selector_view.dart' as _i3;
import '../ui/views/settings/settings_view.dart' as _i5;

class Routes {
  static const appSelectorView = '/app-selector-view';

  static const patchesSelectorView = '/patches-selector-view';

  static const installerView = '/installer-view';

  static const settingsView = '/settings-view';

  static const contributorsView = '/contributors-view';

  static const all = <String>{
    appSelectorView,
    patchesSelectorView,
    installerView,
    settingsView,
    contributorsView
  };
}

class StackedRouter extends _i1.RouterBase {
  final _routes = <_i1.RouteDef>[
    _i1.RouteDef(Routes.appSelectorView, page: _i2.AppSelectorView),
    _i1.RouteDef(Routes.patchesSelectorView, page: _i3.PatchesSelectorView),
    _i1.RouteDef(Routes.installerView, page: _i4.InstallerView),
    _i1.RouteDef(Routes.settingsView, page: _i5.SettingsView),
    _i1.RouteDef(Routes.contributorsView, page: _i6.ContributorsView)
  ];

  final _pagesMap = <Type, _i1.StackedRouteFactory>{
    _i2.AppSelectorView: (data) {
      return MaterialPageRoute<dynamic>(
        builder: (context) => const _i2.AppSelectorView(),
        settings: data,
      );
    },
    _i3.PatchesSelectorView: (data) {
      return MaterialPageRoute<dynamic>(
        builder: (context) => const _i3.PatchesSelectorView(),
        settings: data,
      );
    },
    _i4.InstallerView: (data) {
      final args = data.getArgs<InstallerViewArguments>(
        orElse: () => const InstallerViewArguments(),
      );
      return MaterialPageRoute<dynamic>(
        builder: (context) => _i4.InstallerView(key: args.key),
        settings: data,
      );
    },
    _i5.SettingsView: (data) {
      return MaterialPageRoute<dynamic>(
        builder: (context) => const _i5.SettingsView(),
        settings: data,
      );
    },
    _i6.ContributorsView: (data) {
      return MaterialPageRoute<dynamic>(
        builder: (context) => const _i6.ContributorsView(),
        settings: data,
      );
    }
  };

  @override
  List<_i1.RouteDef> get routes => _routes;
  @override
  Map<Type, _i1.StackedRouteFactory> get pagesMap => _pagesMap;
}

class InstallerViewArguments {
  const InstallerViewArguments({this.key});

  final _i7.Key? key;
}

extension NavigatorStateExtension on _i8.NavigationService {
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

  Future<dynamic> navigateToInstallerView(
      {_i7.Key? key,
      int? routerId,
      bool preventDuplicates = true,
      Map<String, String>? parameters,
      Widget Function(
              BuildContext, Animation<double>, Animation<double>, Widget)?
          transition}) async {
    navigateTo(Routes.installerView,
        arguments: InstallerViewArguments(key: key),
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

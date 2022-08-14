// GENERATED CODE - DO NOT MODIFY BY HAND

// **************************************************************************
// StackedRouterGenerator
// **************************************************************************

// ignore_for_file: public_member_api_docs, unused_import, non_constant_identifier_names

import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

import '../ui/views/app_selector/app_selector_view.dart';
import '../ui/views/contributors/contributors_view.dart';
import '../ui/views/installer/installer_view.dart';
import '../ui/views/patches_selector/patches_selector_view.dart';
import '../ui/views/settings/settings_view.dart';

class Routes {
  static const String appSelectorView = '/app-selector-view';
  static const String patchesSelectorView = '/patches-selector-view';
  static const String installerView = '/installer-view';
  static const String settingsView = '/settings-view';
  static const String contributorsView = '/contributors-view';
  static const all = <String>{
    appSelectorView,
    patchesSelectorView,
    installerView,
    settingsView,
    contributorsView,
  };
}

class StackedRouter extends RouterBase {
  @override
  List<RouteDef> get routes => _routes;
  final _routes = <RouteDef>[
    RouteDef(Routes.appSelectorView, page: AppSelectorView),
    RouteDef(Routes.patchesSelectorView, page: PatchesSelectorView),
    RouteDef(Routes.installerView, page: InstallerView),
    RouteDef(Routes.settingsView, page: SettingsView),
    RouteDef(Routes.contributorsView, page: ContributorsView),
  ];
  @override
  Map<Type, StackedRouteFactory> get pagesMap => _pagesMap;
  final _pagesMap = <Type, StackedRouteFactory>{
    AppSelectorView: (data) {
      return MaterialPageRoute<dynamic>(
        builder: (context) => const AppSelectorView(),
        settings: data,
      );
    },
    PatchesSelectorView: (data) {
      return MaterialPageRoute<dynamic>(
        builder: (context) => const PatchesSelectorView(),
        settings: data,
      );
    },
    InstallerView: (data) {
      var args = data.getArgs<InstallerViewArguments>(
        orElse: () => InstallerViewArguments(),
      );
      return MaterialPageRoute<dynamic>(
        builder: (context) => InstallerView(key: args.key),
        settings: data,
      );
    },
    SettingsView: (data) {
      return MaterialPageRoute<dynamic>(
        builder: (context) => const SettingsView(),
        settings: data,
      );
    },
    ContributorsView: (data) {
      return MaterialPageRoute<dynamic>(
        builder: (context) => const ContributorsView(),
        settings: data,
      );
    },
  };
}

/// ************************************************************************
/// Arguments holder classes
/// *************************************************************************

/// InstallerView arguments holder class
class InstallerViewArguments {
  final Key? key;
  InstallerViewArguments({this.key});
}

/// ************************************************************************
/// Extension for strongly typed navigation
/// *************************************************************************

extension NavigatorStateExtension on NavigationService {
  Future<dynamic> navigateToAppSelectorView({
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo(
      Routes.appSelectorView,
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToPatchesSelectorView({
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo(
      Routes.patchesSelectorView,
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToInstallerView({
    Key? key,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo(
      Routes.installerView,
      arguments: InstallerViewArguments(key: key),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToSettingsView({
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo(
      Routes.settingsView,
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToContributorsView({
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo(
      Routes.contributorsView,
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }
}

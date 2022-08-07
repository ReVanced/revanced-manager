// GENERATED CODE - DO NOT MODIFY BY HAND

// **************************************************************************
// StackedRouterGenerator
// **************************************************************************

// ignore_for_file: no_leading_underscores_for_library_prefixes
import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart' as _i1;
import 'package:stacked_services/stacked_services.dart' as _i6;

import '../ui/views/app_selector/app_selector_view.dart' as _i3;
import '../ui/views/home/home_view.dart' as _i2;
import '../ui/views/patcher/patcher_view.dart' as _i4;
import '../ui/views/patches_selector/patches_selector_view.dart' as _i5;

class Routes {
  static const homeView = '/home-view';

  static const appSelectorView = '/app-selector-view';

  static const patcherView = '/patcher-view';

  static const patchesSelectorView = '/patches-selector-view';

  static const all = <String>{
    homeView,
    appSelectorView,
    patcherView,
    patchesSelectorView
  };
}

class StackedRouter extends _i1.RouterBase {
  final _routes = <_i1.RouteDef>[
    _i1.RouteDef(Routes.homeView, page: _i2.HomeView),
    _i1.RouteDef(Routes.appSelectorView, page: _i3.AppSelectorView),
    _i1.RouteDef(Routes.patcherView, page: _i4.PatcherView),
    _i1.RouteDef(Routes.patchesSelectorView, page: _i5.PatchesSelectorView)
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
    }
  };

  @override
  List<_i1.RouteDef> get routes => _routes;
  @override
  Map<Type, _i1.StackedRouteFactory> get pagesMap => _pagesMap;
}

extension NavigatorStateExtension on _i6.NavigationService {
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
}

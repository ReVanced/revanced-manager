// GENERATED CODE - DO NOT MODIFY BY HAND

// **************************************************************************
// StackedLocatorGenerator
// **************************************************************************

// ignore_for_file: public_member_api_docs

import 'package:stacked_core/stacked_core.dart';
import 'package:stacked_services/src/navigation/navigation_service.dart';

import '../services/patcher_api.dart';
import '../ui/views/app_selector/app_selector_viewmodel.dart';
import '../ui/views/patcher/patcher_viewmodel.dart';
import '../ui/views/patches_selector/patches_selector_viewmodel.dart';

final locator = StackedLocator.instance;

Future<void> setupLocator(
    {String? environment, EnvironmentFilter? environmentFilter}) async {
// Register environments
  locator.registerEnvironment(
      environment: environment, environmentFilter: environmentFilter);

// Register dependencies
  locator.registerLazySingleton(() => NavigationService());
  locator.registerLazySingleton(() => PatcherAPI());
  locator.registerLazySingleton(() => PatcherViewModel());
  locator.registerLazySingleton(() => AppSelectorViewModel());
  locator.registerLazySingleton(() => PatchesSelectorViewModel());
}

// GENERATED CODE - DO NOT MODIFY BY HAND

// **************************************************************************
// StackedLocatorGenerator
// **************************************************************************

// ignore_for_file: public_member_api_docs, depend_on_referenced_packages

import 'package:stacked_core/stacked_core.dart';
import 'package:stacked_services/stacked_services.dart';
import 'package:stacked_themes/stacked_themes.dart';

import '../services/manager_api.dart';
import '../services/patcher_api.dart';
import '../services/root_api.dart';
import '../ui/views/app_selector/app_selector_viewmodel.dart';
import '../ui/views/installer/installer_viewmodel.dart';
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
  locator.registerLazySingleton(() => ManagerAPI());
  locator.registerLazySingleton(() => RootAPI());
  locator.registerLazySingleton(() => PatcherViewModel());
  locator.registerLazySingleton(() => AppSelectorViewModel());
  locator.registerLazySingleton(() => PatchesSelectorViewModel());
  locator.registerLazySingleton(() => InstallerViewModel());
  locator.registerLazySingleton(() => ThemeService.getInstance());
}

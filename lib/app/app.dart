import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_view.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_viewmodel.dart';
import 'package:revanced_manager/ui/views/contributors/contributors_view.dart';
import 'package:revanced_manager/ui/views/home/home_view.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_view.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_view.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:revanced_manager/ui/views/settings/settings_view.dart';
import 'package:stacked/stacked_annotations.dart';
import 'package:stacked_services/stacked_services.dart';
import 'package:stacked_themes/stacked_themes.dart';

@StackedApp(
  routes: [
    MaterialRoute(page: HomeView),
    MaterialRoute(page: AppSelectorView),
    MaterialRoute(page: PatcherView),
    MaterialRoute(page: PatchesSelectorView),
    MaterialRoute(page: SettingsView),
    MaterialRoute(page: ContributorsView)
  ],
  dependencies: [
    LazySingleton(classType: NavigationService),
    LazySingleton(classType: PatcherAPI),
    LazySingleton(classType: PatcherViewModel),
    LazySingleton(classType: AppSelectorViewModel),
    LazySingleton(classType: PatchesSelectorViewModel),
    LazySingleton(
        classType: ThemeService, resolveUsing: ThemeService.getInstance),
  ],
)
class AppSetup {}

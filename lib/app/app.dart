import 'package:revanced_manager/main.dart';
import 'package:revanced_manager/main_viewmodel.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_view.dart';
import 'package:revanced_manager/ui/views/contributors/contributors_view.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/views/installer/installer_view.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_view.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_view.dart';
import 'package:revanced_manager/ui/views/root_checker/root_checker_view.dart';
import 'package:revanced_manager/ui/views/settings/settings_view.dart';
import 'package:stacked/stacked_annotations.dart';
import 'package:stacked_services/stacked_services.dart';
import 'package:stacked_themes/stacked_themes.dart';

@StackedApp(
  routes: [
    MaterialRoute(page: Navigation),
    MaterialRoute(page: PatcherView),
    MaterialRoute(page: AppSelectorView),
    MaterialRoute(page: PatchesSelectorView),
    MaterialRoute(page: InstallerView),
    MaterialRoute(page: SettingsView),
    MaterialRoute(page: ContributorsView),
    MaterialRoute(page: RootCheckerView),
  ],
  dependencies: [
    LazySingleton(classType: MainViewModel),
    LazySingleton(classType: HomeViewModel),
    LazySingleton(classType: PatcherViewModel),
    LazySingleton(classType: NavigationService),
    LazySingleton(
      classType: ThemeService,
      resolveUsing: ThemeService.getInstance,
    ),
    LazySingleton(classType: ManagerAPI),
    LazySingleton(classType: PatcherAPI),
  ],
)
class AppSetup {}

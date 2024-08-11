import 'package:revanced_manager/services/download_manager.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/revanced_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_view.dart';
import 'package:revanced_manager/ui/views/contributors/contributors_view.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/views/installer/installer_view.dart';
import 'package:revanced_manager/ui/views/installer/installer_viewmodel.dart';
import 'package:revanced_manager/ui/views/navigation/navigation_view.dart';
import 'package:revanced_manager/ui/views/navigation/navigation_viewmodel.dart';
import 'package:revanced_manager/ui/views/patch_options/patch_options_view.dart';
import 'package:revanced_manager/ui/views/patch_options/patch_options_viewmodel.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_view.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_view.dart';
import 'package:revanced_manager/ui/views/settings/settings_view.dart';
import 'package:revanced_manager/ui/widgets/appInfoView/app_info_view.dart';
import 'package:stacked/stacked_annotations.dart';
import 'package:stacked_services/stacked_services.dart';

@StackedApp(
  routes: [
    MaterialRoute(page: NavigationView),
    MaterialRoute(page: PatcherView),
    MaterialRoute(page: AppSelectorView),
    MaterialRoute(page: PatchesSelectorView),
    MaterialRoute(page: PatchOptionsView),
    MaterialRoute(page: InstallerView),
    MaterialRoute(page: SettingsView),
    MaterialRoute(page: ContributorsView),
    MaterialRoute(page: AppInfoView),
  ],
  dependencies: [
    LazySingleton(classType: NavigationViewModel),
    LazySingleton(classType: HomeViewModel),
    LazySingleton(classType: PatcherViewModel),
    LazySingleton(classType: PatchOptionsViewModel),
    LazySingleton(classType: InstallerViewModel),
    LazySingleton(classType: NavigationService),
    LazySingleton(classType: ManagerAPI),
    LazySingleton(classType: PatcherAPI),
    LazySingleton(classType: RevancedAPI),
    LazySingleton(classType: GithubAPI),
    LazySingleton(classType: DownloadManager),
    LazySingleton(classType: Toast),
  ],
)
class AppSetup {}

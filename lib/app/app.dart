import 'package:revanced_manager_flutter/ui/views/app_selector/app_selector_view.dart';
import 'package:revanced_manager_flutter/ui/views/home/home_view.dart';
import 'package:revanced_manager_flutter/ui/views/patcher/patcher_view.dart';
import 'package:stacked/stacked_annotations.dart';
import 'package:stacked_services/stacked_services.dart';

@StackedApp(routes: [
  MaterialRoute(page: HomeView),
  MaterialRoute(page: AppSelectorView),
  MaterialRoute(page: PatcherView),
], dependencies: [
  LazySingleton(classType: NavigationService),
])
class AppSetup {}

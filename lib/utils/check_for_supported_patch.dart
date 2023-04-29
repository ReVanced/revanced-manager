import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';

bool isPatchSupported(Patch patch) {
  final PatchedApplication app = locator<PatcherViewModel>().selectedApp!;
  return patch.compatiblePackages.isEmpty ||
      patch.compatiblePackages.any(
        (pack) =>
            pack.name == app.packageName &&
            (pack.versions.isEmpty || pack.versions.contains(app.version)),
      );
}

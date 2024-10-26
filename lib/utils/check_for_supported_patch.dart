import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/services/manager_api.dart';
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

bool hasUnsupportedRequiredOption(List<Option> options, Patch patch) {
  final List<String> requiredOptionsType = [];
  final List<String> supportedOptionsType = [
    'String',
    'Boolean',
    'Int',
    'StringArray',
    'IntArray',
    'LongArray',
  ];
  for (final Option option in options) {
    if (option.required &&
        option.value == null &&
        locator<ManagerAPI>().getPatchOption(
              locator<PatcherViewModel>().selectedApp!.packageName,
              patch.name,
              option.key,
            ) ==
            null) {
      requiredOptionsType.add(option.type);
    }
  }
  for (final String optionType in requiredOptionsType) {
    if (!supportedOptionsType.contains(optionType)) {
      return true;
    }
  }
  return false;
}

List<Option> getNullRequiredOptions(List<Patch> patches, String packageName) {
  final List<Option> requiredNullOptions = [];
  for (final patch in patches) {
    for (final patchOption in patch.options) {
      if (!patch.excluded &&
          patchOption.required &&
          patchOption.value == null &&
          locator<ManagerAPI>()
                  .getPatchOption(packageName, patch.name, patchOption.key) ==
              null) {
        requiredNullOptions.add(patchOption);
      }
    }
  }
  return requiredNullOptions;
}

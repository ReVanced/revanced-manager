import 'package:flutter/material.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:stacked/stacked.dart';

class PatchOptionsViewModel extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final String selectedApp =
      locator<PatcherViewModel>().selectedApp!.packageName;
  List<Option> options = [];
  List<Option> savedOptions = [];
  List<Option> modifiedOptions = [];

  Future<void> initialize() async {
    options = getDefaultOptions();
    for (final Option option in options) {
      final Option? savedOption = _managerAPI.getPatchOption(
        selectedApp,
        _managerAPI.selectedPatch!.name,
        option.key,
      );
      if (savedOption != null) {
        savedOptions.add(savedOption);
      }
    }
    modifiedOptions = [
      ...savedOptions,
      ...options.where(
        (option) => !savedOptions.any((sOption) => sOption.key == option.key),
      ),
    ];
  }

  bool saveOptions(BuildContext context) {
    final List<Option> requiredNullOptions = [];
    for (final Option option in options) {
      if (modifiedOptions.any((mOption) => mOption.key == option.key)) {
        _managerAPI.clearPatchOption(
          selectedApp,
          _managerAPI.selectedPatch!.name,
          option.key,
        );
      }
    }
    for (final Option option in modifiedOptions) {
      if (option.required && option.value == null) {
        requiredNullOptions.add(option);
      } else {
        _managerAPI.setPatchOption(
          option,
          _managerAPI.selectedPatch!.name,
          selectedApp,
        );
      }
    }
    if (requiredNullOptions.isNotEmpty) {
      showRequiredOptionNullDialog(
        context,
        requiredNullOptions,
        _managerAPI,
        selectedApp,
      );
      return false;
    }
    return true;
  }

  void modifyOptions(dynamic value, Option option) {
    final Option modifiedOption = Option(
      title: option.title,
      description: option.description,
      values: option.values,
      type: option.type,
      value: value,
      required: option.required,
      key: option.key,
    );
    modifiedOptions.removeWhere((mOption) => mOption.key == option.key);
    modifiedOptions.add(modifiedOption);
  }

  List<Option> getDefaultOptions() {
    final List<Option> defaultOptions = [];
    for (final option in _managerAPI.options) {
      final Option defaultOption = Option(
        title: option.title,
        description: option.description,
        values: option.values,
        type: option.type,
        value: option.value is List ? option.value.toList() : option.value,
        required: option.required,
        key: option.key,
      );
      defaultOptions.add(defaultOption);
    }
    return defaultOptions;
  }

  dynamic getDefaultValue(Option patchOption) => _managerAPI.options
      .firstWhere(
        (option) => option.key == patchOption.key,
      )
      .value;
}

Future<void> showRequiredOptionNullDialog(
  BuildContext context,
  List<Option> options,
  ManagerAPI managerAPI,
  String selectedApp,
) async {
  final List<String> optionsTitles = [];
  for (final option in options) {
    optionsTitles.add('• ${option.title}');
  }
  await showDialog(
    context: context,
    builder: (context) => AlertDialog(
      title: Text(t.notice),
      actions: [
        TextButton(
          onPressed: () async {
            if (managerAPI.isPatchesChangeEnabled()) {
              locator<PatcherViewModel>()
                  .selectedPatches
                  .remove(managerAPI.selectedPatch);
              locator<PatcherViewModel>().notifyListeners();
              for (final option in options) {
                managerAPI.clearPatchOption(
                  selectedApp,
                  managerAPI.selectedPatch!.name,
                  option.key,
                );
              }
              Navigator.of(context)
                ..pop()
                ..pop()
                ..pop();
            } else {
              PatchesSelectorViewModel().showPatchesChangeDialog(context);
            }
          },
          child: Text(t.patchOptionsView.unselectPatch),
        ),
        FilledButton(
          onPressed: () {
            Navigator.of(context).pop();
          },
          child: Text(t.okButton),
        ),
      ],
      content: Text(
        t.patchOptionsView.requiredOptionNull(
          options: optionsTitles.join('\n'),
        ),
      ),
    ),
  );
}

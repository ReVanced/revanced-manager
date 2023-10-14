import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:stacked/stacked.dart';

class PatchOptionsViewModel extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final String selectedApp =
      locator<PatcherViewModel>().selectedApp!.packageName;
  List<Option> options = [];
  List<Option> savedOptions = [];
  List<Option> visibleOptions = [];

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
    if (savedOptions.isNotEmpty) {
      visibleOptions = [
        ...savedOptions,
        ...options
            .where(
              (option) =>
                  option.required &&
                  !savedOptions.any((sOption) => sOption.key == option.key),
            )
            .toList(),
      ];
    } else {
      visibleOptions = [
        ...options.where((option) => option.required).toList(),
      ];
    }
  }

  void addOption(Option option) {
    visibleOptions.add(option);
    notifyListeners();
  }

  void removeOption(Option option) {
    visibleOptions.removeWhere((vOption) => vOption.key == option.key);
    notifyListeners();
  }

  bool saveOptions(BuildContext context) {
    final List<Option> requiredNullOptions = [];
    for (final Option option in options) {
      if (!visibleOptions.any((vOption) => vOption.key == option.key)) {
        _managerAPI.clearPatchOption(
            selectedApp, _managerAPI.selectedPatch!.name, option.key);
      }
    }
    for (final Option option in visibleOptions) {
      if (option.required && option.value == null) {
        requiredNullOptions.add(option);
      } else {
        _managerAPI.setPatchOption(
            option, _managerAPI.selectedPatch!.name, selectedApp);
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
      optionClassType: option.optionClassType,
      value: value,
      required: option.required,
      key: option.key,
    );
    visibleOptions[visibleOptions
        .indexWhere((vOption) => vOption.key == option.key)] = modifiedOption;
    _managerAPI.modifiedOptions
        .removeWhere((mOption) => mOption.key == option.key);
    _managerAPI.modifiedOptions.add(modifiedOption);
  }

  List<Option> getDefaultOptions() {
    final List<Option> defaultOptions = [];
    for (final option in _managerAPI.options) {
      final Option defaultOption = Option(
        title: option.title,
        description: option.description,
        optionClassType: option.optionClassType,
        value: option.value is List ? option.value.toList() : option.value,
        required: option.required,
        key: option.key,
      );
      defaultOptions.add(defaultOption);
    }
    return defaultOptions;
  }

  void resetOptions() {
    _managerAPI.modifiedOptions.clear();
    visibleOptions =
        getDefaultOptions().where((option) => option.required).toList();
    notifyListeners();
  }

  Future<void> showAddOptionDialog(BuildContext context) async {
    await showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            I18nText(
              'patchOptionsView.addOptions',
            ),
            Text(
              '',
              style: TextStyle(
                fontSize: 16,
                color: Theme.of(context).colorScheme.onSecondaryContainer,
              ),
            ),
          ],
        ),
        actions: [
          CustomMaterialButton(
            label: I18nText('okButton'),
            onPressed: () {
              Navigator.of(context).pop();
            },
          ),
        ],
        contentPadding: const EdgeInsets.all(8),
        content: Wrap(
          spacing: 14,
          runSpacing: 14,
          children: options
              .where(
            (option) =>
                !visibleOptions.any((vOption) => vOption.key == option.key),
          )
              .map((e) {
            return CustomCard(
              padding: const EdgeInsets.all(4),
              backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
              onTap: () {
                addOption(e);
                Navigator.pop(context);
              },
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      e.title,
                      style: const TextStyle(
                        fontSize: 16,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      e.description,
                      style: TextStyle(
                        fontSize: 14,
                        color: Theme.of(context).colorScheme.onSecondaryContainer,
                      ),
                    )
                  ],
                ),
              ),
            );
          }).toList(),
        ),
      ),
    );
  }
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
      backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
      title: I18nText('notice'),
      actions: [
        CustomMaterialButton(
          isFilled: false,
          label: I18nText(
            'patchOptionsView.deselectPatch',
          ),
          onPressed: () async {
            if (managerAPI.isPatchesChangeEnabled()) {
              locator<PatcherViewModel>()
                  .selectedPatches
                  .remove(managerAPI.selectedPatch);
              locator<PatcherViewModel>().notifyListeners();
              for (final option in options) {
                managerAPI.clearPatchOption(
                    selectedApp, managerAPI.selectedPatch!.name, option.key);
              }
              Navigator.of(context)
                ..pop()
                ..pop()
                ..pop();
            } else {
              PatchesSelectorViewModel().showPatchesChangeDialog(context);
            }
          },
        ),
        CustomMaterialButton(
          label: I18nText('okButton'),
          onPressed: () {
            Navigator.of(context).pop();
          },
        ),
      ],
      content: I18nText(
        'patchOptionsView.requiredOptionNull',
        translationParams: {
          'options': optionsTitles.join('\n'),
        },
      ),
    ),
  );
}

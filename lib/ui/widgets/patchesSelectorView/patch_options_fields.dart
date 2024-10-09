import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/views/patch_options/patch_options_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

class BooleanPatchOption extends StatelessWidget {
  const BooleanPatchOption({
    super.key,
    required this.patchOption,
    required this.model,
  });

  final Option patchOption;
  final PatchOptionsViewModel model;

  @override
  Widget build(BuildContext context) {
    final ValueNotifier patchOptionValue = ValueNotifier(patchOption.value);
    return PatchOption(
      widget: Align(
        alignment: Alignment.bottomLeft,
        child: ValueListenableBuilder(
          valueListenable: patchOptionValue,
          builder: (context, value, child) {
            return Switch(
              value: value ?? false,
              onChanged: (bool value) {
                patchOptionValue.value = value;
                model.modifyOptions(value, patchOption);
              },
            );
          },
        ),
      ),
      patchOption: patchOption,
      patchOptionValue: patchOptionValue,
      model: model,
    );
  }
}

class IntAndStringPatchOption extends StatefulWidget {
  const IntAndStringPatchOption({
    super.key,
    required this.patchOption,
    required this.model,
  });

  final Option patchOption;
  final PatchOptionsViewModel model;

  @override
  State<IntAndStringPatchOption> createState() =>
      _IntAndStringPatchOptionState();
}

class _IntAndStringPatchOptionState extends State<IntAndStringPatchOption> {
  ValueNotifier? patchOptionValue;
  String getKey() {
    if (patchOptionValue!.value != null && widget.patchOption.values != null) {
      final List values = widget.patchOption.values!.entries
          .where((e) => e.value == patchOptionValue!.value)
          .toList();
      if (values.isNotEmpty) {
        return values.first.key;
      }
    }
    return '';
  }

  @override
  Widget build(BuildContext context) {
    patchOptionValue ??= ValueNotifier(widget.patchOption.value);
    return PatchOption(
      widget: ValueListenableBuilder(
        valueListenable: patchOptionValue!,
        builder: (context, value, child) {
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextFieldForPatchOption(
                value: value,
                patchOption: widget.patchOption,
                selectedKey: getKey(),
                onChanged: (value) {
                  patchOptionValue!.value = value;
                  widget.model.modifyOptions(value, widget.patchOption);
                },
              ),
              if (value == null)
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: 8),
                    Text(
                      widget.patchOption.required
                          ? t.patchOptionsView.requiredOption
                          : t.patchOptionsView.nullValue,
                      style: TextStyle(
                        color: widget.patchOption.required
                            ? Theme.of(context).colorScheme.error
                            : Theme.of(context)
                                .colorScheme
                                .onSecondaryContainer
                                .withOpacity(0.6),
                      ),
                    ),
                  ],
                ),
            ],
          );
        },
      ),
      patchOption: widget.patchOption,
      patchOptionValue: patchOptionValue!,
      model: widget.model,
    );
  }
}

class IntStringLongListPatchOption extends StatelessWidget {
  const IntStringLongListPatchOption({
    super.key,
    required this.patchOption,
    required this.model,
  });

  final Option patchOption;
  final PatchOptionsViewModel model;

  @override
  Widget build(BuildContext context) {
    final List<dynamic> values = List.from(patchOption.value ?? []);
    final ValueNotifier patchOptionValue = ValueNotifier(values);
    final String type = patchOption.type;

    String getKey(dynamic value) {
      if (value != null && patchOption.values != null) {
        final List values = patchOption.values!.entries
            .where((e) => e.value.toString() == value)
            .toList();
        if (values.isNotEmpty) {
          return values.first.key;
        }
      }
      return '';
    }

    bool isCustomValue() {
      if (values.length == 1 && patchOption.values != null) {
        if (getKey(values[0]) != '') {
          return false;
        }
      }
      return true;
    }

    bool isTextFieldVisible = isCustomValue();

    return PatchOption(
      widget: ValueListenableBuilder(
        valueListenable: patchOptionValue,
        builder: (context, value, child) {
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ListView.builder(
                shrinkWrap: true,
                itemCount: value.length,
                physics: const NeverScrollableScrollPhysics(),
                itemBuilder: (BuildContext context, int index) {
                  final e = values[index];
                  return TextFieldForPatchOption(
                    value: e.toString(),
                    patchOption: patchOption,
                    selectedKey: value.length > 1 ? '' : getKey(e),
                    showDropdown: index == 0,
                    onChanged: (newValue) {
                      if (newValue is List) {
                        values.clear();
                        isTextFieldVisible = false;
                        values.add(newValue.toString());
                      } else {
                        isTextFieldVisible = true;
                        if (values.length == 1 &&
                            values[0].toString().startsWith('[') &&
                            type.contains('Array')) {
                          values.clear();
                          values.addAll(patchOption.value);
                        } else {
                          values[index] = type == 'StringArray'
                              ? newValue
                              : type == 'IntArray'
                                  ? int.parse(
                                      newValue.toString().isEmpty
                                          ? '0'
                                          : newValue.toString(),
                                    )
                                  : num.parse(
                                      newValue.toString().isEmpty
                                          ? '0'
                                          : newValue.toString(),
                                    );
                        }
                      }
                      patchOptionValue.value = List.from(values);
                      model.modifyOptions(values, patchOption);
                    },
                    removeValue: () {
                      patchOptionValue.value = List.from(patchOptionValue.value)
                        ..removeAt(index);
                      values.removeAt(index);
                      model.modifyOptions(values, patchOption);
                    },
                  );
                },
              ),
              if (isTextFieldVisible) ...[
                const SizedBox(height: 4),
                Align(
                  alignment: Alignment.centerLeft,
                  child: TextButton(
                    onPressed: () {
                      if (type == 'StringArray') {
                        patchOptionValue.value =
                            List.from(patchOptionValue.value)..add('');
                        values.add('');
                      } else {
                        patchOptionValue.value =
                            List.from(patchOptionValue.value)..add(0);
                        values.add(0);
                      }
                      model.modifyOptions(values, patchOption);
                    },
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.add, size: 20),
                        Text(
                          t.add,
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ],
          );
        },
      ),
      patchOption: patchOption,
      patchOptionValue: patchOptionValue,
      model: model,
    );
  }
}

class UnsupportedPatchOption extends StatelessWidget {
  const UnsupportedPatchOption({super.key, required this.patchOption});

  final Option patchOption;

  @override
  Widget build(BuildContext context) {
    return PatchOption(
      widget: Align(
        alignment: Alignment.centerLeft,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 8.0),
          child: Text(
            t.patchOptionsView.unsupportedOption,
            style: const TextStyle(
              fontSize: 16,
            ),
          ),
        ),
      ),
      patchOption: patchOption,
      patchOptionValue: ValueNotifier(null),
      model: PatchOptionsViewModel(),
    );
  }
}

class PatchOption extends StatelessWidget {
  const PatchOption({
    super.key,
    required this.widget,
    required this.patchOption,
    required this.patchOptionValue,
    required this.model,
  });

  final Widget widget;
  final Option patchOption;
  final ValueNotifier patchOptionValue;
  final PatchOptionsViewModel model;

  @override
  Widget build(BuildContext context) {
    final defaultValue = model.getDefaultValue(patchOption);
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: CustomCard(
        child: Row(
          children: [
            Expanded(
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              patchOption.title,
                              style: const TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              patchOption.description,
                              style: TextStyle(
                                fontSize: 14,
                                color: Theme.of(context)
                                    .colorScheme
                                    .onSecondaryContainer,
                              ),
                            ),
                          ],
                        ),
                      ),
                      ValueListenableBuilder(
                        valueListenable: patchOptionValue,
                        builder: (context, value, child) {
                          if (defaultValue != patchOptionValue.value) {
                            return IconButton(
                              onPressed: () {
                                patchOptionValue.value = defaultValue;
                                model.modifyOptions(
                                  defaultValue,
                                  patchOption,
                                );
                              },
                              icon: const Icon(Icons.history),
                            );
                          }
                          return const SizedBox();
                        },
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  widget,
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class TextFieldForPatchOption extends StatefulWidget {
  const TextFieldForPatchOption({
    super.key,
    required this.value,
    required this.patchOption,
    this.removeValue,
    required this.onChanged,
    required this.selectedKey,
    this.showDropdown = true,
  });

  final String? value;
  final Option patchOption;
  final String selectedKey;
  final bool showDropdown;
  final void Function()? removeValue;
  final void Function(dynamic value) onChanged;

  @override
  State<TextFieldForPatchOption> createState() =>
      _TextFieldForPatchOptionState();
}

class _TextFieldForPatchOptionState extends State<TextFieldForPatchOption> {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final TextEditingController controller = TextEditingController();
  String? selectedKey;
  String? defaultValue;

  @override
  Widget build(BuildContext context) {
    final bool isStringOption = widget.patchOption.type.contains('String');
    final bool isListOption = widget.patchOption.type.contains('List');
    selectedKey = selectedKey == '' ? selectedKey : widget.selectedKey;
    final bool isValueArray = widget.value?.startsWith('[') ?? false;
    final bool shouldResetValue =
        !isStringOption && isListOption && selectedKey == '' && isValueArray;
    controller.text = shouldResetValue ? '' : widget.value ?? '';
    defaultValue ??= controller.text;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (widget.showDropdown &&
            (widget.patchOption.values?.isNotEmpty ?? false))
          DropdownButton<String>(
            style: const TextStyle(
              fontSize: 16,
            ),
            borderRadius: BorderRadius.circular(4),
            dropdownColor: Theme.of(context).colorScheme.secondaryContainer,
            isExpanded: true,
            value: selectedKey,
            items: widget.patchOption.values!.entries
                .map(
                  (e) => DropdownMenuItem(
                    value: e.key,
                    child: RichText(
                      overflow: TextOverflow.ellipsis,
                      text: TextSpan(
                        text: e.key,
                        style: TextStyle(
                          fontSize: 16,
                          color: Theme.of(context)
                              .colorScheme
                              .onSecondaryContainer,
                        ),
                        children: [
                          TextSpan(
                            text: ' ${e.value}',
                            style: TextStyle(
                              fontSize: 16,
                              color: Theme.of(context)
                                  .colorScheme
                                  .onSecondaryContainer
                                  .withOpacity(0.6),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                )
                .toList()
              ..add(
                DropdownMenuItem(
                  value: '',
                  child: Text(
                    t.patchOptionsView.customValue,
                    style: TextStyle(
                      fontSize: 16,
                      color: Theme.of(context).colorScheme.onSecondaryContainer,
                    ),
                  ),
                ),
              ),
            onChanged: (value) {
              if (value == '') {
                controller.text = defaultValue!;
                widget.onChanged(controller.text);
              } else {
                controller.text = widget.patchOption.values![value].toString();
                widget.onChanged(
                  isListOption
                      ? widget.patchOption.values![value]
                      : controller.text,
                );
              }
              setState(() {
                selectedKey = value;
              });
            },
          ),
        if (selectedKey == '')
          TextFormField(
            inputFormatters: [
              if (widget.patchOption.type.contains('Int'))
                FilteringTextInputFormatter.allow(RegExp(r'[0-9]')),
              if (widget.patchOption.type.contains('Long'))
                FilteringTextInputFormatter.allow(RegExp(r'^[0-9]*\.?[0-9]*')),
            ],
            controller: controller,
            keyboardType:
                isStringOption ? TextInputType.text : TextInputType.number,
            decoration: InputDecoration(
              suffixIcon: PopupMenuButton(
                tooltip: t.patchOptionsView.tooltip,
                itemBuilder: (BuildContext context) {
                  return [
                    if (isListOption)
                      PopupMenuItem(
                        value: 'remove',
                        child: Text(t.remove),
                      ),
                    if (isStringOption) ...[
                      PopupMenuItem(
                        value: 'file',
                        child: Text(t.patchOptionsView.selectFilePath),
                      ),
                      PopupMenuItem(
                        value: 'folder',
                        child: Text(t.patchOptionsView.selectFolder),
                      ),
                    ],
                    if (!widget.patchOption.required)
                      PopupMenuItem(
                        value: 'null',
                        child: Text(t.patchOptionsView.setToNull),
                      ),
                  ];
                },
                onSelected: (String selection) async {
                  Future<bool> gotExternalStoragePermission() async {
                    // manageExternalStorage permission is required for folder selection
                    // otherwise, the app will not complain, but the patches will error out
                    // the same way as if the user selected an empty folder.
                    // Android 11 and above requires the manageExternalStorage permission
                    if (_managerAPI.isScopedStorageAvailable) {
                      final permission =
                          await Permission.manageExternalStorage.request();
                      return permission.isGranted;
                    }
                    return true;
                  }

                  switch (selection) {
                    case 'file':
                      // here scope storage is not required because file_picker
                      // will copy the file to the app's cache
                      final FilePickerResult? result =
                          await FilePicker.platform.pickFiles();
                      if (result == null) {
                        return;
                      }
                      controller.text = result.files.single.path!;
                      widget.onChanged(controller.text);
                      break;
                    case 'folder':
                      if (!await gotExternalStoragePermission()) {
                        return;
                      }
                      final String? result =
                          await FilePicker.platform.getDirectoryPath();
                      if (result == null) {
                        return;
                      }
                      controller.text = result;
                      widget.onChanged(controller.text);
                      break;
                    case 'remove':
                      widget.removeValue!();
                      break;
                    case 'null':
                      controller.text = '';
                      widget.onChanged(null);
                      break;
                  }
                },
              ),
              hintStyle: TextStyle(
                fontSize: 14,
                color: Theme.of(context).colorScheme.onSecondaryContainer,
              ),
            ),
            onChanged: (String value) {
              widget.onChanged(value);
            },
          ),
      ],
    );
  }
}

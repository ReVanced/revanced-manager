import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

class BooleanPatchOption extends StatelessWidget {
  const BooleanPatchOption({
    super.key,
    required this.patchOption,
    required this.removeOption,
    required this.onChanged,
  });

  final Option patchOption;
  final void Function(Option option) removeOption;
  final void Function(dynamic value, Option option) onChanged;

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
                onChanged(value, patchOption);
              },
            );
          },
        ),
      ),
      patchOption: patchOption,
      removeOption: (Option option) {
        removeOption(option);
      },
    );
  }
}

class IntAndStringPatchOption extends StatelessWidget {
  const IntAndStringPatchOption({
    super.key,
    required this.patchOption,
    required this.removeOption,
    required this.onChanged,
  });

  final Option patchOption;
  final void Function(Option option) removeOption;
  final void Function(dynamic value, Option option) onChanged;

  @override
  Widget build(BuildContext context) {
    final ValueNotifier patchOptionValue = ValueNotifier(patchOption.value);
    return PatchOption(
      widget: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextFieldForPatchOption(
            value: patchOption.value,
            optionType: patchOption.optionClassType,
            onChanged: (value) {
              patchOptionValue.value = value;
              onChanged(value, patchOption);
            },
          ),
          ValueListenableBuilder(
            valueListenable: patchOptionValue,
            builder: (context, value, child) {
              if (patchOption.required && value == null) {
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: 8),
                    I18nText(
                      'patchOptionsView.requiredOption',
                      child: Text(
                        '',
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.error,
                        ),
                      ),
                    ),
                  ],
                );
              } else {
                return const SizedBox();
              }
            },
          ),
        ],
      ),
      patchOption: patchOption,
      removeOption: (Option option) {
        removeOption(option);
      },
    );
  }
}

class IntStringLongListPatchOption extends StatelessWidget {
  const IntStringLongListPatchOption({
    super.key,
    required this.patchOption,
    required this.removeOption,
    required this.onChanged,
  });

  final Option patchOption;
  final void Function(Option option) removeOption;
  final void Function(dynamic value, Option option) onChanged;

  @override
  Widget build(BuildContext context) {
    final String type = patchOption.optionClassType;
    final List<dynamic> values = patchOption.value ?? [];
    final ValueNotifier patchOptionValue = ValueNotifier(values);
    return PatchOption(
      widget: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ValueListenableBuilder(
            valueListenable: patchOptionValue,
            builder: (context, value, child) {
              return ListView.builder(
                shrinkWrap: true,
                itemCount: value.length,
                physics: const NeverScrollableScrollPhysics(),
                itemBuilder: (BuildContext context, int index) {
                  final e = values[index];
                  return TextFieldForPatchOption(
                    value: e.toString(),
                    optionType: type,
                    onChanged: (newValue) {
                      values[index] = type == 'StringListPatchOption'
                          ? newValue
                          : type == 'IntListPatchOption'
                              ? int.parse(newValue)
                              : num.parse(newValue);
                      onChanged(values, patchOption);
                    },
                    removeValue: (value) {
                      patchOptionValue.value = List.from(patchOptionValue.value)
                        ..removeAt(index);
                      values.removeAt(index);
                      onChanged(values, patchOption);
                    },
                  );
                },
              );
            },
          ),
          const SizedBox(height: 4),
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton(
              onPressed: () {
                if (type == 'StringListPatchOption') {
                  patchOptionValue.value = List.from(patchOptionValue.value)
                    ..add('');
                  values.add('');
                } else {
                  patchOptionValue.value = List.from(patchOptionValue.value)
                    ..add(0);
                  values.add(0);
                }
                onChanged(values, patchOption);
              },
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.add, size: 20),
                  I18nText(
                    'add',
                    child: const Text(
                      '',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
      patchOption: patchOption,
      removeOption: (Option option) {
        removeOption(option);
      },
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
          child: I18nText(
            'patchOptionsView.unsupportedOption',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 16,
              ),
            ),
          ),
        ),
      ),
      patchOption: patchOption,
      removeOption: (_) {},
    );
  }
}

class PatchOption extends StatelessWidget {
  const PatchOption({
    super.key,
    required this.widget,
    required this.patchOption,
    required this.removeOption,
  });

  final Widget widget;
  final Option patchOption;
  final void Function(Option option) removeOption;

  @override
  Widget build(BuildContext context) {
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
                      if (!patchOption.required)
                        IconButton(
                          onPressed: () => removeOption(patchOption),
                          icon: const Icon(Icons.delete),
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
    this.removeValue,
    required this.onChanged,
    required this.optionType,
  });

  final String? value;
  final String optionType;
  final void Function(dynamic value)? removeValue;
  final void Function(dynamic value) onChanged;

  @override
  State<TextFieldForPatchOption> createState() =>
      _TextFieldForPatchOptionState();
}

class _TextFieldForPatchOptionState extends State<TextFieldForPatchOption> {
  final TextEditingController controller = TextEditingController();
  @override
  Widget build(BuildContext context) {
    final bool isStringOption = widget.optionType.contains('String');
    final bool isListOption = widget.optionType.contains('List');
    controller.text = widget.value ?? '';
    return TextFormField(
      inputFormatters: [
        if (widget.optionType.contains('Int'))
          FilteringTextInputFormatter.allow(RegExp(r'[0-9]')),
        if (widget.optionType.contains('Long'))
          FilteringTextInputFormatter.allow(RegExp(r'^[0-9]*\.?[0-9]*')),
      ],
      controller: controller,
      keyboardType: isStringOption ? TextInputType.text : TextInputType.number,
      decoration: InputDecoration(
        suffixIcon: PopupMenuButton(
          tooltip: FlutterI18n.translate(
            context,
            'patchOptionsView.tooltip',
          ),
          itemBuilder: (BuildContext context) {
            return [
              if (isListOption)
                PopupMenuItem(
                  value: 'remove',
                  child: I18nText('remove'),
                ),
              if (isStringOption && !isListOption) ...[
                PopupMenuItem(
                  value: 'patchOptionsView.selectFilePath',
                  child: I18nText('patchOptionsView.selectFilePath'),
                ),
                PopupMenuItem(
                  value: 'patchOptionsView.selectFolder',
                  child: I18nText('patchOptionsView.selectFolder'),
                ),
              ],
            ];
          },
          onSelected: (String selection) async {
            switch (selection) {
              case 'patchOptionsView.selectFilePath':
                final result = await FilePicker.platform.pickFiles();
                if (result != null && result.files.single.path != null) {
                  controller.text = result.files.single.path.toString();
                  widget.onChanged(controller.text);
                }
                break;
              case 'patchOptionsView.selectFolder':
                final result = await FilePicker.platform.getDirectoryPath();
                if (result != null) {
                  controller.text = result;
                  widget.onChanged(controller.text);
                }
                break;
              case 'remove':
                widget.removeValue!(widget.value);
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
    );
  }
}

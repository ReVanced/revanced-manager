import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

// TODO(aabed): Implement rest of options

class BooleanPatchOption extends StatefulWidget {
  const BooleanPatchOption({super.key, required this.patchOption});

  final Option patchOption;

  @override
  State<BooleanPatchOption> createState() => _BooleanPatchOptionState();
}

class _BooleanPatchOptionState extends State<BooleanPatchOption> {
  late final bool? defaultValue;

  @override
  Widget build(BuildContext context) {
    return PatchOption(
      widget: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                widget.patchOption.title +
                    (widget.patchOption.required ? ' *' : ''),
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
              if (!widget.patchOption.required)
                IconButton(onPressed: () {}, icon: const Icon(Icons.delete))
            ],
          ),
          const SizedBox(height: 4),
          Text(
            widget.patchOption.description,
            style: TextStyle(
              fontSize: 14,
              color: Theme.of(context).colorScheme.onSecondaryContainer,
            ),
          ),
          const SizedBox(height: 4),
          Switch(
            value: widget.patchOption.value ?? false,
            onChanged: (bool value) {
              widget.patchOption.value = value;
            },
          ),
        ],
      ),
    );
  }
}

class StringPatchOption extends StatelessWidget {
  const StringPatchOption({super.key, required this.patchOption});

  final Option patchOption;

  @override
  Widget build(BuildContext context) {
    final controller = TextEditingController(text: patchOption.value ?? '');

    return PatchOption(
      widget: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                patchOption.title + (patchOption.required ? ' *' : ''),
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
              if (!patchOption.required)
                IconButton(onPressed: () {}, icon: const Icon(Icons.delete))
            ],
          ),
          const SizedBox(height: 4),
          Text(
            patchOption.description,
            style: TextStyle(
              fontSize: 14,
              color: Theme.of(context).colorScheme.onSecondaryContainer,
            ),
          ),
          const SizedBox(height: 4),
          TextField(
            controller: controller,
            decoration: InputDecoration(
              suffixIcon: PopupMenuButton(
                tooltip: FlutterI18n.translate(
                  context,
                  'patchOptionsView.tooltip',
                ),
                itemBuilder: (BuildContext context) {
                  return [
                    PopupMenuItem(
                      value: 'patchOptionsView.selectFilePath',
                      child: I18nText('patchOptionsView.selectFilePath'),
                    ),
                    PopupMenuItem(
                      value: 'patchOptionsView.selectFolder',
                      child: I18nText('patchOptionsView.selectFolder'),
                    ),
                  ];
                },
                onSelected: (String value) async {
                  switch (value) {
                    case 'patchOptionsView.selectFilePath':
                      final result = await FilePicker.platform.pickFiles();
                      if (result != null && result.files.single.path != null) {
                        patchOption.value = controller.text =
                            result.files.single.path.toString();
                      }
                      break;
                    case 'patchOptionsView.selectFolder':
                      final result =
                          await FilePicker.platform.getDirectoryPath();
                      if (result != null) {
                        patchOption.value = controller.text = result;
                      }
                      break;
                  }
                },
              ),
              hintStyle: TextStyle(
                fontSize: 14,
                color: Theme.of(context).colorScheme.onSecondaryContainer,
              ),
            ),
            onSubmitted: (String value) {
              // TODO(aabed): save default so it can be reset to null if necessary
              patchOption.value = value;
            },
          ),
        ],
      ),
    );
  }
}

class ListPatchOption extends StatelessWidget {
  const ListPatchOption({super.key, required this.patchOption});

  final Option patchOption;

  @override
  Widget build(BuildContext context) {
    return const Placeholder();
  }
}

class PatchOption extends StatelessWidget {
  const PatchOption({
    super.key,
    required this.widget,
  });

  final Widget widget;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: CustomCard(
        onTap: () {},
        child: Row(
          children: [Expanded(child: widget)],
        ),
      ),
    );
  }
}

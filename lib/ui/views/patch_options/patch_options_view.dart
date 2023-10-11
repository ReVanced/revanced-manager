import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/ui/views/patch_options/patch_options_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/patchesSelectorView/patch_options_fields.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:stacked/stacked.dart';

class PatchOptionsView extends StatelessWidget {
  const PatchOptionsView({super.key});

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<PatchOptionsViewModel>.reactive(
      onViewModelReady: (model) => model.initialize(),
      viewModelBuilder: () => PatchOptionsViewModel(),
      builder: (context, model, child) => GestureDetector(
        onTap: () => FocusScope.of(context).unfocus(),
        child: Scaffold(
          floatingActionButton: FloatingActionButton.extended(
            onPressed: () async {
              final bool saved = model.saveOptions(context);
              if (saved && context.mounted) {
                Navigator.pop(context);
              }
            },
            label: I18nText('patchOptionsView.saveOptions'),
            icon: const Icon(Icons.save),
          ),
          body: CustomScrollView(
            slivers: <Widget>[
              SliverAppBar(
                title: I18nText(
                  'patchOptionsView.viewTitle',
                  child: Text(
                    '',
                    style: GoogleFonts.inter(
                      color: Theme.of(context).textTheme.titleLarge!.color,
                    ),
                  ),
                ),
                actions: [
                  IconButton(
                    onPressed: () {
                      model.resetOptions();
                    },
                    icon: const Icon(
                      Icons.history,
                    ),
                  ),
                ],
              ),
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Column(
                    children: [
                      for (final Option option in model.visibleOptions)
                        if (option.optionClassType == 'StringPatchOption' ||
                            option.optionClassType == 'IntPatchOption')
                          IntAndStringPatchOption(
                            patchOption: option,
                            removeOption: (option) {
                              model.removeOption(option);
                            },
                            onChanged: (value, option) {
                              model.modifyOptions(value, option);
                            },
                          )
                        else if (option.optionClassType == 'BooleanPatchOption')
                          BooleanPatchOption(
                            patchOption: option,
                            removeOption: (option) {
                              model.removeOption(option);
                            },
                            onChanged: (value, option) {
                              model.modifyOptions(value, option);
                            },
                          )
                        else if (option.optionClassType ==
                                'StringListPatchOption' ||
                            option.optionClassType == 'IntListPatchOption' ||
                            option.optionClassType == 'LongListPatchOption')
                          IntStringLongListPatchOption(
                            patchOption: option,
                            removeOption: (option) {
                              model.removeOption(option);
                            },
                            onChanged: (value, option) {
                              model.modifyOptions(value, option);
                            },
                          )
                        else
                          UnsupportedPatchOption(
                            patchOption: option,
                          ),
                      if (model.visibleOptions.length !=
                          model.options.length) ...[
                        const SizedBox(
                          height: 8,
                        ),
                        CustomMaterialButton(
                          onPressed: () {
                            model.showAddOptionDialog(context);
                          },
                          label: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.add),
                              I18nText('patchOptionsView.addOptions'),
                            ],
                          ),
                        ),
                      ],
                      const SizedBox(
                        height: 80,
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

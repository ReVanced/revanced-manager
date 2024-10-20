import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/ui/views/patch_options/patch_options_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/patchesSelectorView/patch_options_fields.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_floating_action_button_extended.dart';
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
          floatingActionButton: HapticFloatingActionButtonExtended(
            onPressed: () async {
              final bool saved = model.saveOptions(context);
              if (saved && context.mounted) {
                Navigator.pop(context);
              }
            },
            label: Text(t.patchOptionsView.saveOptions),
            icon: const Icon(Icons.save),
          ),
          body: CustomScrollView(
            slivers: <Widget>[
              SliverAppBar(
                title: Text(
                  t.patchOptionsView.viewTitle,
                  style: GoogleFonts.inter(
                    color: Theme.of(context).textTheme.titleLarge!.color,
                  ),
                ),
              ),
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Column(
                    children: [
                      for (final Option option in model.modifiedOptions)
                        if (option.type == 'kotlin.String' ||
                            option.type == 'kotlin.Int')
                          IntAndStringPatchOption(
                            patchOption: option,
                            model: model,
                          )
                        else if (option.type == 'kotlin.Boolean')
                          BooleanPatchOption(
                            patchOption: option,
                            model: model,
                          )
                        else if (option.type == 'kotlin.collections.List<kotlin.String>' ||
                            option.type == 'kotlin.collections.List<kotlin.Int>' ||
                            option.type == 'kotlin.collections.List<kotlin.Long>')
                          IntStringLongListPatchOption(
                            patchOption: option,
                            model: model,
                          )
                        else
                          UnsupportedPatchOption(
                            patchOption: option,
                          ),
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

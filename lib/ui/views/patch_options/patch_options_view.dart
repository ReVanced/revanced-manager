import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/ui/views/patch_options/patch_options_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/patchesSelectorView/patch_options_fields.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:stacked/stacked.dart';

class PatchOptionsView extends StatelessWidget {
  const PatchOptionsView({super.key});

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<PatchOptionsViewModel>.reactive(
      onViewModelReady: (model) => model.initialize(),
      viewModelBuilder: () => PatchOptionsViewModel(),
      builder: (context, model, child) => Scaffold(
        floatingActionButton: FloatingActionButton.extended(
          onPressed: () => {
            // TODO(aabed): Implement save options
          },
          label: I18nText('patchOptionsView.saveOptions'),
          icon: const Icon(Icons.save),
        ),
        resizeToAvoidBottomInset: false,
        body: CustomScrollView(
          slivers: <Widget>[
            CustomSliverAppBar(
              title: I18nText(
                'patchOptionsView.viewTitle',
                child: Text(
                  '',
                  style: GoogleFonts.inter(
                    color: Theme.of(context).textTheme.titleLarge!.color,
                  ),
                ),
              ),
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Column(
                  children: [
                    for (final Option option
                        in model.options.where((option) => option.required))
                      if (option.optionClassType == 'StringPatchOption')
                        StringPatchOption(
                          patchOption: option,
                        )
                      else if (option.optionClassType == 'BooleanPatchOption')
                        BooleanPatchOption(
                          patchOption: option,
                        )
                      else if (option.optionClassType == 'ListPatchOption')
                        ListPatchOption(
                          patchOption: option,
                        ),
                    FilledButton.icon(
                      onPressed: () {
                        // TODO(aabed): Implement add option using dialog
                      },
                      icon: const Icon(Icons.add),
                      label: I18nText('patchOptionsView.addOption'),
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
    );
  }
}

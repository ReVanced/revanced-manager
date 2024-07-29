import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/patcherView/app_selector_card.dart';
import 'package:revanced_manager/ui/widgets/patcherView/patch_selector_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_floating_action_button_extended.dart';
import 'package:stacked/stacked.dart';

class PatcherView extends StatelessWidget {
  const PatcherView({super.key});

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<PatcherViewModel>.reactive(
      disposeViewModel: false,
      viewModelBuilder: () => locator<PatcherViewModel>(),
      builder: (context, model, child) => Scaffold(
        floatingActionButton: Visibility(
          visible: model.showPatchButton(),
          child: HapticFloatingActionButtonExtended(
            label: Text(t.patcherView.patchButton),
            icon: const Icon(Icons.build),
            onPressed: () async {
              if (model.checkRequiredPatchOption(context)) {
                final bool proceed = model.showRemovedPatchesDialog(context);
                if (proceed && context.mounted) {
                  model.showIncompatibleArchWarningDialog(context);
                }
              }
            },
          ),
        ),
        body: CustomScrollView(
          slivers: <Widget>[
            CustomSliverAppBar(
              isMainView: true,
              title: Text(
                t.patcherView.widgetTitle,
                style: GoogleFonts.inter(
                  color: Theme.of(context).textTheme.titleLarge!.color,
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.all(20.0),
              sliver: SliverList(
                delegate: SliverChildListDelegate.fixed(
                  <Widget>[
                    AppSelectorCard(
                      onPressed: () => {
                        model.navigateToAppSelector(),
                        model.ctx = context,
                      },
                    ),
                    const SizedBox(height: 16),
                    Opacity(
                      opacity: model.dimPatchesCard() ? 0.5 : 1,
                      child: PatchSelectorCard(
                        onPressed: model.dimPatchesCard()
                            ? () => {}
                            : () => model.navigateToPatchesSelector(),
                      ),
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

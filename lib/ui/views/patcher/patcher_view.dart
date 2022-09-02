import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/theme.dart';
import 'package:revanced_manager/ui/views/app_selector/app_selector_view.dart';
import 'package:revanced_manager/ui/views/installer/installer_view.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_view.dart';
import 'package:revanced_manager/ui/widgets/patcherView/app_selector_card.dart';
import 'package:revanced_manager/ui/widgets/patcherView/patch_selector_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:revanced_manager/ui/widgets/shared/open_container_wrapper.dart';
import 'package:stacked/stacked.dart';

class PatcherView extends StatelessWidget {
  const PatcherView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<PatcherViewModel>.reactive(
      disposeViewModel: false,
      viewModelBuilder: () => locator<PatcherViewModel>(),
      builder: (context, model, child) => Scaffold(
        floatingActionButton: Visibility(
          visible: model.showPatchButton(),
          child: OpenContainerWrapper(
            openBuilder: (_, __) => const InstallerView(),
            closedBuilder: (_, openContainer) => FloatingActionButton.extended(
              label: I18nText('patcherView.patchButton'),
              icon: const Icon(Icons.build),
              onPressed: openContainer,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              backgroundColor: Theme.of(context).colorScheme.secondary,
              foregroundColor: Theme.of(context).colorScheme.surface,
            ),
          ),
        ),
        body: CustomScrollView(
          slivers: <Widget>[
            CustomSliverAppBar(
              title: I18nText(
                'patcherView.widgetTitle',
                child: Text(
                  '',
                  style: GoogleFonts.inter(
                    color: Theme.of(context).textTheme.headline5!.color,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 20.0),
              sliver: SliverList(
                delegate: SliverChildListDelegate.fixed(
                  <Widget>[
                    OpenContainerWrapper(
                      openBuilder: (_, __) => const AppSelectorView(),
                      closedBuilder: (_, openContainer) => AppSelectorCard(
                        onPressed: openContainer,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Opacity(
                      opacity: isDark
                          ? (model.dimPatchesCard() ? 0.5 : 1)
                          : (model.dimPatchesCard() ? 0.75 : 1),
                      child: OpenContainerWrapper(
                        openBuilder: (_, __) => const PatchesSelectorView(),
                        closedBuilder: (_, openContainer) => PatchSelectorCard(
                          onPressed:
                              model.dimPatchesCard() ? () => {} : openContainer,
                        ),
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

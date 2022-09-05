import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/patcherView/app_selector_card.dart';
import 'package:revanced_manager/ui/widgets/patcherView/patch_selector_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
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
          child: FloatingActionButton.extended(
            label: I18nText('patcherView.patchButton'),
            icon: const Icon(Icons.build),
            onPressed: () => model.navigateToInstaller(),
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
                    color: Theme.of(context).textTheme.headline6!.color,
                  ),
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 20.0),
              sliver: SliverList(
                delegate: SliverChildListDelegate.fixed(
                  <Widget>[
                    AppSelectorCard(
                      onPressed: () => model.navigateToAppSelector(),
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

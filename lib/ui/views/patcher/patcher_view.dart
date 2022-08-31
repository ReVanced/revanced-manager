import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/theme.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/patcherView/app_selector_card.dart';
import 'package:revanced_manager/ui/widgets/patcherView/patch_selector_card.dart';
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
            shape: const RoundedRectangleBorder(
              borderRadius: BorderRadius.all(
                Radius.circular(16.0),
              ),
            ),
            backgroundColor: Theme.of(context).colorScheme.secondary,
            foregroundColor: Colors.white,
          ),
        ),
        body: CustomScrollView(
          slivers: <Widget>[
            SliverAppBar(
              pinned: true,
              snap: false,
              floating: false,
              expandedHeight: 100.0,
              automaticallyImplyLeading: false,
              backgroundColor: MaterialStateColor.resolveWith(
                (states) => states.contains(MaterialState.scrolledUnder)
                    ? isDark
                        ? Theme.of(context).colorScheme.primary
                        : Theme.of(context).navigationBarTheme.backgroundColor!
                    : Theme.of(context).scaffoldBackgroundColor,
              ),
              flexibleSpace: FlexibleSpaceBar(
                titlePadding: const EdgeInsets.symmetric(
                  vertical: 23.0,
                  horizontal: 20.0,
                ),
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
            ),
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 20.0),
              sliver: SliverList(
                delegate: SliverChildListDelegate.fixed(
                  <Widget>[
                    AppSelectorCard(
                      onPressed: model.navigateToAppSelector,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                    const SizedBox(height: 16),
                    Opacity(
                      opacity: isDark
                          ? (model.dimPatchesCard() ? 0.5 : 1)
                          : (model.dimPatchesCard() ? 0.75 : 1),
                      child: PatchSelectorCard(
                        onPressed: model.dimPatchesCard()
                            ? () => {}
                            : model.navigateToPatchesSelector,
                        color: Theme.of(context).colorScheme.primary,
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

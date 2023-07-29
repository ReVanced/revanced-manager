import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/ui/views/installer/installer_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/installerView/gradient_progress_indicator.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:stacked/stacked.dart';

class InstallerView extends StatelessWidget {
  const InstallerView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<InstallerViewModel>.reactive(
      onViewModelReady: (model) => model.initialize(context),
      viewModelBuilder: () => InstallerViewModel(),
      builder: (context, model, child) => WillPopScope(
        child: SafeArea(
          top: false,
          bottom: false,
          child: Scaffold(
            body: CustomScrollView(
              controller: model.scrollController,
              slivers: <Widget>[
                CustomSliverAppBar(
                  title: Text(
                    model.headerLogs,
                    style: GoogleFonts.inter(
                      color: Theme.of(context).textTheme.titleLarge!.color,
                    ),
                  ),
                  onBackButtonPressed: () => model.onWillPop(context),
                  bottom: PreferredSize(
                    preferredSize: const Size(double.infinity, 1.0),
                    child: GradientProgressIndicator(progress: model.progress),
                  ),
                ),
                SliverPadding(
                  padding: const EdgeInsets.all(20.0),
                  sliver: SliverList(
                    delegate: SliverChildListDelegate.fixed(
                      <Widget>[
                        CustomCard(
                          child: Text(
                            model.logs,
                            style: GoogleFonts.jetBrainsMono(
                              fontSize: 13,
                              height: 1.5,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                SliverFillRemaining(
                  hasScrollBody: false,
                  child: Align(
                    alignment: Alignment.bottomCenter,
                    child: Visibility(
                      visible: !model.isPatching && !model.hasErrors,
                      child: Padding(
                        padding: const EdgeInsets.all(20.0).copyWith(top: 0.0),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.end,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Visibility(
                              visible: !model.isPatching && !model.hasErrors,
                              child: IconButton(
                                tooltip: FlutterI18n.translate(
                                  context,
                                  'installerView.saveApkButtonTooltip',
                                ),
                                icon: const Icon(Icons.save),
                                onPressed: () => model.onButtonPressed(0),
                              ),
                            ),
                            Visibility(
                              visible: !model.isPatching && !model.hasErrors,
                              child: IconButton(
                                tooltip: FlutterI18n.translate(
                                  context,
                                  'installerView.shareLogButtonTooltip',
                                ),
                                icon: const Icon(
                                  FontAwesomeIcons.solidFileLines,
                                ),
                                iconSize: 20,
                                onPressed: () => model.onButtonPressed(1),
                              ),
                            ),
                            Visibility(
                              visible: model.isInstalled,
                              child: CustomMaterialButton(
                                label: I18nText('installerView.openButton'),
                                isExpanded: true,
                                onPressed: () {
                                  model.openApp();
                                  model.cleanPatcher();
                                  Navigator.of(context).pop();
                                },
                              ),
                            ),
                            Visibility(
                              visible: !model.isInstalled && model.isRooted,
                              child: CustomMaterialButton(
                                isFilled: false,
                                label: I18nText(
                                  'installerView.installRootButton',
                                ),
                                isExpanded: true,
                                onPressed: () => model.installResult(
                                  context,
                                  true,
                                ),
                              ),
                            ),
                            Visibility(
                              visible: !model.isInstalled,
                              child: const SizedBox(
                                width: 8,
                              ),
                            ),
                            Visibility(
                              visible: !model.isInstalled,
                              child: CustomMaterialButton(
                                label: I18nText('installerView.installButton'),
                                isExpanded: true,
                                onPressed: () => model.installResult(
                                  context,
                                  false,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
                SliverFillRemaining(
                  hasScrollBody: false,
                  child: SizedBox(
                    height: MediaQuery.of(context).viewPadding.bottom,
                  ),
                ),
              ],
            ),
          ),
        ),
        onWillPop: () => model.onWillPop(context),
      ),
    );
  }
}

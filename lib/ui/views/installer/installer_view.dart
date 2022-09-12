import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/ui/views/installer/installer_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/installerView/custom_material_button.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_popup_menu.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:stacked/stacked.dart';

class InstallerView extends StatelessWidget {
  const InstallerView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<InstallerViewModel>.reactive(
      onModelReady: (model) => model.initialize(context),
      viewModelBuilder: () => InstallerViewModel(),
      builder: (context, model, child) => WillPopScope(
        child: Scaffold(
          body: CustomScrollView(
            controller: model.scrollController,
            slivers: <Widget>[
              CustomSliverAppBar(
                title: Text(
                  model.headerLogs,
                  style: GoogleFonts.inter(
                    color: Theme.of(context).textTheme.headline6!.color,
                  ),
                ),
                actions: <Widget>[
                  Visibility(
                    visible: !model.isPatching && model.hasErrors,
                    child: CustomPopupMenu(
                      onSelected: (value) => model.onMenuSelection(value),
                      children: {
                        0: I18nText(
                          'installerView.shareApkMenuOption',
                          child: const Text(
                            '',
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        1: I18nText(
                          'installerView.shareLogMenuOption',
                          child: const Text(
                            '',
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      },
                    ),
                  ),
                ],
                bottom: PreferredSize(
                  preferredSize: const Size(double.infinity, 1.0),
                  child: LinearProgressIndicator(
                    color: Theme.of(context).colorScheme.primary,
                    backgroundColor:
                        Theme.of(context).colorScheme.primaryContainer,
                    value: model.progress,
                  ),
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
                            visible: !model.isInstalled,
                            child: CustomMaterialButton(
                              isFilled: false,
                              label:
                                  I18nText('installerView.installRootButton'),
                              isExpanded: true,
                              onPressed: () => model.installResult(true),
                            ),
                          ),
                          Visibility(
                            visible: !model.isInstalled,
                            child: const SizedBox(
                              width: 16,
                            ),
                          ),
                          Visibility(
                            visible: !model.isInstalled,
                            child: CustomMaterialButton(
                              label: I18nText('installerView.installButton'),
                              isExpanded: true,
                              onPressed: () => model.installResult(false),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
        onWillPop: () async {
          if (!model.isPatching) {
            model.cleanPatcher();
            Navigator.of(context).pop();
          }
          return false;
        },
      ),
    );
  }
}

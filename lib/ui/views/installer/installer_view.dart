import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/ui/views/installer/installer_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/installerView/gradient_progress_indicator.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
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
            floatingActionButton: Visibility(
              visible: !model.isPatching && !model.hasErrors,
              child: FloatingActionButton.extended(
                label: I18nText(
                  model.isInstalled
                      ? 'installerView.openButton'
                      : 'installerView.installButton',
                ),
                icon: model.isInstalled
                    ? const Icon(Icons.open_in_new)
                    : const Icon(Icons.file_download_outlined),
                onPressed: model.isInstalled
                    ? () => {
                          model.openApp(),
                          model.cleanPatcher(),
                          Navigator.of(context).pop(),
                        }
                    : () => model.installTypeDialog(context),
                elevation: 0,
              ),
            ),
            floatingActionButtonLocation:
                FloatingActionButtonLocation.endContained,
            bottomNavigationBar: Visibility(
              visible: !model.isPatching,
              child: BottomAppBar(
                child: Row(
                  children: <Widget>[
                    Visibility(
                      visible: !model.hasErrors,
                      child: IconButton.filledTonal(
                        tooltip: FlutterI18n.translate(
                          context,
                          'installerView.exportApkButtonTooltip',
                        ),
                        icon: const Icon(Icons.save),
                        onPressed: () => model.onButtonPressed(0),
                      ),
                    ),
                    IconButton.filledTonal(
                      tooltip: FlutterI18n.translate(
                        context,
                        'installerView.exportLogButtonTooltip',
                      ),
                      icon: const Icon(Icons.post_add),
                      onPressed: () => model.onButtonPressed(1),
                    ),
                  ],
                ),
              ),
            ),
            body: CustomScrollView(
              controller: model.scrollController,
              slivers: <Widget>[
                CustomSliverAppBar(
                  title: Text(
                    model.headerLogs,
                    style: GoogleFonts.inter(
                      color: Theme.of(context).textTheme.titleLarge!.color,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
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
              ],
            ),
          ),
        ),
        onWillPop: () => model.onWillPop(context),
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/installer/installer_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/installerView/gradient_progress_indicator.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_floating_action_button_extended.dart';
import 'package:stacked/stacked.dart';

class InstallerView extends StatelessWidget {
  const InstallerView({super.key});

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<InstallerViewModel>.reactive(
      onViewModelReady: (model) => model.initialize(context),
      viewModelBuilder: () => InstallerViewModel(),
      builder: (context, model, child) => PopScope<Object?>(
        canPop: !model.isPatching,
        onPopInvokedWithResult: (bool didPop, Object? result) {
          if (didPop) {
            model.onPop();
          } else {
            model.onPopAttempt(context);
          }
        },
        child: Scaffold(
          floatingActionButton: Visibility(
            visible:
                !model.isPatching && !model.hasErrors && !model.isInstalling,
            child: HapticFloatingActionButtonExtended(
              label: Text(
                model.isInstalled
                    ? t.installerView.openButton
                    : t.installerView.installButton,
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
                      tooltip: t.installerView.exportApkButtonTooltip,
                      icon: const Icon(Icons.save),
                      onPressed: () => model.onButtonPressed(0),
                    ),
                  ),
                  IconButton.filledTonal(
                    tooltip: t.installerView.exportLogButtonTooltip,
                    icon: const Icon(Icons.post_add),
                    onPressed: () => model.onButtonPressed(1),
                  ),
                ],
              ),
            ),
          ),
          body: NotificationListener<ScrollNotification>(
            onNotification: model.handleAutoScrollNotification,
            child: Scaffold(
              body: CustomScrollView(
                key: model.logCustomScrollKey,
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
                    onBackButtonPressed: () => Navigator.maybePop(context),
                    bottom: PreferredSize(
                      preferredSize: const Size(double.infinity, 1.0),
                      child: GradientProgressIndicator(
                        progress: model.progress,
                      ),
                    ),
                  ),
                  SliverPadding(
                    padding: EdgeInsets.only(
                      left: 20,
                      right: 20,
                      top: 20,
                      bottom: MediaQuery.paddingOf(context).bottom,
                    ),
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
              floatingActionButtonLocation:
                  FloatingActionButtonLocation.endDocked,
              floatingActionButton: Visibility(
                visible: model.showAutoScrollButton,
                child: Align(
                  alignment: const Alignment(1, 0.85),
                  child: FloatingActionButton(
                    onPressed: model.scrollToBottom,
                    child: const Icon(Icons.arrow_downward_rounded),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

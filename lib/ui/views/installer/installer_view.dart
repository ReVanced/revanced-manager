import 'package:flutter/material.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/ui/views/installer/installer_viewmodel.dart';
import 'package:stacked/stacked.dart';

class InstallerView extends StatelessWidget {
  InstallerView({Key? key}) : super(key: key);
  final ScrollController _controller = ScrollController();

  @override
  Widget build(BuildContext context) {
    WidgetsBinding.instance.addPostFrameCallback(
      (_) => _controller.jumpTo(_controller.position.maxScrollExtent),
    );
    return ViewModelBuilder<InstallerViewModel>.reactive(
      disposeViewModel: false,
      onModelReady: (model) => model.initialize(),
      viewModelBuilder: () => locator<InstallerViewModel>(),
      builder: (context, model, child) => WillStartForegroundTask(
        onWillStart: () async => model.isPatching,
        androidNotificationOptions: AndroidNotificationOptions(
          channelId: 'revanced-patcher-patching',
          channelName: 'Patching',
          channelDescription: 'This notification appears when the patching '
              'foreground service is running.',
          channelImportance: NotificationChannelImportance.LOW,
          priority: NotificationPriority.LOW,
        ),
        notificationTitle: 'Patching',
        notificationText: 'ReVanced Manager is patching',
        callback: () => {},
        child: WillPopScope(
          child: Scaffold(
            body: SafeArea(
              child: LayoutBuilder(
                builder: (context, constraints) => SingleChildScrollView(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  controller: _controller,
                  child: ConstrainedBox(
                    constraints: BoxConstraints(
                      minWidth: constraints.maxWidth,
                      minHeight: constraints.maxHeight,
                    ),
                    child: IntrinsicHeight(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          I18nText(
                            'installerView.widgetTitle',
                            child: Text(
                              '',
                              style: Theme.of(context).textTheme.headline5,
                            ),
                          ),
                          Padding(
                            padding: const EdgeInsets.symmetric(
                              vertical: 16.0,
                              horizontal: 4.0,
                            ),
                            child: LinearProgressIndicator(
                              color: Theme.of(context).colorScheme.secondary,
                              backgroundColor: Colors.white,
                              value: model.progress,
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.all(12.0),
                            width: double.infinity,
                            decoration: BoxDecoration(
                              color: Theme.of(context).colorScheme.primary,
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: SelectableText(
                              model.logs,
                              style: const TextStyle(
                                fontFamily: 'monospace',
                                fontSize: 15,
                                height: 1.5,
                              ),
                            ),
                          ),
                          const Spacer(),
                          Visibility(
                            visible: model.showButtons,
                            child: Row(
                              children: [
                                Expanded(
                                  child: MaterialButton(
                                    textColor: Colors.white,
                                    color:
                                        Theme.of(context).colorScheme.secondary,
                                    padding: const EdgeInsets.symmetric(
                                      vertical: 12,
                                      horizontal: 8,
                                    ),
                                    shape: RoundedRectangleBorder(
                                      borderRadius: BorderRadius.circular(12),
                                    ),
                                    onPressed: () => model.installResult(),
                                    child: I18nText(
                                      'installerView.installButton',
                                    ),
                                  ),
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: MaterialButton(
                                    textColor: Colors.white,
                                    color:
                                        Theme.of(context).colorScheme.secondary,
                                    padding: const EdgeInsets.symmetric(
                                      vertical: 12,
                                      horizontal: 8,
                                    ),
                                    shape: RoundedRectangleBorder(
                                      borderRadius: BorderRadius.circular(12),
                                    ),
                                    onPressed: () => model.shareResult(),
                                    child: I18nText(
                                      'installerView.shareButton',
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
          onWillPop: () async {
            if (!model.isPatching) {
              model.cleanWorkplace();
              Navigator.of(context).pop();
            }
            return false;
          },
        ),
      ),
    );
  }
}

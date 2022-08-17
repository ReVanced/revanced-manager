import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
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
      builder: (context, model, child) => WillPopScope(
        child: Scaffold(
          floatingActionButton: Visibility(
            visible: !model.isPatching,
            child: FloatingActionButton.extended(
              onPressed: () =>
                  model.isInstalled ? model.openApp() : model.installResult(),
              label: I18nText(model.isInstalled
                  ? 'installerView.fabOpenButton'
                  : 'installerView.fabInstallButton'),
              icon: model.isInstalled
                  ? const Icon(Icons.open_in_new)
                  : const Icon(Icons.install_mobile),
              backgroundColor: Theme.of(context).colorScheme.secondary,
              foregroundColor: Colors.white,
            ),
          ),
          body: SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              controller: _controller,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      I18nText(
                        'installerView.widgetTitle',
                        child: Text(
                          '',
                          style: Theme.of(context).textTheme.headline5,
                        ),
                      ),
                      Visibility(
                        visible: !model.isPatching,
                        child: IconButton(
                          icon: const Icon(Icons.share),
                          onPressed: () => model.shareResult(),
                        ),
                      ),
                    ],
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
        ),
        onWillPop: () async {
          if (!model.isPatching) {
            model.cleanWorkplace();
            Navigator.of(context).pop();
          }
          return false;
        },
      ),
    );
  }
}

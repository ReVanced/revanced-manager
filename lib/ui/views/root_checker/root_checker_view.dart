import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/ui/views/root_checker/root_checker_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/rootCheckerView/magisk_button.dart';
import 'package:stacked/stacked.dart';

class RootCheckerView extends StatelessWidget {
  const RootCheckerView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<RootCheckerViewModel>.reactive(
      onModelReady: (model) => model.initialize(),
      viewModelBuilder: () => RootCheckerViewModel(),
      builder: (context, model, child) => Scaffold(
        floatingActionButton: FloatingActionButton.extended(
          label: I18nText('rootCheckerView.nonRootButton'),
          icon: const Icon(Icons.keyboard_arrow_right),
          onPressed: () => model.navigateAsNonRoot(),
        ),
        body: Container(
          height: double.infinity,
          padding: const EdgeInsets.symmetric(vertical: 8.0, horizontal: 28.0),
          child: Column(
            children: <Widget>[
              const SizedBox(height: 120),
              I18nText(
                'rootCheckerView.widgetTitle',
                child: Text(
                  '',
                  style: GoogleFonts.jetBrainsMono(
                    fontSize: 24,
                  ),
                ),
              ),
              const SizedBox(height: 24),
              I18nText(
                'rootCheckerView.widgetDescription',
                child: const Text(
                  '',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 17,
                    letterSpacing: 1.1,
                  ),
                ),
              ),
              Expanded(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    MagiskButton(
                      onPressed: () => model.navigateAsRoot(),
                    ),
                    I18nText(
                      'rootCheckerView.grantedPermission',
                      translationParams: {
                        'isRooted': model.isRooted.toString(),
                      },
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

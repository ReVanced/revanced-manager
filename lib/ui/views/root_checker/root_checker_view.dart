import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/ui/views/root_checker/root_checker_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/magisk_button.dart';
import 'package:stacked/stacked.dart';

class RootCheckerView extends StatelessWidget {
  const RootCheckerView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<RootCheckerViewModel>.reactive(
      onModelReady: (model) => model.initialize,
      viewModelBuilder: () => RootCheckerViewModel(),
      builder: (context, model, child) => Scaffold(
        floatingActionButton: Column(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            const Text('nonroot'),
            const SizedBox(height: 8),
            FloatingActionButton(
              onPressed: model.navigateToHome,
              backgroundColor: Theme.of(context).colorScheme.secondary,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(48),
              ),
              child: const Icon(
                Icons.keyboard_arrow_right,
                size: 32,
              ),
            ),
          ],
        ),
        body: Container(
          height: double.infinity,
          padding: const EdgeInsets.symmetric(vertical: 8.0, horizontal: 28.0),
          child: Column(
            mainAxisSize: MainAxisSize.max,
            children: [
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
                child: Text(
                  '',
                  textAlign: TextAlign.center,
                  style: GoogleFonts.roboto(
                    fontSize: 17,
                    letterSpacing: 1.1,
                  ),
                ),
              ),
              const SizedBox(height: 170),
              MagiskButton(
                onPressed: () {
                  model.getMagiskPermissions();
                  Future.delayed(const Duration(seconds: 5), () {
                    model.checkRoot();
                  });
                },
              ),
              Text(
                "Magisk permission granted: ${model.isRooted.toString()}",
                style: GoogleFonts.poppins(),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

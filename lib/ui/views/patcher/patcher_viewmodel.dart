import 'package:injectable/injectable.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:stacked/stacked.dart';

@lazySingleton
class PatcherViewModel extends BaseViewModel {
  PatchedApplication? selectedApp;
  List<Patch> selectedPatches = [];

  bool showPatchButton() {
    return selectedPatches.isNotEmpty;
  }

  bool dimPatchesCard() {
    return selectedApp == null;
  }
}

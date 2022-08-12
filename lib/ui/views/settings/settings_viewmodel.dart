import 'package:stacked/stacked.dart';

class SettingsViewModel extends BaseViewModel {
  void setLanguage(String language) {
    notifyListeners();
  }
}

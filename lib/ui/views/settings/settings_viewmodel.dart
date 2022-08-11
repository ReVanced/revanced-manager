import 'package:stacked/stacked.dart';

class SettingsViewModel extends BaseViewModel {
  bool isDarkMode = true;
  void setLanguage(String language) {
    notifyListeners();
  }
}

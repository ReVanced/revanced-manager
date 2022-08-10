import 'package:stacked/stacked.dart';

class SettingsViewModel extends BaseViewModel {
  bool isDarkMode = true;
  bool isDynamicColors = false;

  void toggleDynamicColors() {
    isDynamicColors = !isDynamicColors;
    notifyListeners();
  }

  void toggleTheme() {
    isDarkMode = !isDarkMode;
    notifyListeners();
  }

  void setLanguage(String language) {
    notifyListeners();
  }
}

// ignore_for_file: use_build_context_synchronously

import 'package:dynamic_themes/dynamic_themes.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_section.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_radio_list_tile.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_switch_list_tile.dart';

class SUpdateThemeUI extends StatefulWidget {
  const SUpdateThemeUI({super.key});

  @override
  State<SUpdateThemeUI> createState() => _SUpdateThemeUIState();
}

class _SUpdateThemeUIState extends State<SUpdateThemeUI> {
  final ManagerAPI managerAPI = locator<ManagerAPI>();

  @override
  Widget build(BuildContext context) {
    return SettingsSection(
      title: t.settingsView.appearanceSectionTitle,
      children: <Widget>[
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: Text(
            t.settingsView.themeModeLabel,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w500,
            ),
          ),
          trailing: FilledButton(
            onPressed: () => {showThemeDialog(context)},
            child: getThemeModeName(),
          ),
          onTap: () => {showThemeDialog(context)},
        ),
        if (managerAPI.isDynamicThemeAvailable)
          HapticSwitchListTile(
            contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
            title: Text(
              t.settingsView.dynamicThemeLabel,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
            subtitle: Text(t.settingsView.dynamicThemeHint),
            value: getDynamicThemeStatus(),
            onChanged: (value) => {
              setUseDynamicTheme(
                context,
                value,
              ),
            },
          ),
      ],
    );
  }

  bool getDynamicThemeStatus() {
    return managerAPI.getUseDynamicTheme();
  }

  Future<void> setUseDynamicTheme(BuildContext context, bool value) async {
    await managerAPI.setUseDynamicTheme(value);
    final int currentTheme = (DynamicTheme.of(context)!.themeId ~/ 2) * 2;
    await DynamicTheme.of(context)!.setTheme(currentTheme + (value ? 1 : 0));
    setState(() {});
  }

  int getThemeMode() {
    return managerAPI.getThemeMode();
  }

  Future<void> setThemeMode(BuildContext context, int value) async {
    await managerAPI.setThemeMode(value);
    final bool isDynamicTheme = DynamicTheme.of(context)!.themeId.isEven;
    await DynamicTheme.of(context)!
        .setTheme(value * 2 + (isDynamicTheme ? 0 : 1));
    final bool isLight = value != 2 &&
        (value == 1 ||
            DynamicTheme.of(context)!.theme.brightness == Brightness.light);
    SystemChrome.setSystemUIOverlayStyle(
      SystemUiOverlayStyle(
        systemNavigationBarIconBrightness:
            isLight ? Brightness.dark : Brightness.light,
      ),
    );
    setState(() {});
  }

  Text getThemeModeName() {
    switch (getThemeMode()) {
      case 0:
        return Text(t.settingsView.systemThemeLabel);
      case 1:
        return Text(t.settingsView.lightThemeLabel);
      case 2:
        return Text(t.settingsView.darkThemeLabel);
      default:
        return Text(t.settingsView.systemThemeLabel);
    }
  }

  Future<void> showThemeDialog(BuildContext context) async {
    final ValueNotifier<int> newTheme = ValueNotifier(getThemeMode());

    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(t.settingsView.themeModeLabel),
        icon: const Icon(Icons.palette),
        contentPadding: const EdgeInsets.symmetric(vertical: 16),
        content: SingleChildScrollView(
          child: ValueListenableBuilder(
            valueListenable: newTheme,
            builder: (context, value, child) {
              return Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  HapticRadioListTile(
                    title: Text(t.settingsView.systemThemeLabel),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 16),
                    value: 0,
                    groupValue: value,
                    onChanged: (value) {
                      newTheme.value = value!;
                    },
                  ),
                  HapticRadioListTile(
                    title: Text(t.settingsView.lightThemeLabel),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 16),
                    value: 1,
                    groupValue: value,
                    onChanged: (value) {
                      newTheme.value = value!;
                    },
                  ),
                  HapticRadioListTile(
                    title: Text(t.settingsView.darkThemeLabel),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 16),
                    value: 2,
                    groupValue: value,
                    onChanged: (value) {
                      newTheme.value = value!;
                    },
                  ),
                ],
              );
            },
          ),
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
            },
            child: Text(t.cancelButton),
          ),
          FilledButton(
            onPressed: () {
              setThemeMode(context, newTheme.value);
              Navigator.of(context).pop();
            },
            child: Text(t.okButton),
          ),
        ],
      ),
    );
  }
}

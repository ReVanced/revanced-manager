import 'package:device_apps/device_apps.dart';
import 'package:flutter/material.dart';
import 'package:revanced_manager_flutter/ui/widgets/installed_app_item.dart';

class AppSelectorScreen extends StatefulWidget {
  const AppSelectorScreen({Key? key}) : super(key: key);

  @override
  State<AppSelectorScreen> createState() => _AppSelectorScreenState();
}

class _AppSelectorScreenState extends State<AppSelectorScreen> {
  List<Application> apps = [];

  void getApps() async {
    apps = await DeviceApps.getInstalledApplications();
    setState(() {});
  }

  @override
  void initState() {
    getApps();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: apps.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : ListView.builder(
                itemCount: apps.length,
                itemBuilder: (context, index) {
                  return InstalledAppItem(
                    name: apps[index].appName,
                    pkgName: apps[index].packageName,
                  );
                },
              ),
      ),
    );
  }
}

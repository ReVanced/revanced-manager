import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:wakelock_plus/wakelock_plus.dart';
import 'screens/headunit_screen.dart';
import 'theme/car_theme.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Force Landscape Orientation for Headunit
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.landscapeLeft,
    DeviceOrientation.landscapeRight,
  ]);

  // Keep screen awake while driving
  try {
    await WakelockPlus.enable();
  } catch (e) {
    print('Wakelock error: $e');
  }

  runApp(const YouTubeCarApp());
}

class YouTubeCarApp extends StatelessWidget {
  const YouTubeCarApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'YouTube Car Headunit',
      debugShowCheckedModeBanner: false,
      theme: CarTheme.darkTheme,
      home: const HeadunitScreen(),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager_flutter/ui/screens/home_screen.dart';
import 'package:revanced_manager_flutter/ui/screens/patcher_screen.dart';
import 'constants.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'ReVanced Manager',
      theme: ThemeData.light().copyWith(
        navigationBarTheme: NavigationBarThemeData(
          labelTextStyle: MaterialStateProperty.all(
            GoogleFonts.roboto(
              fontSize: 12,
            ),
          ),
        ),
        backgroundColor: Colors.red,
        textTheme: GoogleFonts.interTextTheme(
          Theme.of(context).textTheme,
        ),
        useMaterial3: true,
        colorScheme: const ColorScheme.light(
          primary: purple40,
          secondary: purpleGrey40,
          tertiary: pink40,
          background: Colors.red,
        ),
      ),
      darkTheme: ThemeData.dark().copyWith(
        navigationBarTheme: NavigationBarThemeData(
          iconTheme: MaterialStateProperty.all(const IconThemeData(
            color: Colors.white,
          )),
          indicatorColor: const Color(0xff223144),
          backgroundColor: const Color(0x1b222b6b),
          labelTextStyle: MaterialStateProperty.all(
            GoogleFonts.roboto(
              fontSize: 12,
            ),
          ),
        ),
        backgroundColor: Colors.red,
        useMaterial3: true,
        scaffoldBackgroundColor: const Color(0xff0A0D11),
        colorScheme: const ColorScheme.dark(
          primary: purple80,
          secondary: purpleGrey80,
          tertiary: pink80,
          background: Colors.red,
        ),
      ),
      home: const Navigation(),
    );
  }
}

class Navigation extends StatefulWidget {
  const Navigation({Key? key}) : super(key: key);

  @override
  State<Navigation> createState() => _NavigationState();
}

class _NavigationState extends State<Navigation> {
  int currentPageIndex = 0;
  final List<Widget> screens = [
    HomeScreen(),
    PatcherScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: screens[currentPageIndex],
      bottomNavigationBar: NavigationBar(
        onDestinationSelected: (int index) {
          setState(() {
            currentPageIndex = index;
          });
        },
        selectedIndex: currentPageIndex,
        destinations: const <Widget>[
          NavigationDestination(
            icon: Icon(Icons.dashboard),
            label: "Dashboard",
          ),
          NavigationDestination(
            icon: Icon(Icons.build),
            label: "Patcher",
          ),
        ],
      ),
    );
  }
}

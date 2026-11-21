import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:latlong2/latlong.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:youtube_car_headunit/controllers/feed_controller.dart';
import 'package:youtube_car_headunit/controllers/location_controller.dart';
import 'package:youtube_car_headunit/controllers/navigation_controller.dart';
import 'package:youtube_car_headunit/controllers/playback_controller.dart';
import 'package:youtube_car_headunit/models/navigation.dart';
import 'package:youtube_car_headunit/screens/headunit_screen.dart';
import 'package:youtube_car_headunit/services/car_audio_handler.dart';
import 'package:youtube_car_headunit/services/geocoding_service.dart';
import 'package:youtube_car_headunit/services/recent_destinations.dart';
import 'package:youtube_car_headunit/services/routing_service.dart';
import 'package:youtube_car_headunit/services/settings_store.dart';
import 'package:youtube_car_headunit/services/video_source.dart';
import 'package:youtube_car_headunit/services/voice_guidance.dart';
import 'package:youtube_car_headunit/services/youtube_worker.dart';
import 'package:youtube_car_headunit/theme/car_theme.dart';

/// A street running east from the start, then a left turn north to the destination.
const _start = LatLng(-6.2, 106.8);
const _corner = LatLng(-6.2, 106.81);
const _end = LatLng(-6.19, 106.81);
const _monas = Place(
  name: 'Monas',
  address: 'Gambir, Jakarta Pusat',
  location: _end,
);

NavRoute _route({
  Duration duration = const Duration(minutes: 4),
  int via = 0,
  RouteProvider provider = RouteProvider.osrm,
}) {
  final points = <LatLng>[
    for (var i = 0; i <= 10; i++) LatLng(-6.2 + via * 0.0005, 106.8 + i * 0.001),
    for (var i = 1; i <= 10; i++) LatLng(-6.2 + i * 0.001, 106.81),
  ];
  const distance = Distance();
  final cumulative = <double>[0];
  for (var i = 1; i < points.length; i++) {
    cumulative.add(cumulative.last + distance.as(LengthUnit.Meter, points[i - 1], points[i]));
  }
  return NavRoute(
    points: points,
    cumulative: cumulative,
    distance: cumulative.last,
    duration: duration,
    summary: via == 0 ? 'Jalan Merdeka' : 'Jalan Tol Dalam Kota',
    hasTolls: via != 0,
    provider: provider,
    // Mapbox reports traffic: a jam on the second half of the first street.
    congestion: provider == RouteProvider.mapbox
        ? [
            for (var i = 0; i < points.length - 1; i++)
              i >= 5 && i < 10 ? Congestion.heavy : Congestion.low,
          ]
        : null,
    steps: [
      RouteStep(
        type: ManeuverType.depart,
        name: 'Jalan Merdeka',
        instruction: 'Ke timur di Jalan Merdeka',
        location: points.first,
        pointIndex: 0,
        distance: cumulative[10],
        duration: duration ~/ 2,
      ),
      RouteStep(
        type: ManeuverType.turn,
        modifier: ManeuverModifier.left,
        name: 'Jalan Thamrin',
        instruction: 'Belok kiri ke Jalan Thamrin',
        location: _corner,
        pointIndex: 10,
        distance: cumulative.last - cumulative[10],
        duration: duration ~/ 2,
      ),
      RouteStep(
        type: ManeuverType.arrive,
        instruction: 'Anda telah tiba di tujuan',
        location: points.last,
        pointIndex: points.length - 1,
        distance: 0,
        duration: Duration.zero,
      ),
    ],
  );
}

class _FakeGeocoding extends GeocodingService {
  final List<String> queries = [];

  @override
  Future<List<Place>> search(String query, {LatLng? near, int limit = 8}) async {
    queries.add(query);
    return const [
      _monas,
      Place(name: 'Monumen Pancasila', location: LatLng(-6.3, 106.9)),
    ];
  }

  @override
  Future<Place?> reverse(LatLng point) async =>
      Place(name: 'Jalan Kebon Sirih', location: point);
}

class _FakeRouting extends RoutingService {
  _FakeRouting({this.mapbox = false, this.single}) : super(mapboxToken: '');

  final bool mapbox;

  /// Replaces the default route and its alternative.
  final NavRoute? single;

  @override
  bool get hasMapbox => mapbox;

  @override
  Future<RouteResult> route(
    LatLng from,
    LatLng to, {
    RouteProvider provider = RouteProvider.osrm,
    bool avoidTolls = false,
    bool alternatives = true,
    double? heading,
  }) async {
    final used = mapbox ? provider : RouteProvider.osrm;
    if (single case final route?) {
      return RouteResult(provider: used, routes: [route]);
    }
    return RouteResult(
      provider: used,
      routes: [
        _route(provider: used),
        if (alternatives)
          _route(duration: const Duration(minutes: 6), via: 1, provider: used),
      ],
    );
  }
}

class _NoVideos implements VideoSource {
  @override
  Future<VideoPage> search(String query) async => const VideoPage([]);

  @override
  Future<VideoPage> nextPage(VideoPage page) async => const VideoPage([]);
}

class _Shell {
  _Shell(this.location, this.navigation, this.geocoding);

  final LocationController location;
  final NavigationController navigation;
  final _FakeGeocoding geocoding;
}

Future<_Shell> _pumpShell(
  WidgetTester tester, {
  Map<String, Object> prefs = const {},
  bool mapbox = false,
  NavRoute? route,
}) async {
  tester.view.physicalSize = const Size(2220, 1080);
  tester.view.devicePixelRatio = 2.625;
  addTearDown(tester.view.reset);
  SharedPreferences.setMockInitialValues(prefs);
  final preferences = await SharedPreferences.getInstance();
  final settings = SettingsStore(preferences);
  final location = LocationController(enabled: false);
  final geocoding = _FakeGeocoding();
  final navigation = NavigationController(
    location: location,
    geocoding: geocoding,
    routing: _FakeRouting(mapbox: mapbox, single: route),
    store: NavigationStore(preferences),
  );
  final youtube = YouTubeWorker();
  final feed = FeedController(_NoVideos());
  final playback = PlaybackController(
    youtube: youtube,
    handler: CarAudioHandler(),
    settings: settings,
  );
  addTearDown(() {
    navigation.dispose();
    location.dispose();
    playback.dispose();
    feed.dispose();
    youtube.dispose();
  });
  location.injectPosition(_start, heading: 90, speed: 10, accuracy: 5);
  await tester.pumpWidget(
    MaterialApp(
      theme: CarTheme.darkTheme,
      home: HeadunitScreen(
        playback: playback,
        feed: feed,
        settings: settings,
        location: location,
        navigation: navigation,
        tilesEnabled: false,
        speech: const SilentSpeech(),
      ),
    ),
  );
  await tester.pump();
  return _Shell(location, navigation, geocoding);
}

/// Disposes the screen so no timer or ticker outlives the test.
Future<void> _unmount(WidgetTester tester) => tester.pumpWidget(const SizedBox());

Future<void> _searchAndPreview(WidgetTester tester, _Shell shell) async {
  await tester.tap(find.text('Navigasi'));
  await tester.pumpAndSettle();
  await tester.enterText(find.byType(TextField), 'Mon');
  await tester.pump(const Duration(milliseconds: 450));
  await tester.pump();
  await tester.tap(find.text('Monas'));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('search waits for three characters and the typing pause',
      (tester) async {
    final shell = await _pumpShell(tester);

    await tester.tap(find.text('Navigasi'));
    await tester.pumpAndSettle();
    expect(find.byType(TextField), findsOneWidget);
    expect(find.textContaining('tekan lama di peta'), findsOneWidget);

    await tester.enterText(find.byType(TextField), 'Mo');
    await tester.pump(const Duration(milliseconds: 500));
    expect(shell.geocoding.queries, isEmpty);
    expect(find.text('Ketik minimal 3 huruf.'), findsOneWidget);

    await tester.enterText(find.byType(TextField), 'Mon');
    await tester.pump(const Duration(milliseconds: 200));
    await tester.enterText(find.byType(TextField), 'Mona');
    await tester.pump(const Duration(milliseconds: 200));
    expect(shell.geocoding.queries, isEmpty, reason: 'still typing');
    await tester.pump(const Duration(milliseconds: 250));
    await tester.pump();
    expect(shell.geocoding.queries, ['Mona']);
    expect(find.text('Monas'), findsOneWidget);
    expect(find.text('Gambir, Jakarta Pusat'), findsOneWidget);

    await _unmount(tester);
  });

  testWidgets('a result opens the route preview with its alternative',
      (tester) async {
    final shell = await _pumpShell(tester);
    await _searchAndPreview(tester, shell);

    expect(shell.navigation.state, NavState.preview);
    expect(find.text('Monas'), findsOneWidget);
    expect(find.text('4 mnt'), findsOneWidget);
    expect(find.text('6 mnt'), findsOneWidget);
    expect(find.text('TOL'), findsOneWidget, reason: 'only the alternative uses a toll road');
    expect(find.text('Hindari tol'), findsOneWidget);
    // OSRM cannot avoid tolls: the switch is off and disabled, and says why.
    expect(find.text('Hindari tol hanya dengan Mapbox'), findsOneWidget);
    expect(tester.widget<Switch>(find.byType(Switch)).onChanged, isNull);
    expect(find.text('Mulai'), findsOneWidget);
    expect(find.text('Google Maps'), findsOneWidget);
    // No Mapbox token in tests: no provider toggle.
    expect(find.text('Mapbox'), findsNothing);
    expect(find.textContaining('Rute: OSRM'), findsWidgets);

    // The alternative and the chosen route are drawn.
    final lines = tester
        .widgetList<PolylineLayer>(find.byType(PolylineLayer))
        .expand((layer) => layer.polylines)
        .toList();
    expect(lines.where((l) => l.color == CarTheme.accent), isNotEmpty);
    expect(lines.length, greaterThanOrEqualTo(2));

    await tester.tap(find.text('6 mnt'));
    await tester.pump();
    expect(shell.navigation.selectedIndex, 1);

    // Back cancels the preview.
    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();
    expect(shell.navigation.state, NavState.idle);
    expect(find.text('Cari tujuan'), findsOneWidget);

    await _unmount(tester);
  });

  testWidgets('guidance shows the next turn in Maps, in the rail and on Home',
      (tester) async {
    final shell = await _pumpShell(tester);
    await _searchAndPreview(tester, shell);

    await tester.tap(find.text('Mulai'));
    await tester.pump();
    expect(shell.navigation.state, NavState.navigating);

    // Halfway along the first street.
    shell.location.injectPosition(
      const LatLng(-6.2, 106.805),
      heading: 90,
      speed: 10,
      accuracy: 5,
    );
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));

    expect(find.text('Belok kiri ke Jalan Thamrin'), findsWidgets);
    expect(find.text('Akhiri'), findsOneWidget);
    expect(find.text('Tiba'), findsOneWidget);
    expect(find.bySemanticsLabel('Petunjuk arah'), findsOneWidget);

    // Travelled grey behind the car, the rest in the accent.
    final lines = tester
        .widgetList<PolylineLayer>(find.byType(PolylineLayer))
        .first
        .polylines;
    expect(lines.length, 2);
    expect(lines.last.color, CarTheme.accent);
    expect(lines.first.points.last.longitude, closeTo(106.805, 1e-4));

    // North-up by default: driving east, the car sits left of the free area's centre
    // so the road ahead (east) is on screen to its right.
    MapCamera cameraNow() =>
        MapCamera.of(tester.element(find.byType(PolylineLayer).first));
    var camera = cameraNow();
    final size = camera.nonRotatedSize;
    expect(camera.rotation, 0);
    var car = camera.latLngToScreenOffset(const LatLng(-6.2, 106.805));
    const panelRight = 372.0; // 360 dp panel + 12 dp margin
    final freeCentre = panelRight + (size.width - panelRight) / 2;
    expect(car.dx, lessThan(freeCentre - 50));
    expect(car.dy, closeTo(size.height / 2, 2));

    // The compass turns heading-up on: the road east points up, the car low.
    await tester.tap(find.byTooltip('Arah jalan di atas'));
    await tester.pump();
    camera = cameraNow();
    expect(camera.rotation, closeTo(-90, 1));
    car = camera.latLngToScreenOffset(const LatLng(-6.2, 106.805));
    final ahead = camera.latLngToScreenOffset(_corner);
    expect(ahead.dy, lessThan(car.dy - 100), reason: 'the road ahead points up');
    expect((ahead.dx - car.dx).abs(), lessThan(1));
    expect(car.dy / size.height, closeTo(0.72, 0.02));
    expect(car.dx, closeTo(freeCentre, 2));

    await tester.tap(find.byTooltip('Utara di atas'));
    await tester.pump();
    expect(cameraNow().rotation, 0);

    // Home shows the turn on its map card.
    await tester.tap(find.bySemanticsLabel('Home'));
    await tester.pumpAndSettle();
    expect(find.text('Belok kiri ke Jalan Thamrin'), findsOneWidget);
    expect(find.text('Navigasi'), findsNothing);

    // The rail tile returns to Maps; Akhiri ends guidance and the tile goes away.
    await tester.tap(find.bySemanticsLabel('Petunjuk arah'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Akhiri'));
    await tester.pumpAndSettle();
    expect(shell.navigation.state, NavState.idle);
    expect(find.bySemanticsLabel('Petunjuk arah'), findsNothing);

    await _unmount(tester);
  });

  testWidgets('recent destinations show while the field is empty',
      (tester) async {
    await _pumpShell(
      tester,
      prefs: {
        'nav_recent_destinations':
            '[{"name":"Bundaran HI","address":"Menteng","lat":-6.195,"lon":106.823}]',
      },
    );

    await tester.tap(find.text('Navigasi'));
    await tester.pumpAndSettle();
    expect(find.text('TUJUAN TERAKHIR'), findsOneWidget);
    expect(find.text('Bundaran HI'), findsOneWidget);

    await _unmount(tester);
  });

  testWidgets('a long press on the map sets point B and names it',
      (tester) async {
    final shell = await _pumpShell(tester);
    await tester.tap(find.bySemanticsLabel('Maps'));
    await tester.pumpAndSettle();

    await tester.longPress(find.byType(FlutterMap));
    await tester.pumpAndSettle();
    expect(shell.navigation.state, NavState.preview);
    expect(find.text('Jalan Kebon Sirih'), findsOneWidget);
    expect(find.text('Mulai'), findsOneWidget);

    await _unmount(tester);
  });

  testWidgets('with Mapbox the preview offers the provider and colors traffic',
      (tester) async {
    final shell = await _pumpShell(tester, mapbox: true);
    await _searchAndPreview(tester, shell);

    expect(find.text('Mapbox'), findsOneWidget);
    expect(find.text('OSM'), findsOneWidget);
    expect(find.textContaining('Rute: Mapbox'), findsWidgets);
    expect(find.text('Hindari tol hanya dengan Mapbox'), findsNothing);
    expect(tester.widget<Switch>(find.byType(Switch)).onChanged, isNotNull);

    final colors = tester
        .widgetList<PolylineLayer>(find.byType(PolylineLayer))
        .expand((layer) => layer.polylines)
        .map((line) => line.color)
        .toSet();
    expect(colors, containsAll([CarTheme.accent, const Color(0xFFE5484D)]));

    await tester.tap(find.text('OSM'));
    await tester.pumpAndSettle();
    expect(shell.navigation.provider, RouteProvider.osrm);
    expect(find.text('Hindari tol hanya dengan Mapbox'), findsOneWidget);

    await _unmount(tester);
  });

  testWidgets('arriving shows the arrival card until Selesai', (tester) async {
    final shell = await _pumpShell(tester);
    await _searchAndPreview(tester, shell);
    await tester.tap(find.text('Mulai'));
    await tester.pump();

    for (final point in const [
      LatLng(-6.2, 106.805),
      _corner,
      LatLng(-6.195, 106.81),
      LatLng(-6.1905, 106.81),
      _end,
    ]) {
      shell.location.injectPosition(point, heading: 0, speed: 10, accuracy: 5);
      await tester.pump(const Duration(seconds: 1));
    }
    await tester.pump(const Duration(seconds: 1));

    expect(shell.navigation.state, NavState.arrived);
    expect(find.text('Tiba di tujuan'), findsOneWidget);
    expect(find.text('Akhiri'), findsNothing, reason: 'only the arrival card shows');
    await tester.tap(find.text('Selesai'));
    await tester.pumpAndSettle();
    expect(shell.navigation.state, NavState.idle);
    expect(find.text('Cari tujuan'), findsOneWidget);

    await _unmount(tester);
  });

  testWidgets('north-up driving south keeps the road ahead below the car',
      (tester) async {
    // Straight south for 1.1 km.
    final points = [for (var i = 0; i <= 10; i++) LatLng(-6.2 - i * 0.001, 106.8)];
    const distance = Distance();
    final cumulative = <double>[0];
    for (var i = 1; i < points.length; i++) {
      cumulative.add(
          cumulative.last + distance.as(LengthUnit.Meter, points[i - 1], points[i]));
    }
    final south = NavRoute(
      points: points,
      cumulative: cumulative,
      distance: cumulative.last,
      duration: const Duration(minutes: 2),
      steps: [
        RouteStep(
          type: ManeuverType.depart,
          instruction: 'Ke selatan',
          location: points.first,
          pointIndex: 0,
          distance: cumulative.last,
          duration: const Duration(minutes: 2),
        ),
        RouteStep(
          type: ManeuverType.arrive,
          instruction: 'Anda telah tiba di tujuan',
          location: points.last,
          pointIndex: points.length - 1,
          distance: 0,
          duration: Duration.zero,
        ),
      ],
    );
    final shell = await _pumpShell(tester, route: south);
    shell.location.injectPosition(_start, heading: 180, speed: 10, accuracy: 5);
    await shell.navigation.previewTo(
      Place(name: 'Selatan', location: points.last),
    );
    await tester.tap(find.bySemanticsLabel('Maps'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Mulai'));
    await tester.pump();
    shell.location.injectPosition(
      const LatLng(-6.202, 106.8),
      heading: 180,
      speed: 10,
      accuracy: 5,
    );
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));

    final camera = MapCamera.of(tester.element(find.byType(PolylineLayer).first));
    final car = camera.latLngToScreenOffset(const LatLng(-6.202, 106.8));
    final ahead = camera.latLngToScreenOffset(const LatLng(-6.204, 106.8));
    expect(camera.rotation, 0);
    expect(car.dy, lessThan(camera.nonRotatedSize.height * 0.3));
    expect(ahead.dy, greaterThan(car.dy));
    expect(ahead.dy, lessThan(camera.nonRotatedSize.height));

    await _unmount(tester);
  });

  testWidgets('search stays usable with the keyboard up', (tester) async {
    await _pumpShell(tester);
    await tester.tap(find.text('Navigasi'));
    await tester.pumpAndSettle();
    // A landscape keyboard covering ~240 dp of the 411 dp screen.
    tester.view.viewInsets = const FakeViewPadding(bottom: 240 * 2.625);
    await tester.pump();

    await tester.enterText(find.byType(TextField), 'Mon');
    await tester.pump(const Duration(milliseconds: 450));
    await tester.pump();

    const keyboardTop = 1080 / 2.625 - 240;
    for (final name in ['Monas', 'Monumen Pancasila']) {
      expect(tester.getRect(find.text(name)).bottom, lessThan(keyboardTop));
    }

    await _unmount(tester);
  });

  testWidgets('the phone status bar is hidden again after the keyboard or a pause',
      (tester) async {
    final modes = <Object?>[];
    tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
      SystemChannels.platform,
      (call) async {
        // Manual mode with overlays goes out as setEnabledSystemUIOverlays.
        if (call.method.startsWith('SystemChrome.setEnabledSystemUI')) {
          modes.add(call.arguments);
        }
        return null;
      },
    );
    addTearDown(() => tester.binding.defaultBinaryMessenger
        .setMockMethodCallHandler(SystemChannels.platform, null));
    await _pumpShell(tester);
    modes.clear();

    // The keyboard closes: hidden at once.
    tester.view.viewInsets = const FakeViewPadding(bottom: 600);
    await tester.pump();
    tester.view.viewInsets = FakeViewPadding.zero;
    await tester.pump();
    expect(modes, hasLength(1));

    // The status bar comes back on its own: hidden after two seconds.
    tester.view.padding = const FakeViewPadding(top: 63);
    await tester.pump();
    expect(modes, hasLength(1));
    await tester.pump(const Duration(seconds: 2));
    expect(modes, hasLength(2));

    await _unmount(tester);
  });

  testWidgets('every guidance starts north-up and the controls stay put',
      (tester) async {
    final shell = await _pumpShell(tester, mapbox: true);
    // Like the device run: YouTube first, then Maps from the rail.
    await tester.pump(const Duration(seconds: 4));
    await tester.tap(find.bySemanticsLabel('YouTube'));
    await tester.pumpAndSettle();
    await tester.tap(find.bySemanticsLabel('Maps'));
    await tester.pumpAndSettle();

    Future<void> previewAndStart() async {
      await tester.tap(find.bySemanticsLabel('Cari tujuan'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField), 'Mon');
      await tester.pump(const Duration(milliseconds: 450));
      await tester.pump();
      await tester.tap(find.text('Monas'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('OSM'));
      await tester.pumpAndSettle();
    }

    MapCamera camera() =>
        MapCamera.of(tester.element(find.byType(PolylineLayer).first));

    Future<void> drive() async {
      for (var i = 1; i <= 3; i++) {
        shell.location.injectPosition(LatLng(-6.2, 106.8 + i * 0.001),
            heading: 90, speed: 10, accuracy: 5);
        await tester.pump(const Duration(seconds: 1));
      }
    }

    await previewAndStart();
    final recenter = tester.getRect(find.byTooltip('Ikuti mobil'));
    await tester.tap(find.text('Mulai'));
    await tester.pump();
    await drive();
    expect(camera().rotation, 0);
    expect(find.byTooltip('Arah jalan di atas'), findsOneWidget);
    expect(tester.getRect(find.byTooltip('Ikuti mobil')), recenter);

    // Heading-up in this guidance does not carry over to the next one.
    await tester.tap(find.byTooltip('Arah jalan di atas'));
    await tester.pump();
    expect(camera().rotation, closeTo(-90, 1));
    await tester.tap(find.text('Akhiri'));
    await tester.pumpAndSettle();
    shell.location.injectPosition(_start, heading: 90, speed: 10, accuracy: 5);
    await tester.pump(const Duration(seconds: 1));
    await previewAndStart();
    await tester.tap(find.text('Mulai'));
    await tester.pump();
    await drive();
    expect(camera().rotation, 0);
    expect(find.byTooltip('Arah jalan di atas'), findsOneWidget);

    await _unmount(tester);
  });
}

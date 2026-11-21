import 'dart:async';
import 'dart:convert';
import 'dart:isolate';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:latlong2/latlong.dart';

import '../models/navigation.dart';
import 'geo.dart';
import 'geocoding_service.dart' show kNavUserAgent;
import 'polyline.dart';
import 'route_instructions.dart';

/// Mapbox access token, passed at build time with `--dart-define(-from-file)`; empty in
/// builds without one, which then route with OSRM only. It travels in request URLs, so
/// URLs and HTTP exceptions (whose text contains the URL) are never logged.
const String kMapboxToken = String.fromEnvironment('MAPBOX_TOKEN');

/// OSRM's demo server: non-commercial use, at most one request per second.
const String kOsrmDemo = 'https://router.project-osrm.org/route/v1/driving';

/// FOSSGIS's OSRM car instance, the fallback: one request per second, no heavy use.
const String kOsrmFossgis =
    'https://routing.openstreetmap.de/routed-car/route/v1/driving';

const String kMapboxDirections =
    'https://api.mapbox.com/directions/v5/mapbox/driving-traffic';

/// Status and body of one HTTP GET.
class HttpReply {
  const HttpReply(this.statusCode, this.body);

  final int statusCode;
  final String body;
}

typedef HttpGet = Future<HttpReply> Function(Uri uri);

/// Routes from Mapbox (live traffic, with a token) or OSRM (keyless), with fallbacks.
///
/// Mapbox results are kept in memory only: its terms forbid caching or storing
/// Directions results, so nothing here writes them anywhere.
class RoutingService {
  RoutingService({
    String mapboxToken = kMapboxToken,
    HttpGet? get,
    this.osrmServers = const [kOsrmDemo, kOsrmFossgis],
    this.mapboxBase = kMapboxDirections,
    this.osrmInterval = const Duration(milliseconds: 1100),
    this.mapboxInterval = const Duration(milliseconds: 500),
    DateTime Function()? now,
  })  : _mapboxToken = mapboxToken,
        _get = get ?? isolateGet,
        _now = now ?? DateTime.now;

  final String _mapboxToken;
  final HttpGet _get;
  final List<String> osrmServers;
  final String mapboxBase;

  /// Spacing between request starts per host; OSRM's servers allow one per second.
  final Duration osrmInterval;
  final Duration mapboxInterval;
  final DateTime Function() _now;

  final Map<String, DateTime> _nextSlot = {};

  bool get hasMapbox => _mapboxToken.isNotEmpty;

  /// Routes from [from] to [to], best first, up to two alternatives when [alternatives].
  ///
  /// [heading] (degrees) makes the route start in the car's direction of travel, which
  /// matters for reroutes. Mapbox is used when asked and available; when it fails
  /// (network, 401/429, quota) OSRM answers and [RouteResult.fellBack] is set. Throws
  /// [NavException] when no server can route.
  Future<RouteResult> route(
    LatLng from,
    LatLng to, {
    RouteProvider provider = RouteProvider.osrm,
    bool avoidTolls = false,
    bool alternatives = true,
    double? heading,
  }) async {
    if (provider == RouteProvider.mapbox && hasMapbox) {
      try {
        return await _request(mapboxBase, RouteProvider.mapbox, from, to,
            avoidTolls: avoidTolls, alternatives: alternatives, heading: heading);
      } on _Unavailable catch (e) {
        debugPrint('Mapbox unavailable (${e.reason}); routing with OSRM');
        final osrm = await _osrm(from, to, avoidTolls, alternatives, heading);
        return RouteResult(
          routes: osrm.routes,
          provider: RouteProvider.osrm,
          tollsAvoided: osrm.tollsAvoided,
          fellBack: true,
        );
      }
    }
    return _osrm(from, to, avoidTolls, alternatives, heading);
  }

  Future<RouteResult> _osrm(LatLng from, LatLng to, bool avoidTolls,
      bool alternatives, double? heading) async {
    for (final base in osrmServers) {
      try {
        return await _request(base, RouteProvider.osrm, from, to,
            avoidTolls: avoidTolls, alternatives: alternatives, heading: heading);
      } on _Unavailable catch (e) {
        debugPrint('OSRM ${Uri.parse(base).host} unavailable (${e.reason})');
      }
    }
    throw const NavException(
        'Server rute tidak dapat dihubungi. Periksa koneksi internet.');
  }

  Future<RouteResult> _request(
    String base,
    RouteProvider provider,
    LatLng from,
    LatLng to, {
    required bool avoidTolls,
    required bool alternatives,
    double? heading,
  }) async {
    // Avoiding tolls is a Mapbox feature: OSRM's demo and FOSSGIS servers answer
    // exclude=toll with 400 InvalidValue, so OSRM is never asked.
    var exclude = avoidTolls && provider == RouteProvider.mapbox;
    while (true) {
      final uri = _uri(base, provider, from, to,
          exclude: exclude, alternatives: alternatives, heading: heading);
      final reply = await _send(uri);
      final status = reply.statusCode;
      if (status == 401 || status == 403 || status == 429 || status >= 500) {
        throw _Unavailable('HTTP $status');
      }
      if (status != 200) {
        // Error bodies are a few bytes; decoding them here is free.
        final code = _errorCode(reply.body);
        if (exclude && _rejectsExclude(code)) {
          // The server cannot avoid tolls here: route anyway, reported as not avoided.
          exclude = false;
          continue;
        }
        if (code == 'NoRoute' || code == 'NoSegment') throw const NavException(_noRoute);
        if (status == 400 || status == 422) {
          throw const NavException('Rute ke tujuan ini tidak dapat dihitung.');
        }
        throw _Unavailable('HTTP $status');
      }

      final body = reply.body;
      final ParsedRoutes parsed;
      try {
        // 200-500 KB for a long route with alternatives: never decoded on the UI isolate,
        // where a decode that size froze the A6+'s screen for 170-850 ms.
        parsed = await _parseInBackground(body, provider);
      } catch (e) {
        throw _Unavailable('unreadable response (${e.runtimeType})');
      }
      if (parsed.code == 'NoRoute' || parsed.code == 'NoSegment') {
        throw const NavException(_noRoute);
      }
      if (parsed.code != 'Ok' || parsed.routes.isEmpty) {
        throw _Unavailable('code ${parsed.code}');
      }

      var routes = parsed.routes;
      if (exclude) {
        // Alternatives may still use tolls where none avoids them; prefer those that do.
        final tollFree = [for (final r in routes) if (!r.hasTolls) r];
        if (tollFree.isNotEmpty) routes = tollFree;
      }
      return RouteResult(
        routes: routes.take(3).toList(growable: false),
        provider: provider,
        tollsAvoided: exclude && !routes.first.hasTolls,
      );
    }
  }

  Uri _uri(String base, RouteProvider provider, LatLng from, LatLng to,
      {required bool exclude, required bool alternatives, double? heading}) {
    String c(LatLng p) =>
        '${p.longitude.toStringAsFixed(6)},${p.latitude.toStringAsFixed(6)}';
    final params = <String, String>{
      'overview': 'full',
      'geometries': 'polyline6',
      'steps': 'true',
      if (provider == RouteProvider.osrm)
        // `true` returns at most one alternative; the preview offers up to two.
        'alternatives': alternatives ? '2' : 'false'
      else
        'alternatives': '$alternatives',
      if (exclude) 'exclude': 'toll',
      // Start within 45 degrees of the car's heading; any approach at the destination.
      if (heading != null) 'bearings': '${heading.round() % 360},45;',
      if (provider == RouteProvider.mapbox) ...{
        'annotations': 'congestion,duration,distance',
        'language': 'id',
        'voice_instructions': 'true',
        'banner_instructions': 'true',
        'voice_units': 'metric',
        'access_token': _mapboxToken,
      },
    };
    return Uri.parse('$base/${c(from)};${c(to)}').replace(queryParameters: params);
  }

  /// Waits for the host's next free slot, then sends. Slots are reserved in call order,
  /// so concurrent requests queue instead of bursting.
  Future<HttpReply> _send(Uri uri) async {
    final interval =
        uri.host == Uri.parse(mapboxBase).host ? mapboxInterval : osrmInterval;
    final now = _now();
    final slot = _nextSlot[uri.host];
    final start = slot == null || slot.isBefore(now) ? now : slot;
    _nextSlot[uri.host] = start.add(interval);
    final wait = start.difference(now);
    if (wait > Duration.zero) await Future<void>.delayed(wait);
    try {
      return await _get(uri);
    } catch (e) {
      // Only the type: the exception text carries the URL, which may hold the token.
      throw _Unavailable('${e.runtimeType}');
    }
  }

  /// The production GET: runs in a short-lived isolate, so the TLS handshake, which Dart
  /// performs synchronously, stays off the UI thread.
  static Future<HttpReply> isolateGet(Uri uri) => Isolate.run(() => _plainGet(uri));

  static Future<HttpReply> _plainGet(Uri uri) async {
    final client = http.Client();
    try {
      final response = await client
          .get(uri, headers: const {'User-Agent': kNavUserAgent})
          .timeout(const Duration(seconds: 12));
      return HttpReply(response.statusCode, utf8.decode(response.bodyBytes));
    } finally {
      client.close();
    }
  }

  /// Static, so the closure captures only its arguments and never this service.
  static Future<ParsedRoutes> _parseInBackground(String body, RouteProvider provider) =>
      Isolate.run(() => parseRouteResponse(body, provider));

  static String? _errorCode(String body) {
    try {
      final json = jsonDecode(body);
      return json is Map<String, Object?> ? json['code'] as String? : null;
    } catch (_) {
      return null;
    }
  }

  static bool _rejectsExclude(String? code) =>
      code == 'InvalidValue' || code == 'InvalidQuery' || code == 'InvalidInput';
}

const _noRoute = 'Rute tidak ditemukan ke tujuan ini.';

class _Unavailable implements Exception {
  const _Unavailable(this.reason);

  final String reason;
}

/// The `code` of a Directions response and its routes (empty unless the code is `Ok`).
class ParsedRoutes {
  const ParsedRoutes(this.code, this.routes);

  final String code;
  final List<NavRoute> routes;
}

/// Parses an OSRM or Mapbox Directions response requested with `overview=full`,
/// `geometries=polyline6` and `steps=true`. Pure, so it runs inside [Isolate.run].
ParsedRoutes parseRouteResponse(String body, RouteProvider provider) {
  final json = jsonDecode(body) as Map<String, Object?>;
  final code = json['code'] as String? ?? '';
  if (code != 'Ok') return ParsedRoutes(code, const []);
  final uuid = json['uuid'] as String?;
  final list = json['routes'] as List<Object?>? ?? const [];
  final routes = <NavRoute>[];
  for (var i = 0; i < list.length; i++) {
    final route = _parseRoute(list[i] as Map<String, Object?>, provider, uuid, i);
    if (route != null) routes.add(route);
  }
  return ParsedRoutes(code, routes);
}

NavRoute? _parseRoute(
    Map<String, Object?> r, RouteProvider provider, String? uuid, int index) {
  final geometry = r['geometry'];
  if (geometry is! String) return null;
  final points = decodePolyline(geometry);
  if (points.length < 2) return null;
  final last = points.length - 1;

  // Mapbox writes maneuver text in the requested language when it supports it; the
  // route's voiceLocale says which language it used.
  final serverText = provider == RouteProvider.mapbox &&
      (r['voiceLocale'] as String? ?? '').startsWith('id');

  final steps = <RouteStep>[];
  final summaries = <String>[];
  final congestion = <Congestion>[];
  final durations = <double>[];
  var hasAnnotations = provider == RouteProvider.mapbox;
  var hasTolls = false;
  var expected = 0;

  for (final legObject in r['legs'] as List<Object?>? ?? const []) {
    final leg = legObject as Map<String, Object?>;
    final summary = leg['summary'] as String? ?? '';
    if (summary.isNotEmpty) summaries.add(summary);

    for (final stepObject in leg['steps'] as List<Object?>? ?? const []) {
      final s = stepObject as Map<String, Object?>;
      final m = s['maneuver'] as Map<String, Object?>;
      final loc = m['location'] as List<Object?>;
      final location =
          LatLng((loc[1] as num).toDouble(), (loc[0] as num).toDouble());
      final intersections = s['intersections'] as List<Object?>? ?? const [];
      final first = intersections.isEmpty
          ? null
          : intersections.first as Map<String, Object?>;
      for (final x in intersections) {
        final classes = (x as Map<String, Object?>)['classes'] as List<Object?>?;
        if (classes != null && classes.contains('toll')) hasTolls = true;
      }

      // Mapbox gives the maneuver's index in the full geometry; for OSRM it follows from
      // the step geometries, which chain into the full one sharing their joint points.
      final geometryIndex = first?['geometry_index'];
      final pointIndex = (geometryIndex is int
              ? geometryIndex
              : _locate(points, location, expected))
          .clamp(0, last);
      final stepGeometry = s['geometry'];
      final stepPoints =
          stepGeometry is String ? decodePolyline(stepGeometry).length : 0;
      expected = pointIndex + math.max(stepPoints - 1, 0);

      final rawType = m['type'] as String? ?? 'turn';
      final rawModifier = m['modifier'] as String?;
      final exit = m['exit'] is num ? (m['exit'] as num).toInt() : null;
      final name = s['name'] as String? ?? '';
      final ref = s['ref'] as String?;
      // OSRM's public servers send no road classes; Indonesian toll roads are named
      // "Jalan Tol ...", which is the only toll hint their routes carry.
      if (_tollName.hasMatch(name)) hasTolls = true;
      final serverInstruction = m['instruction'];
      final firstClasses = first?['classes'] as List<Object?>?;
      final instruction = serverText &&
              serverInstruction is String &&
              serverInstruction.isNotEmpty
          ? serverInstruction
          : compileInstruction(StepText(
              type: rawType,
              modifier: rawModifier,
              name: name,
              ref: ref,
              destinations: s['destinations'] as String?,
              exits: s['exits'] as String?,
              exit: exit,
              rotaryName: s['rotary_name'] as String?,
              bearingAfter: m['bearing_after'] as num?,
              drivingSide: s['driving_side'] as String? ?? 'right',
              mode: s['mode'] as String? ?? 'driving',
              motorway: firstClasses?.contains('motorway') ?? false,
            ));

      steps.add(RouteStep(
        type: maneuverTypeOf(rawType),
        modifier: maneuverModifierOf(rawModifier),
        exit: exit,
        name: name,
        ref: ref,
        instruction: instruction,
        location: location,
        pointIndex: pointIndex,
        distance: (s['distance'] as num? ?? 0).toDouble(),
        duration: _seconds(s['duration'] as num? ?? 0),
      ));
    }

    if (hasAnnotations) {
      final annotation = leg['annotation'] as Map<String, Object?>?;
      final c = annotation?['congestion'] as List<Object?>?;
      final d = annotation?['duration'] as List<Object?>?;
      if (c == null || d == null || c.length != d.length) {
        hasAnnotations = false;
      } else {
        congestion.addAll(c.map(_congestionOf));
        durations.addAll(d.map((v) => (v as num).toDouble()));
      }
    }
  }
  if (steps.isEmpty) return null;

  // Annotations cover each pair of consecutive points; anything else is unusable.
  final aligned = hasAnnotations && congestion.length == last;
  return NavRoute(
    points: points,
    cumulative: cumulativeDistances(points),
    distance: (r['distance'] as num? ?? 0).toDouble(),
    duration: _seconds(r['duration'] as num? ?? 0),
    steps: steps,
    summary: summaries.join(', '),
    provider: provider,
    congestion: aligned ? congestion : null,
    segmentDurations: aligned ? durations : null,
    hasTolls: hasTolls,
    uuid: uuid,
    routeIndex: index,
  );
}

final _tollName = RegExp(r'(^|\s)tol(\s|$)', caseSensitive: false);

Duration _seconds(num s) => Duration(milliseconds: (s * 1000).round());

Congestion _congestionOf(Object? value) => switch (value) {
      'low' => Congestion.low,
      'moderate' => Congestion.moderate,
      'heavy' => Congestion.heavy,
      'severe' => Congestion.severe,
      _ => Congestion.unknown,
    };

/// Index of the route point at [location], searched near [expected].
int _locate(List<LatLng> points, LatLng location, int expected) {
  final start = expected.clamp(0, points.length - 1);
  if (distanceMeters(points[start], location) < 3) return start;
  var best = start;
  var bestDistance = double.infinity;
  final from = math.max(0, start - 5);
  final to = math.min(points.length, start + 400);
  for (var i = from; i < to; i++) {
    final d = distanceMeters(points[i], location);
    if (d < bestDistance) {
      bestDistance = d;
      best = i;
    }
  }
  return best;
}

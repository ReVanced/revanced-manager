import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:latlong2/latlong.dart';

import '../models/navigation.dart';
import 'geo.dart';

/// Same app id the map tiles send; identifies the app without personal data.
const String kNavUserAgent = 'com.carheadunit.youtube_car_headunit/1.0';

/// Place search and reverse geocoding through Photon (photon.komoot.io).
///
/// Photon allows search-as-you-type under fair use, so results are cached and requests
/// are spaced by [minInterval]. Nominatim is never used for search: its policy forbids
/// autocomplete. Responses are a few KB, so they are decoded on the calling isolate over
/// one keep-alive client.
class GeocodingService {
  GeocodingService({
    http.Client? client,
    this.baseUrl = 'https://photon.komoot.io',
    this.minInterval = const Duration(milliseconds: 600),
    this.timeout = const Duration(seconds: 8),
    DateTime Function()? now,
  })  : _client = client ?? http.Client(),
        _now = now ?? DateTime.now;

  final http.Client _client;
  final String baseUrl;
  final Duration minInterval;
  final Duration timeout;
  final DateTime Function() _now;

  static const _cacheSize = 64;
  static const _cacheTtl = Duration(minutes: 10);
  final _cache = <String, _Cached>{}; // insertion-ordered: LRU

  DateTime? _lastRequest;
  int _searchSeq = 0;

  /// Queries shorter than this return nothing without a request.
  static const minQueryLength = 3;

  /// Places matching [query], ranked by Photon with a bias towards [near].
  ///
  /// A search superseded by a newer one before its request went out returns an empty
  /// list without querying. Throws [NavException] when Photon cannot be reached.
  Future<List<Place>> search(String query, {LatLng? near, int limit = 8}) async {
    final q = query.trim();
    if (q.length < minQueryLength) return const [];
    final key = 's|${q.toLowerCase()}|${_round(near, 2)}|$limit';
    final cached = _fromCache(key);
    if (cached != null) return cached;

    final seq = ++_searchSeq;
    await _waitForSlot();
    if (seq != _searchSeq) return const [];

    final uri = Uri.parse('$baseUrl/api/').replace(queryParameters: {
      'q': q,
      'limit': '$limit',
      if (near != null) 'lat': near.latitude.toStringAsFixed(5),
      if (near != null) 'lon': near.longitude.toStringAsFixed(5),
    });
    final places = rankByDistance(parsePhotonPlaces(await _get(uri)), near);
    _store(key, places);
    return places;
  }

  /// The place at [point], or null when Photon knows nothing there.
  Future<Place?> reverse(LatLng point) async {
    final key = 'r|${_round(point, 4)}';
    final cached = _fromCache(key);
    if (cached != null) return cached.isEmpty ? null : cached.first;

    await _waitForSlot();
    final uri = Uri.parse('$baseUrl/reverse').replace(queryParameters: {
      'lat': point.latitude.toStringAsFixed(6),
      'lon': point.longitude.toStringAsFixed(6),
      'limit': '1',
    });
    final places = parsePhotonPlaces(await _get(uri));
    _store(key, places);
    if (places.isEmpty) return null;
    // The driver chose this point; keep its position, take only Photon's name.
    return Place(name: places.first.name, address: places.first.address, location: point);
  }

  void close() => _client.close();

  Future<void> _waitForSlot() async {
    final last = _lastRequest;
    final now = _now();
    if (last != null) {
      final wait = minInterval - now.difference(last);
      if (wait > Duration.zero) {
        _lastRequest = last.add(minInterval);
        await Future<void>.delayed(wait);
        return;
      }
    }
    _lastRequest = now;
  }

  Future<String> _get(Uri uri) async {
    final http.Response response;
    try {
      response = await _client
          .get(uri, headers: const {'User-Agent': kNavUserAgent})
          .timeout(timeout);
    } catch (e) {
      debugPrint('Photon request failed: $e');
      throw const NavException('Pencarian gagal. Periksa koneksi internet.');
    }
    if (response.statusCode != 200) {
      debugPrint('Photon HTTP ${response.statusCode}');
      throw const NavException('Pencarian sedang tidak tersedia. Coba lagi nanti.');
    }
    return utf8.decode(response.bodyBytes);
  }

  List<Place>? _fromCache(String key) {
    final hit = _cache.remove(key);
    if (hit == null) return null;
    if (_now().difference(hit.time) > _cacheTtl) return null;
    _cache[key] = hit; // most recently used last
    return hit.places;
  }

  void _store(String key, List<Place> places) {
    _cache[key] = _Cached(places, _now());
    while (_cache.length > _cacheSize) {
      _cache.remove(_cache.keys.first);
    }
  }

  static String _round(LatLng? p, int digits) => p == null
      ? '-'
      : '${p.latitude.toStringAsFixed(digits)},${p.longitude.toStringAsFixed(digits)}';
}

class _Cached {
  _Cached(this.places, this.time);

  final List<Place> places;
  final DateTime time;
}

/// Places within [nearbyMeters] of [near] first, nearest first, then the rest in their
/// original order. Photon ranks an exact name match far away above a close partial one
/// ("alun" put a place 133 km away above one 1.1 km away), which is wrong for a driver.
List<Place> rankByDistance(List<Place> places, LatLng? near,
    {double nearbyMeters = 30000}) {
  if (near == null) return places;
  final nearby = <(double, int, Place)>[];
  final rest = <Place>[];
  for (var i = 0; i < places.length; i++) {
    final d = distanceMeters(near, places[i].location);
    if (d <= nearbyMeters) {
      nearby.add((d, i, places[i]));
    } else {
      rest.add(places[i]);
    }
  }
  // The index breaks ties, so equal distances keep Photon's order.
  nearby.sort((a, b) => a.$1 != b.$1 ? a.$1.compareTo(b.$1) : a.$2.compareTo(b.$2));
  return [for (final n in nearby) n.$3, ...rest];
}

/// Places from a Photon GeoJSON response (`/api` or `/reverse`); unusable features are
/// skipped.
List<Place> parsePhotonPlaces(String body) {
  final Object? json;
  try {
    json = jsonDecode(body);
  } catch (_) {
    throw const NavException('Jawaban pencarian tidak terbaca.');
  }
  final features = json is Map<String, Object?> ? json['features'] : null;
  if (features is! List) return const [];
  final places = <Place>[];
  final seen = <String>{};
  for (final f in features) {
    final place = _placeOf(f);
    if (place == null) continue;
    // Photon returns a street once per OSM way; one row per name and area is enough.
    if (seen.add('${place.name}|${place.address}')) places.add(place);
  }
  return places;
}

Place? _placeOf(Object? feature) {
  if (feature is! Map<String, Object?>) return null;
  final geometry = feature['geometry'];
  final props = feature['properties'];
  if (geometry is! Map<String, Object?> || props is! Map<String, Object?>) return null;
  final coords = geometry['coordinates'];
  if (coords is! List || coords.length < 2) return null;
  final lon = coords[0];
  final lat = coords[1];
  if (lon is! num || lat is! num) return null;

  String? s(String key) {
    final v = props[key];
    return v is String && v.trim().isNotEmpty ? v.trim() : null;
  }

  final street = s('street');
  final number = s('housenumber');
  final streetLine =
      street == null ? null : (number == null ? street : '$street $number');
  final name = s('name') ?? streetLine ?? s('district') ?? s('city');
  if (name == null) return null;

  final parts = <String>[];
  for (final part in [
    if (streetLine != name) streetLine,
    s('district') ?? s('locality'),
    s('city') ?? s('county'),
    s('state'),
  ]) {
    if (part != null && part != name && !parts.contains(part)) parts.add(part);
  }
  return Place(
    name: name,
    address: parts.isEmpty ? null : parts.join(', '),
    location: LatLng(lat.toDouble(), lon.toDouble()),
  );
}

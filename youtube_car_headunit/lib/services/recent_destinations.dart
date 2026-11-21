import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:latlong2/latlong.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/navigation.dart';

/// Navigation choices kept across restarts: recent destinations, the routing provider
/// and "avoid tolls". Keys are separate from [SettingsStore]'s.
///
/// Only places the driver chose are stored, never routes: Mapbox's terms forbid storing
/// Directions results, and Photon places carry no such restriction.
class NavigationStore {
  NavigationStore(this._prefs, {this.maxRecents = 8});

  static Future<NavigationStore> load() async =>
      NavigationStore(await SharedPreferences.getInstance());

  final SharedPreferences _prefs;
  final int maxRecents;

  static const _kRecents = 'nav_recent_destinations';
  static const _kProvider = 'nav_route_provider';
  static const _kAvoidTolls = 'nav_avoid_tolls';

  /// Two entries closer than this are the same place.
  static const _samePlaceMeters = 50.0;
  static const _distance = Distance();

  /// Newest first.
  List<Place> get recents {
    final raw = _prefs.getString(_kRecents);
    if (raw == null) return const [];
    try {
      final list = jsonDecode(raw) as List<Object?>;
      return [
        for (final item in list) Place.fromJson(item as Map<String, Object?>),
      ];
    } catch (e) {
      // A corrupt list only costs the recents; never block the search panel on it.
      debugPrint('Ignoring unreadable recent destinations: $e');
      return const [];
    }
  }

  /// Puts [place] first; an earlier entry for the same place is dropped.
  Future<void> addRecent(Place place) {
    final list = [
      place,
      for (final p in recents)
        if (!_same(p, place)) p,
    ];
    return _saveRecents(list.take(maxRecents).toList());
  }

  Future<void> removeRecent(Place place) =>
      _saveRecents([for (final p in recents) if (p != place) p]);

  Future<void> _saveRecents(List<Place> list) => _prefs.setString(
      _kRecents, jsonEncode([for (final p in list) p.toJson()]));

  bool _same(Place a, Place b) =>
      _distance.as(LengthUnit.Meter, a.location, b.location) < _samePlaceMeters;

  /// Null until the driver picks one.
  RouteProvider? get provider {
    final name = _prefs.getString(_kProvider);
    for (final p in RouteProvider.values) {
      if (p.name == name) return p;
    }
    return null;
  }

  Future<void> setProvider(RouteProvider provider) =>
      _prefs.setString(_kProvider, provider.name);

  bool get avoidTolls => _prefs.getBool(_kAvoidTolls) ?? false;

  Future<void> setAvoidTolls(bool value) => _prefs.setBool(_kAvoidTolls, value);
}

import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../../controllers/navigation_controller.dart';
import '../../models/navigation.dart';
import '../../theme/car_theme.dart';
import '../../utils/format.dart';
import 'nav_format.dart';

/// The route before "Mulai": destination, the route and its alternatives with time,
/// distance and arrival, avoid tolls, the provider toggle and the Google Maps hand-off.
class RoutePreview extends StatelessWidget {
  const RoutePreview({
    super.key,
    required this.navigation,
    required this.onGoogleMaps,
  });

  final NavigationController navigation;

  /// Hands the destination to Google Maps instead of guiding in the app.
  final ValueChanged<Place> onGoogleMaps;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: navigation,
      builder: (context, _) {
        final place = navigation.destination;
        final routes = navigation.routes;
        final address = place?.address;
        return SurfaceCard(
          padding: const EdgeInsets.all(12),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  const SizedBox(width: 4),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          place?.name ?? '',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: CarTheme.heading,
                        ),
                        if (address != null && address.isNotEmpty)
                          Text(
                            address,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: CarTheme.caption,
                          ),
                      ],
                    ),
                  ),
                  ControlButton(
                    icon: LucideIcons.x,
                    tooltip: 'Batal',
                    onPressed: navigation.stop,
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Flexible(child: _routes(routes)),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: _TollSwitch(
                      value: navigation.avoidTolls,
                      // OSRM's public servers refuse exclude=toll, so only Mapbox
                      // can avoid tolls; the stored choice returns with Mapbox.
                      enabled: navigation.hasMapbox &&
                          navigation.provider == RouteProvider.mapbox,
                      onChanged: navigation.setAvoidTolls,
                    ),
                  ),
                  if (navigation.hasMapbox) ...[
                    const SizedBox(width: 8),
                    _ProviderToggle(
                      value: navigation.provider,
                      onChanged: navigation.setProvider,
                    ),
                  ],
                ],
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: FilledButton.icon(
                      onPressed: navigation.route != null && !navigation.loading
                          ? navigation.start
                          : null,
                      style: FilledButton.styleFrom(
                        minimumSize: const Size.fromHeight(52),
                      ),
                      icon: const Icon(LucideIcons.navigation, size: 20),
                      label: const Text('Mulai'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  FilledButton(
                    onPressed: place == null ? null : () => onGoogleMaps(place),
                    style: FilledButton.styleFrom(
                      backgroundColor: CarTheme.surfaceHigh,
                      foregroundColor: CarTheme.text,
                      minimumSize: const Size(0, 52),
                      side: const BorderSide(color: CarTheme.border),
                    ),
                    child: const Text('Google Maps'),
                  ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _routes(List<NavRoute> routes) {
    if (routes.isEmpty) {
      if (navigation.error case final error? when !navigation.loading) {
        return Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(error, style: CarTheme.caption),
              const SizedBox(height: 10),
              FilledButton(
                onPressed: navigation.retry,
                child: const Text('Coba lagi'),
              ),
            ],
          ),
        );
      }
      return const SizedBox(
        height: 112,
        child: Center(
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              SizedBox.square(
                dimension: 22,
                child: CircularProgressIndicator(strokeWidth: 2.5),
              ),
              SizedBox(width: 12),
              Text('Mencari rute...', style: CarTheme.caption),
            ],
          ),
        ),
      );
    }
    final now = DateTime.now();
    return Stack(
      children: [
        ListView.separated(
          shrinkWrap: true,
          padding: EdgeInsets.zero,
          itemCount: routes.length,
          separatorBuilder: (_, __) => const SizedBox(height: 4),
          itemBuilder: (context, i) => _RouteRow(
            route: routes[i],
            arrival: now.add(routes[i].duration),
            selected: i == navigation.selectedIndex,
            onTap: () => navigation.selectRoute(i),
          ),
        ),
        // Routes are being fetched again (tolls or provider changed).
        if (navigation.loading)
          const Positioned(
            left: 8,
            right: 8,
            top: 0,
            child: LinearProgressIndicator(minHeight: 2),
          ),
      ],
    );
  }
}

class _RouteRow extends StatelessWidget {
  const _RouteRow({
    required this.route,
    required this.arrival,
    required this.selected,
    required this.onTap,
  });

  final NavRoute route;
  final DateTime arrival;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final shape = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(CarTheme.radius),
      side: BorderSide(color: selected ? CarTheme.accent : CarTheme.border),
    );
    final via = route.summary.isEmpty ? '' : ' · via ${route.summary}';
    return Material(
      color: selected ? CarTheme.accentSoft : Colors.transparent,
      shape: shape,
      child: InkWell(
        onTap: onTap,
        customBorder: shape,
        child: ConstrainedBox(
          constraints: const BoxConstraints(minHeight: 54),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Text(
                            formatTravelTime(route.duration),
                            style: CarTheme.heading.copyWith(
                              color: selected ? CarTheme.accent : CarTheme.text,
                            ),
                          ),
                          // OSRM cannot avoid tolls, so the driver picks by this.
                          if (route.hasTolls) ...[
                            const SizedBox(width: 8),
                            const Tag('TOL'),
                          ],
                        ],
                      ),
                      Text(
                        '${formatDistance(route.distance)}$via',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: CarTheme.caption,
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                Text('Tiba ${formatClock(arrival)}', style: CarTheme.time),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _TollSwitch extends StatelessWidget {
  const _TollSwitch({
    required this.value,
    required this.enabled,
    required this.onChanged,
  });

  final bool value;
  final bool enabled;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: enabled ? () => onChanged(!value) : null,
      borderRadius: BorderRadius.circular(CarTheme.radius),
      child: ConstrainedBox(
        constraints: const BoxConstraints(minHeight: 48),
        child: Row(
          children: [
            const SizedBox(width: 4),
            Expanded(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Hindari tol',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: CarTheme.body.copyWith(
                      color: enabled ? CarTheme.text : CarTheme.textMuted,
                    ),
                  ),
                  if (!enabled)
                    Text(
                      'Hindari tol hanya dengan Mapbox',
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: CarTheme.caption.copyWith(fontSize: 12),
                    ),
                ],
              ),
            ),
            // Off while disabled: under OSRM the route may use tolls.
            Switch(value: enabled && value, onChanged: enabled ? onChanged : null),
          ],
        ),
      ),
    );
  }
}

/// Mapbox (live traffic) or OSM routing; shown only when the build has a Mapbox token.
class _ProviderToggle extends StatelessWidget {
  const _ProviderToggle({required this.value, required this.onChanged});

  final RouteProvider value;
  final ValueChanged<RouteProvider> onChanged;

  @override
  Widget build(BuildContext context) {
    Widget segment(RouteProvider provider, String label) {
      final selected = provider == value;
      return Material(
        color: selected ? CarTheme.accent : Colors.transparent,
        borderRadius: BorderRadius.circular(CarTheme.radiusSmall),
        child: InkWell(
          onTap: selected ? null : () => onChanged(provider),
          borderRadius: BorderRadius.circular(CarTheme.radiusSmall),
          child: SizedBox(
            height: 44,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: Center(
                child: Text(
                  label,
                  style: CarTheme.label.copyWith(
                    fontSize: 13,
                    color: selected ? CarTheme.onAccent : CarTheme.textMuted,
                  ),
                ),
              ),
            ),
          ),
        ),
      );
    }

    return Semantics(
      label: 'Penyedia rute',
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: CarTheme.surfaceHigh,
          borderRadius: BorderRadius.circular(CarTheme.radius),
          border: Border.all(color: CarTheme.border),
        ),
        child: Padding(
          padding: const EdgeInsets.all(2),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              segment(RouteProvider.mapbox, 'Mapbox'),
              segment(RouteProvider.osrm, 'OSM'),
            ],
          ),
        ),
      ),
    );
  }
}

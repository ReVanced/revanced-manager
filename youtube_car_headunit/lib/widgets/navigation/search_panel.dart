import 'dart:async';

import 'package:flutter/material.dart';
import 'package:latlong2/latlong.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../../controllers/location_controller.dart';
import '../../controllers/navigation_controller.dart';
import '../../models/navigation.dart';
import '../../theme/car_theme.dart';
import 'nav_format.dart';

/// Destination search with results while typing, and the recent destinations while the
/// field is empty.
///
/// Photon allows search-as-you-type under fair use: a request goes out 400 ms after the
/// last keystroke and only from three characters on. A slower answer to an older query
/// is dropped, so results always match the text in the field.
class SearchPanel extends StatefulWidget {
  const SearchPanel({
    super.key,
    required this.navigation,
    required this.location,
    required this.onClose,
    this.compact = false,
  });

  static const debounce = Duration(milliseconds: 400);
  static const minChars = 3;

  final NavigationController navigation;
  final LocationController location;
  final VoidCallback onClose;

  /// Denser header and rows while the keyboard leaves little height, so two or three
  /// results still show.
  final bool compact;

  @override
  State<SearchPanel> createState() => _SearchPanelState();
}

class _SearchPanelState extends State<SearchPanel> {
  final TextEditingController _text = TextEditingController();
  Timer? _debounce;
  int _sequence = 0;
  List<Place> _results = const [];
  bool _loading = false;
  String? _error;

  String get _query => _text.text.trim();

  void _onChanged(String _) {
    _debounce?.cancel();
    if (_query.length < SearchPanel.minChars) {
      _sequence++; // drops an answer still on its way
      setState(() {
        _results = const [];
        _loading = false;
        _error = null;
      });
      return;
    }
    setState(() => _loading = true);
    _debounce = Timer(SearchPanel.debounce, _search);
  }

  Future<void> _search() async {
    _debounce?.cancel();
    final query = _query;
    if (query.length < SearchPanel.minChars) return;
    final sequence = ++_sequence;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final results = await widget.navigation.search(query);
      if (!mounted || sequence != _sequence) return;
      setState(() {
        _results = results;
        _loading = false;
      });
    } catch (e) {
      if (!mounted || sequence != _sequence) return;
      setState(() {
        _results = const [];
        _loading = false;
        _error = e is NavException
            ? e.message
            : 'Pencarian gagal. Periksa koneksi internet.';
      });
    }
  }

  void _clear() {
    _text.clear();
    _onChanged('');
  }

  void _pick(Place place) {
    FocusScope.of(context).unfocus();
    widget.navigation.previewTo(place);
    widget.onClose();
  }

  Future<void> _remove(Place place) async {
    await widget.navigation.removeRecent(place);
    if (mounted) setState(() {});
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _text.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      clip: true,
      padding: EdgeInsets.zero,
      child: Column(
        children: [
          Padding(
            padding: widget.compact
                ? const EdgeInsets.fromLTRB(4, 2, 8, 2)
                : const EdgeInsets.fromLTRB(4, 8, 8, 8),
            child: Row(
              children: [
                ControlButton(
                  icon: LucideIcons.arrowLeft,
                  tooltip: 'Tutup pencarian',
                  onPressed: widget.onClose,
                ),
                const SizedBox(width: 4),
                Expanded(
                  child: TextField(
                    controller: _text,
                    autofocus: true,
                    textInputAction: TextInputAction.search,
                    style: CarTheme.body,
                    onChanged: _onChanged,
                    onSubmitted: (_) => _search(),
                    decoration: InputDecoration(
                      isDense: widget.compact,
                      contentPadding: widget.compact
                          ? const EdgeInsets.symmetric(horizontal: 12, vertical: 10)
                          : null,
                      hintText: 'Cari tujuan',
                      prefixIcon: const Icon(LucideIcons.search, size: 20),
                      suffixIcon: ListenableBuilder(
                        listenable: _text,
                        builder: (context, _) => _text.text.isEmpty
                            ? const SizedBox.shrink()
                            : IconButton(
                                tooltip: 'Hapus',
                                icon: const Icon(LucideIcons.x, size: 20),
                                onPressed: _clear,
                              ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(child: _body()),
        ],
      ),
    );
  }

  Widget _body() {
    final query = _query;
    if (query.length < SearchPanel.minChars) {
      final recents = widget.navigation.recentDestinations;
      if (query.isNotEmpty) return const _Hint('Ketik minimal 3 huruf.');
      if (recents.isEmpty) {
        return const _Hint(
          'Ketik nama tempat atau alamat, atau tekan lama di peta untuk memilih titik.',
        );
      }
      return ListView(
        padding: const EdgeInsets.only(bottom: 8),
        children: [
          if (!widget.compact)
            const Padding(
              padding: EdgeInsets.fromLTRB(16, 12, 16, 4),
              child: Text('TUJUAN TERAKHIR', style: CarTheme.label),
            ),
          for (final place in recents)
            _PlaceRow(
              icon: LucideIcons.history,
              place: place,
              distance: _distanceTo(place),
              compact: widget.compact,
              onTap: () => _pick(place),
              onRemove: () => _remove(place),
            ),
        ],
      );
    }
    if (_error case final error?) {
      return _Hint(error, action: 'Coba lagi', onAction: _search);
    }
    if (_results.isEmpty) {
      return _loading
          ? const Center(child: CircularProgressIndicator())
          : const _Hint('Tidak ditemukan. Coba nama lain.');
    }
    return Stack(
      children: [
        ListView(
          padding: const EdgeInsets.only(bottom: 8),
          children: [
            for (final place in _results)
              _PlaceRow(
                icon: LucideIcons.mapPin,
                place: place,
                distance: _distanceTo(place),
                compact: widget.compact,
                onTap: () => _pick(place),
              ),
          ],
        ),
        // Newer results are on their way; the old ones stay until they arrive.
        if (_loading)
          const Positioned(
            left: 0,
            right: 0,
            top: 0,
            child: LinearProgressIndicator(minHeight: 2),
          ),
      ],
    );
  }

  String? _distanceTo(Place place) {
    final car = widget.location.position;
    if (car == null) return null;
    return formatDistance(
      const Distance().as(LengthUnit.Meter, car, place.location),
    );
  }
}

class _PlaceRow extends StatelessWidget {
  const _PlaceRow({
    required this.icon,
    required this.place,
    required this.distance,
    required this.onTap,
    this.compact = false,
    this.onRemove,
  });

  final IconData icon;
  final Place place;
  final String? distance;
  final bool compact;
  final VoidCallback onTap;
  final VoidCallback? onRemove;

  @override
  Widget build(BuildContext context) {
    final address = place.address;
    return InkWell(
      onTap: onTap,
      child: ConstrainedBox(
        constraints: BoxConstraints(minHeight: compact ? 48 : 60),
        child: Padding(
          padding: EdgeInsets.only(left: 16, right: onRemove == null ? 16 : 4),
          child: Row(
            children: [
              Icon(icon, size: 22, color: CarTheme.textMuted),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      place.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: CarTheme.body,
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
              if (distance case final distance?) ...[
                const SizedBox(width: 10),
                Text(distance, style: CarTheme.time),
              ],
              if (onRemove case final onRemove?)
                IconButton(
                  tooltip: 'Hapus dari riwayat',
                  constraints: const BoxConstraints.tightFor(width: 48, height: 48),
                  icon: const Icon(LucideIcons.x, size: 18, color: CarTheme.textFaint),
                  onPressed: onRemove,
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Hint extends StatelessWidget {
  const _Hint(this.text, {this.action, this.onAction});

  final String text;
  final String? action;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(text, style: CarTheme.caption),
          if (action case final action?) ...[
            const SizedBox(height: 12),
            FilledButton(onPressed: onAction, child: Text(action)),
          ],
        ],
      ),
    );
  }
}

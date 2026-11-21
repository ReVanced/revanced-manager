import 'dart:convert';

/// One HLS variant as YouTube's ios client lists it. The variant playlist carries video
/// only; its sound comes from the audio rendition of group [audioGroup].
class HlsVariant {
  const HlsVariant({
    required this.url,
    required this.width,
    required this.height,
    required this.codecs,
    required this.bitrate,
    required this.audioGroup,
    this.fps = 30,
  });

  final Uri url;
  final int width;
  final int height;

  /// The variant's CODECS attribute, e.g. `avc1.4D401F,mp4a.40.2`.
  final String codecs;
  final int bitrate;
  final String? audioGroup;
  final num fps;

  bool get isAvc => codecs.startsWith('avc1');
}

/// Picks the variant to play for [targetHeight], or null when none has an audio rendition.
///
/// H.264 (avc1) wins over VP9 whenever any exists, because every Android head unit decodes
/// it in hardware while VP9 often falls back to a software decoder that stutters on
/// low-end SoCs. Then: exact height, else the tallest below, else the shortest above;
/// ties prefer at most 30 fps and the lower bitrate.
HlsVariant? pickHlsVariant(List<HlsVariant> variants, int targetHeight) {
  HlsVariant? best;
  List<num>? bestKey;
  for (final variant in variants) {
    if (variant.audioGroup == null) continue;
    final key = _rank(variant, targetHeight);
    if (bestKey == null || _compareKeys(key, bestKey) < 0) {
      best = variant;
      bestKey = key;
    }
  }
  return best;
}

List<num> _rank(HlsVariant v, int target) {
  final group = v.height == target ? 0 : (v.height < target ? 1 : 2);
  return [
    v.isAvc ? 0 : 1,
    group,
    (v.height - target).abs(),
    v.fps > 30 ? 1 : 0,
    v.bitrate,
  ];
}

int _compareKeys(List<num> a, List<num> b) {
  for (var i = 0; i < a.length; i++) {
    final c = a[i].compareTo(b[i]);
    if (c != 0) return c;
  }
  return 0;
}

/// The parts of an HLS master playlist the app needs.
class HlsMaster {
  const HlsMaster({required this.variants, required this.audioGroups});

  final List<HlsVariant> variants;

  /// Audio rendition playlist per GROUP-ID.
  final Map<String, Uri> audioGroups;

  /// Variants whose audio group has a rendition, i.e. the ones that can play with sound.
  List<HlsVariant> get playable => [
        for (final v in variants)
          if (audioGroups.containsKey(v.audioGroup)) v,
      ];
}

/// Parses variant streams and audio renditions from an HLS master playlist; other tags,
/// attributes and subtitle renditions are ignored. Relative URIs resolve against [base].
HlsMaster parseHlsMaster(String text, Uri base) {
  final variants = <HlsVariant>[];
  final audioGroups = <String, Uri>{};
  Map<String, String>? streamInf;
  for (final raw in const LineSplitter().convert(text)) {
    final line = raw.trim();
    if (line.isEmpty) continue;
    if (line.startsWith('#EXT-X-MEDIA:')) {
      final a = _attributes(line.substring('#EXT-X-MEDIA:'.length));
      final group = a['GROUP-ID'];
      final uri = a['URI'];
      if (a['TYPE'] == 'AUDIO' && group != null && uri != null) {
        audioGroups.putIfAbsent(group, () => base.resolve(uri));
      }
    } else if (line.startsWith('#EXT-X-STREAM-INF:')) {
      streamInf = _attributes(line.substring('#EXT-X-STREAM-INF:'.length));
    } else if (!line.startsWith('#') && streamInf != null) {
      final a = streamInf;
      streamInf = null;
      final size = (a['RESOLUTION'] ?? '').split('x');
      final width = size.length == 2 ? int.tryParse(size[0]) : null;
      final height = size.length == 2 ? int.tryParse(size[1]) : null;
      if (width == null || height == null) continue; // audio-only variant
      variants.add(
        HlsVariant(
          url: base.resolve(line),
          width: width,
          height: height,
          codecs: a['CODECS'] ?? '',
          bitrate: int.tryParse(a['BANDWIDTH'] ?? '') ?? 0,
          audioGroup: a['AUDIO'],
          fps: num.tryParse(a['FRAME-RATE'] ?? '') ?? 30,
        ),
      );
    }
  }
  return HlsMaster(variants: variants, audioGroups: audioGroups);
}

/// Quoted values may contain commas (CODECS="avc1...,mp4a..."), so a plain split fails.
final RegExp _attributePattern = RegExp(r'([A-Z0-9-]+)=("[^"]*"|[^,]*)');

Map<String, String> _attributes(String list) {
  return {
    for (final m in _attributePattern.allMatches(list))
      m.group(1)!: m.group(2)!.startsWith('"')
          ? m.group(2)!.substring(1, m.group(2)!.length - 1)
          : m.group(2)!,
  };
}

/// Builds a one-variant HLS master so ExoPlayer plays exactly [variant] together with its
/// audio rendition. Playing the bare variant URL would be silent.
String buildHlsMaster({required HlsVariant variant, required Uri audioUrl}) {
  return [
    '#EXTM3U',
    '#EXT-X-INDEPENDENT-SEGMENTS',
    '#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud",NAME="default",DEFAULT=YES,'
        'AUTOSELECT=YES,URI="$audioUrl"',
    '#EXT-X-STREAM-INF:BANDWIDTH=${variant.bitrate},'
        'CODECS="${variant.codecs}",'
        'RESOLUTION=${variant.width}x${variant.height},AUDIO="aud"',
    '${variant.url}',
    '',
  ].join('\n');
}

/// Wraps a master playlist in a `data:` URI. video_player on Android hands non-asset URIs
/// to ExoPlayer's DefaultDataSource, which reads `data:` itself, so no file or local server
/// is needed. All URIs inside the playlist must be absolute.
Uri hlsMasterDataUri(String master) => Uri.dataFromBytes(
      utf8.encode(master),
      mimeType: 'application/vnd.apple.mpegurl',
    );

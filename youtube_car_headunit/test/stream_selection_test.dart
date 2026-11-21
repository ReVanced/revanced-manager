import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:youtube_car_headunit/services/stream_selection.dart';

HlsVariant variant(
  int height, {
  String codecs = 'avc1.4D401F,mp4a.40.2',
  String? audioGroup = '234',
  num fps = 25,
  int bitrate = 1000000,
}) {
  return HlsVariant(
    url: Uri.parse('https://example.com/$height/$codecs/index.m3u8'),
    width: height * 16 ~/ 9,
    height: height,
    codecs: codecs,
    bitrate: bitrate,
    audioGroup: audioGroup,
    fps: fps,
  );
}

void main() {
  // Shape of what the ios client returned for dQw4w9WgXcQ on 2026-11-21.
  final youtubeLike = [
    variant(144, codecs: 'avc1.4D400C,mp4a.40.5', audioGroup: '233'),
    variant(240, codecs: 'avc1.4D4015,mp4a.40.5', audioGroup: '233'),
    variant(360),
    variant(480),
    variant(720),
    variant(1080, codecs: 'avc1.640028,mp4a.40.2'),
    variant(720, codecs: 'vp09.00.31.08,mp4a.40.2'),
    variant(1080, codecs: 'vp09.00.40.08,mp4a.40.2'),
    variant(2160, codecs: 'vp09.00.50.08,mp4a.40.2'),
  ];

  test('exact height, H.264 over VP9', () {
    final pick = pickHlsVariant(youtubeLike, 720)!;
    expect(pick.height, 720);
    expect(pick.isAvc, isTrue);
  });

  test('tallest below the target when the exact height is missing', () {
    final pick = pickHlsVariant([variant(360), variant(480)], 720)!;
    expect(pick.height, 480);
  });

  test('shortest above when nothing is below', () {
    final pick = pickHlsVariant([variant(1080), variant(720)], 480)!;
    expect(pick.height, 720);
  });

  test('H.264 preferred even over an exact VP9 height', () {
    final pick = pickHlsVariant(
      [variant(720, codecs: 'vp09.00.31.08,mp4a.40.2'), variant(480)],
      720,
    )!;
    expect(pick.isAvc, isTrue);
    expect(pick.height, 480);
  });

  test('prefers 30 fps or less, then the lower bitrate', () {
    final pick = pickHlsVariant(
      [
        variant(720, fps: 60, bitrate: 2000000),
        variant(720, bitrate: 1500000),
        variant(720, bitrate: 1200000),
      ],
      720,
    )!;
    expect(pick.fps, 25);
    expect(pick.bitrate, 1200000);
  });

  test('variants without an audio rendition are unusable', () {
    expect(pickHlsVariant([variant(720, audioGroup: null)], 720), isNull);
    expect(pickHlsVariant(const [], 720), isNull);
  });

  test('master pairs the variant with its audio rendition', () {
    final video = variant(720);
    final audio = Uri.parse('https://example.com/audio234/index.m3u8');
    final master = buildHlsMaster(variant: video, audioUrl: audio);
    final lines = master.split('\n');
    expect(lines.first, '#EXTM3U');
    expect(
      master,
      contains('#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud",NAME="default",'
          'DEFAULT=YES,AUTOSELECT=YES,URI="$audio"'),
    );
    expect(
      master,
      contains('#EXT-X-STREAM-INF:BANDWIDTH=1000000,'
          'CODECS="avc1.4D401F,mp4a.40.2",RESOLUTION=1280x720,AUDIO="aud"'),
    );
    // The variant URI line directly follows its STREAM-INF tag.
    final infIndex = lines.indexWhere((l) => l.startsWith('#EXT-X-STREAM-INF'));
    expect(lines[infIndex + 1], video.url.toString());
  });

  // Trimmed from the ios master YouTube served for dQw4w9WgXcQ on 2026-11-21.
  const youtubeMaster = '''#EXTM3U
#EXT-X-INDEPENDENT-SEGMENTS
#EXT-X-MEDIA:URI="https://m.example/a233/index.m3u8",TYPE=AUDIO,GROUP-ID="233",NAME="Default",YT-EXT-ABSOLUTE-LOUDNESS=-13.010,DEFAULT=YES,AUTOSELECT=YES
#EXT-X-MEDIA:URI="https://m.example/a234/index.m3u8",TYPE=AUDIO,GROUP-ID="234",NAME="Default",YT-EXT-ABSOLUTE-LOUDNESS=-13.010,DEFAULT=YES,AUTOSELECT=YES
#EXT-X-MEDIA:URI="https://m.example/sub/en.m3u8",TYPE=SUBTITLES,GROUP-ID="vtt",LANGUAGE="en",NAME="English",DEFAULT=NO,AUTOSELECT=YES
#EXT-X-STREAM-INF:BANDWIDTH=300694,CODECS="avc1.4D4015,mp4a.40.5",RESOLUTION=426x240,FRAME-RATE=25,VIDEO-RANGE=SDR,YT-EXT-ABSOLUTE-LOUDNESS=-13.010,AUDIO="233",SUBTITLES="vtt",CLOSED-CAPTIONS=NONE
https://m.example/v229/index.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=1248435,CODECS="avc1.4D401F,mp4a.40.2",RESOLUTION=1280x720,FRAME-RATE=25,VIDEO-RANGE=SDR,AUDIO="234",SUBTITLES="vtt",CLOSED-CAPTIONS=NONE
https://m.example/v232/index.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=1100000,CODECS="vp09.00.31.08,mp4a.40.2",RESOLUTION=1280x720,FRAME-RATE=25,AUDIO="234"
https://m.example/v609/index.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=900000,CODECS="avc1.4D401E,mp4a.40.2",RESOLUTION=854x480,FRAME-RATE=25,AUDIO="999"
https://m.example/orphan/index.m3u8
''';

  test('parses YouTube master variants and audio renditions', () {
    final master =
        parseHlsMaster(youtubeMaster, Uri.parse('https://m.example/master'));
    expect(master.variants, hasLength(4));
    expect(master.audioGroups.keys, ['233', '234']);
    final v720 = master.variants[1];
    expect(v720.height, 720);
    expect(v720.width, 1280);
    expect(v720.codecs, 'avc1.4D401F,mp4a.40.2');
    expect(v720.bitrate, 1248435);
    expect(v720.fps, 25);
    expect(v720.audioGroup, '234');
    expect(v720.url.toString(), 'https://m.example/v232/index.m3u8');
    // The 480p variant points at an audio group without a rendition: unplayable.
    expect(master.playable.map((v) => v.height), [240, 720, 720]);
  });

  test('parsed master feeds the picker and builder end to end', () {
    final master =
        parseHlsMaster(youtubeMaster, Uri.parse('https://m.example/master'));
    final pick = pickHlsVariant(master.playable, 480)!;
    expect(pick.height, 240); // tallest playable H.264 below 480
    final hd = pickHlsVariant(master.playable, 720)!;
    expect(hd.url.toString(), 'https://m.example/v232/index.m3u8');
    final built = buildHlsMaster(
      variant: hd,
      audioUrl: master.audioGroups[hd.audioGroup]!,
    );
    expect(built, contains('URI="https://m.example/a234/index.m3u8"'));
  });

  test('relative URIs resolve against the master URL', () {
    const relative = '''#EXTM3U
#EXT-X-MEDIA:URI="audio/index.m3u8",TYPE=AUDIO,GROUP-ID="a",NAME="x"
#EXT-X-STREAM-INF:BANDWIDTH=1,CODECS="avc1.4D401F,mp4a.40.2",RESOLUTION=1280x720,AUDIO="a"
video/index.m3u8
''';
    final master =
        parseHlsMaster(relative, Uri.parse('https://m.example/dir/master.m3u8'));
    expect(master.audioGroups['a'].toString(),
        'https://m.example/dir/audio/index.m3u8');
    expect(master.variants.single.url.toString(),
        'https://m.example/dir/video/index.m3u8');
  });

  test('data URI round-trips the master', () {
    const master = '#EXTM3U\n#EXT-X-INDEPENDENT-SEGMENTS\n';
    final uri = hlsMasterDataUri(master);
    expect(uri.scheme, 'data');
    expect(uri.data!.mimeType, 'application/vnd.apple.mpegurl');
    expect(utf8.decode(uri.data!.contentAsBytes()), master);
  });
}

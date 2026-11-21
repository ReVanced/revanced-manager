// Indonesian turn instructions for OSRM steps.
//
// The compiler below is a Dart port of the v5 compiler of osrm-text-instructions
// (https://github.com/Project-OSRM/osrm-text-instructions, BSD 2-Clause License,
// Copyright (c) 2017, Project OSRM contributors), and [_id] mirrors the structure of its
// `languages/translations/id.json`. Keep both shapes in step with upstream so a newer
// id.json can be dropped in.

import '../models/navigation.dart';

/// The OSRM step fields an instruction is built from.
class StepText {
  const StepText({
    required this.type,
    this.modifier,
    this.name = '',
    this.ref,
    this.destinations,
    this.exits,
    this.exit,
    this.rotaryName,
    this.bearingAfter,
    this.drivingSide = 'right',
    this.mode = 'driving',
    this.motorway = false,
  });

  /// Raw OSRM `maneuver.type`, e.g. `turn`, `new name`, `roundabout`.
  final String type;

  /// Raw OSRM `maneuver.modifier`, e.g. `slight left`; null when absent.
  final String? modifier;
  final String name;
  final String? ref;
  final String? destinations;
  final String? exits;
  final int? exit;
  final String? rotaryName;
  final num? bearingAfter;
  final String drivingSide;
  final String mode;

  /// True when the road is a motorway (intersection class), which names it by its ref.
  final bool motorway;
}

/// The Indonesian instruction for one step, e.g. "Belok kiri ke Jalan Sudirman".
String compileInstruction(StepText step) {
  var type = step.type;
  final modifier = step.modifier;
  if (_id[type] == null) {
    // The OSRM spec lets servers add types; clients treat unknown ones as `turn`.
    type = 'turn';
  }

  Map<String, Object?> object;
  final modes = _id['modes'] as Map<String, Object?>;
  if (modes[step.mode] != null) {
    object = modes[step.mode] as Map<String, Object?>;
  } else {
    final typeTable = _id[type] as Map<String, Object?>;
    final omitSide = type == 'off ramp' &&
        modifier != null &&
        modifier.contains(step.drivingSide);
    if (modifier != null && typeTable[modifier] != null && !omitSide) {
      object = typeTable[modifier] as Map<String, Object?>;
    } else {
      object = typeTable['default'] as Map<String, Object?>;
    }
  }

  if (type == 'rotary' || type == 'roundabout') {
    final rotary = step.rotaryName;
    final exit = step.exit;
    if (rotary != null && exit != null && object['name_exit'] != null) {
      object = object['name_exit'] as Map<String, Object?>;
    } else if (rotary != null && object['name'] != null) {
      object = object['name'] as Map<String, Object?>;
    } else if (exit != null && object['exit'] != null) {
      object = object['exit'] as Map<String, Object?>;
    } else {
      object = object['default'] as Map<String, Object?>;
    }
  }

  final wayName = _wayName(step);
  final String template;
  if (object['destination'] != null && _has(step.destinations)) {
    template = object['destination'] as String;
  } else if (object['exit'] != null && _has(step.exits)) {
    template = object['exit'] as String;
  } else if (object['name'] != null && wayName.isNotEmpty) {
    template = object['name'] as String;
  } else {
    template = object['default'] as String;
  }

  final constants = _id['constants'] as Map<String, Object?>;
  final modifiers = constants['modifier'] as Map<String, Object?>;
  return _tokenize(template, {
    'way_name': wayName,
    'destination': _firstDestination(step.destinations),
    'exit': (step.exits ?? '').split(';').first,
    'exit_number': _ordinalize(step.exit ?? 1),
    'rotary_name': step.rotaryName ?? '',
    'modifier': modifier == null ? '' : (modifiers[modifier] as String? ?? ''),
    'direction': _direction(step.bearingAfter),
    'nth': '',
    'lane_instruction': '',
  });
}

/// The instruction announced ahead of the maneuver: "Dalam 300 meter, belok kiri ...".
String inDistance(String instruction, double meters) => _tokenize(
      _phrase('one in distance'),
      {
        'distance': spokenDistance(meters),
        'instruction_one': _lowerFirst(instruction)
      },
    );

/// Two maneuvers close together: "Belok kiri, kemudian belok kanan ...".
String linked(String first, String second) => _tokenize(
      _phrase('two linked'),
      {'instruction_one': first, 'instruction_two': _lowerFirst(second)},
    );

/// Distance for the banner: `80 m`, `350 m`, `1,2 km`, `15 km`.
String formatDistance(double meters) {
  final rounded = _roundMeters(meters);
  if (rounded < 1000) return '$rounded m';
  final km = meters / 1000;
  if (km < 10) return '${_decimal(km)} km';
  return '${km.round()} km';
}

/// Distance to speak: `50 meter`, `300 meter`, `1,5 kilometer`, `12 kilometer`.
String spokenDistance(double meters) {
  final rounded = _roundMeters(meters);
  if (rounded < 1000) return '$rounded meter';
  final km = meters / 1000;
  if (km < 10) return '${_decimal(km)} kilometer';
  return '${km.round()} kilometer';
}

/// Rounds to steps that read well while driving: 10 m under 100 m, 50 m under 500 m,
/// then 100 m.
int _roundMeters(double meters) {
  if (meters < 100) return ((meters / 10).round() * 10).clamp(10, 100);
  if (meters < 500) return (meters / 50).round() * 50;
  return (meters / 100).round() * 100;
}

/// One decimal with an Indonesian comma, without a trailing `,0`.
String _decimal(double value) {
  final tenths = (value * 10).round();
  if (tenths % 10 == 0) return '${tenths ~/ 10}';
  return '${tenths ~/ 10},${tenths % 10}';
}

ManeuverType maneuverTypeOf(String type) => switch (type) {
      'depart' => ManeuverType.depart,
      'arrive' => ManeuverType.arrive,
      'new name' => ManeuverType.newName,
      'continue' => ManeuverType.continueOn,
      'merge' => ManeuverType.merge,
      'on ramp' => ManeuverType.onRamp,
      'off ramp' => ManeuverType.offRamp,
      'fork' => ManeuverType.fork,
      'end of road' => ManeuverType.endOfRoad,
      'roundabout' => ManeuverType.roundabout,
      'rotary' => ManeuverType.rotary,
      'roundabout turn' => ManeuverType.roundaboutTurn,
      'exit roundabout' => ManeuverType.exitRoundabout,
      'exit rotary' => ManeuverType.exitRotary,
      'notification' => ManeuverType.notification,
      _ => ManeuverType.turn,
    };

ManeuverModifier maneuverModifierOf(String? modifier) => switch (modifier) {
      'uturn' => ManeuverModifier.uturn,
      'sharp right' => ManeuverModifier.sharpRight,
      'right' => ManeuverModifier.right,
      'slight right' => ManeuverModifier.slightRight,
      'straight' => ManeuverModifier.straight,
      'slight left' => ManeuverModifier.slightLeft,
      'left' => ManeuverModifier.left,
      'sharp left' => ManeuverModifier.sharpLeft,
      _ => ManeuverModifier.none,
    };

bool _has(String? s) => s != null && s.isNotEmpty;

String _wayName(StepText step) {
  var name = step.name;
  final rawRef = step.ref;
  final ref = (rawRef ?? '').split(';').first;
  // Mapbox mixes the ref into the name ("Jalan Tol (E 1)"); keep one of them.
  if (rawRef != null && name == rawRef) name = '';
  if (rawRef != null) name = name.replaceAll(' ($rawRef)', '');
  if (name.isNotEmpty && ref.isNotEmpty && name != ref && !step.motorway) {
    return _tokenize(_phrase('name and ref'), {'name': name, 'ref': ref});
  }
  if (name.isNotEmpty &&
      ref.isNotEmpty &&
      step.motorway &&
      RegExp(r'\d').hasMatch(ref)) {
    return ref;
  }
  if (name.isEmpty && ref.isNotEmpty) return ref;
  return name;
}

String _firstDestination(String? destinations) {
  if (!_has(destinations)) return '';
  final parts = destinations!.split(': ');
  final ref = parts.first.split(',').first;
  final dest = parts.length > 1 ? parts[1].split(',').first : '';
  if (ref.isNotEmpty && dest.isNotEmpty) return '$ref: $dest';
  return ref.isNotEmpty ? ref : dest;
}

String _ordinalize(int n) {
  final constants = _id['constants'] as Map<String, Object?>;
  final ordinals = constants['ordinalize'] as Map<String, Object?>;
  return ordinals['$n'] as String? ?? '';
}

String _direction(num? degree) {
  if (degree == null) return '';
  final constants = _id['constants'] as Map<String, Object?>;
  final d = constants['direction'] as Map<String, Object?>;
  final String key;
  if (degree >= 0 && degree <= 20) {
    key = 'north';
  } else if (degree > 20 && degree < 70) {
    key = 'northeast';
  } else if (degree >= 70 && degree <= 110) {
    key = 'east';
  } else if (degree > 110 && degree < 160) {
    key = 'southeast';
  } else if (degree >= 160 && degree <= 200) {
    key = 'south';
  } else if (degree > 200 && degree < 250) {
    key = 'southwest';
  } else if (degree >= 250 && degree <= 290) {
    key = 'west';
  } else if (degree > 290 && degree < 340) {
    key = 'northwest';
  } else if (degree >= 340 && degree <= 360) {
    key = 'north';
  } else {
    return '';
  }
  return d[key] as String;
}

String _phrase(String key) =>
    (_id['phrase'] as Map<String, Object?>)[key] as String;

final _token = RegExp(r'\{(\w+)(?::(\w+))?\}');

String _tokenize(String template, Map<String, String> tokens) {
  var out = template.replaceAllMapped(_token, (m) => tokens[m.group(1)] ?? '');
  // Empty tokens leave doubled or dangling spaces ("tujuan , di kiri").
  out = out.replaceAll(RegExp(r' {2,}'), ' ').replaceAll(' ,', ',').trim();
  return _upperFirst(out);
}

String _upperFirst(String s) =>
    s.isEmpty ? s : s[0].toUpperCase() + s.substring(1);

String _lowerFirst(String s) =>
    s.isEmpty ? s : s[0].toLowerCase() + s.substring(1);

/// `v5` of osrm-text-instructions' id.json, generated by
/// biblio/scratchpad/in-app-navigation/tools/gen_id_table.py. Upstream strings are kept
/// except where upstream left English or reads wrong aloud (ordinals as words, the
/// phrases, ramp exits, slight/sharp turns, leaving a roundabout); keys the compiler
/// never uses are dropped.
const Map<String, Object?> _id = {
  'constants': {
    'ordinalize': {
      '1': 'pertama',
      '2': 'kedua',
      '3': 'ketiga',
      '4': 'keempat',
      '5': 'kelima',
      '6': 'keenam',
      '7': 'ketujuh',
      '8': 'kedelapan',
      '9': 'kesembilan',
      '10': 'kesepuluh',
    },
    'direction': {
      'north': 'utara',
      'northeast': 'timur laut',
      'east': 'timur',
      'southeast': 'tenggara',
      'south': 'selatan',
      'southwest': 'barat daya',
      'west': 'barat',
      'northwest': 'barat laut',
    },
    'modifier': {
      'left': 'kiri',
      'right': 'kanan',
      'sharp left': 'tajam kiri',
      'sharp right': 'tajam kanan',
      'slight left': 'agak ke kiri',
      'slight right': 'agak ke kanan',
      'straight': 'lurus',
      'uturn': 'putar balik',
    },
  },
  'modes': {
    'ferry': {
      'default': 'Naik ferry',
      'name': 'Naik ferry di {way_name}',
      'destination': 'Naik ferry menuju {destination}',
    },
  },
  'phrase': {
    'two linked by distance':
        '{instruction_one}, kemudian, dalam {distance}, {instruction_two}',
    'two linked': '{instruction_one}, kemudian {instruction_two}',
    'one in distance': 'Dalam {distance}, {instruction_one}',
    'name and ref': '{name} ({ref})',
    'exit with number': 'keluar {exit}',
  },
  'arrive': {
    'default': {
      'default': 'Anda telah tiba di tujuan {nth}',
    },
    'left': {
      'default': 'Anda telah tiba di tujuan {nth}, di sebelah kiri',
    },
    'right': {
      'default': 'Anda telah tiba di tujuan {nth}, di sebelah kanan',
    },
    'sharp left': {
      'default': 'Anda telah tiba di tujuan {nth}, di sebelah kiri',
    },
    'sharp right': {
      'default': 'Anda telah tiba di tujuan {nth}, di sebelah kanan',
    },
    'slight right': {
      'default': 'Anda telah tiba di tujuan {nth}, di sebelah kanan',
    },
    'slight left': {
      'default': 'Anda telah tiba di tujuan {nth}, di sebelah kiri',
    },
    'straight': {
      'default': 'Anda telah tiba di tujuan {nth}, lurus saja',
    },
  },
  'continue': {
    'default': {
      'default': 'Belok {modifier}',
      'name': 'Terus {modifier} ke {way_name}',
      'destination': 'Belok {modifier} menuju {destination}',
      'exit': 'Belok {modifier} ke {way_name}',
    },
    'straight': {
      'default': 'Lurus terus',
      'name': 'Terus ke {way_name}',
      'destination': 'Terus menuju {destination}',
    },
    'sharp left': {
      'default': 'Belok kiri tajam',
      'name': 'Belok kiri tajam untuk tetap di {way_name}',
      'destination': 'Belok kiri tajam menuju {destination}',
    },
    'sharp right': {
      'default': 'Belok kanan tajam',
      'name': 'Belok kanan tajam untuk tetap di {way_name}',
      'destination': 'Belok kanan tajam menuju {destination}',
    },
    'slight left': {
      'default': 'Tetap agak di kiri',
      'name': 'Tetap agak di kiri ke {way_name}',
      'destination': 'Tetap agak di kiri menuju {destination}',
    },
    'slight right': {
      'default': 'Tetap agak di kanan',
      'name': 'Tetap agak di kanan ke {way_name}',
      'destination': 'Tetap agak di kanan menuju {destination}',
    },
    'uturn': {
      'default': 'Putar balik',
      'name': 'Putar balik ke arah {way_name}',
      'destination': 'Putar balik menuju {destination}',
    },
  },
  'depart': {
    'default': {
      'default': 'Arah {direction}',
      'name': 'Arah {direction} di {way_name}',
    },
  },
  'end of road': {
    'default': {
      'default': 'Belok {modifier}',
      'name': 'Belok {modifier} ke {way_name}',
      'destination': 'Belok {modifier} menuju {destination}',
    },
    'straight': {
      'default': 'Lurus terus',
      'name': 'Tetap lurus ke {way_name}',
      'destination': 'Tetap lurus menuju {destination}',
    },
    'uturn': {
      'default': 'Putar balik di akhir jalan',
      'name': 'Putar balik di {way_name} di akhir jalan',
      'destination': 'Putar balik menuju {destination} di akhir jalan',
    },
  },
  'fork': {
    'default': {
      'default': 'Tetap {modifier} di pertigaan',
      'name': 'Tetap {modifier} di pertigaan ke {way_name}',
      'destination': 'Tetap {modifier} di pertigaan menuju {destination}',
    },
    'slight left': {
      'default': 'Tetap di kiri pada pertigaan',
      'name': 'Tetap di kiri pada pertigaan ke arah {way_name}',
      'destination': 'Tetap di kiri pada pertigaan menuju {destination}',
    },
    'slight right': {
      'default': 'Tetap di kanan pada pertigaan',
      'name': 'Tetap di kanan pada pertigaan ke arah {way_name}',
      'destination': 'Tetap di kanan pada pertigaan menuju {destination}',
    },
    'sharp left': {
      'default': 'Belok kiri pada pertigaan',
      'name': 'Belok kiri tajam ke arah {way_name}',
      'destination': 'Belok kiri tajam menuju {destination}',
    },
    'sharp right': {
      'default': 'Belok kanan pada pertigaan',
      'name': 'Belok kanan tajam ke arah {way_name}',
      'destination': 'Belok kanan tajam menuju {destination}',
    },
    'uturn': {
      'default': 'Putar balik',
      'name': 'Putar balik ke arah {way_name}',
      'destination': 'Putar balik menuju {destination}',
    },
  },
  'merge': {
    'default': {
      'default': 'Bergabung {modifier}',
      'name': 'Bergabung {modifier} ke arah {way_name}',
      'destination': 'Bergabung {modifier} menuju {destination}',
    },
    'straight': {
      'default': 'Bergabung lurus',
      'name': 'Bergabung lurus ke arah {way_name}',
      'destination': 'Bergabung lurus menuju {destination}',
    },
    'slight left': {
      'default': 'Bergabung di kiri',
      'name': 'Bergabung di kiri ke arah {way_name}',
      'destination': 'Bergabung di kiri menuju {destination}',
    },
    'slight right': {
      'default': 'Bergabung di kanan',
      'name': 'Bergabung di kanan ke arah {way_name}',
      'destination': 'Bergabung di kanan menuju {destination}',
    },
    'sharp left': {
      'default': 'Bergabung di kiri',
      'name': 'Bergabung di kiri ke arah {way_name}',
      'destination': 'Bergabung di kiri menuju {destination}',
    },
    'sharp right': {
      'default': 'Bergabung di kanan',
      'name': 'Bergabung di kanan ke arah {way_name}',
      'destination': 'Bergabung di kanan menuju {destination}',
    },
    'uturn': {
      'default': 'Putar balik',
      'name': 'Putar balik ke arah {way_name}',
      'destination': 'Putar balik menuju {destination}',
    },
  },
  'new name': {
    'default': {
      'default': 'Lanjutkan {modifier}',
      'name': 'Lanjutkan {modifier} menuju {way_name}',
      'destination': 'Lanjutkan {modifier} menuju {destination}',
    },
    'straight': {
      'default': 'Lurus terus',
      'name': 'Terus ke {way_name}',
      'destination': 'Terus menuju {destination}',
    },
    'sharp left': {
      'default': 'Belok kiri tajam',
      'name': 'Belok kiri tajam ke arah {way_name}',
      'destination': 'Belok kiri tajam menuju {destination}',
    },
    'sharp right': {
      'default': 'Belok kanan tajam',
      'name': 'Belok kanan tajam ke arah {way_name}',
      'destination': 'Belok kanan tajam menuju {destination}',
    },
    'slight left': {
      'default': 'Lanjut dengan agak ke kiri',
      'name': 'Lanjut dengan agak di kiri ke {way_name}',
      'destination': 'Tetap agak di kiri menuju {destination}',
    },
    'slight right': {
      'default': 'Tetap agak di kanan',
      'name': 'Tetap agak di kanan ke {way_name}',
      'destination': 'Tetap agak di kanan menuju {destination}',
    },
    'uturn': {
      'default': 'Putar balik',
      'name': 'Putar balik ke arah {way_name}',
      'destination': 'Putar balik menuju {destination}',
    },
  },
  'notification': {
    'default': {
      'default': 'Lanjutkan {modifier}',
      'name': 'Lanjutkan {modifier} menuju {way_name}',
      'destination': 'Lanjutkan {modifier} menuju {destination}',
    },
    'uturn': {
      'default': 'Putar balik',
      'name': 'Putar balik ke arah {way_name}',
      'destination': 'Putar balik menuju {destination}',
    },
  },
  'off ramp': {
    'default': {
      'default': 'Ambil jalan melandai',
      'name': 'Ambil jalan melandai ke {way_name}',
      'destination': 'Ambil jalan melandai menuju {destination}',
      'exit': 'Ambil jalan keluar {exit}',
      'exit_destination': 'Ambil jalan keluar {exit} menuju {destination}',
    },
    'left': {
      'default': 'Ambil jalan yang melandai di sebelah kiri',
      'name': 'Ambil jalan melandai di sebelah kiri ke arah {way_name}',
      'destination':
          'Ambil jalan melandai di sebelah kiri menuju {destination}',
      'exit': 'Ambil jalan keluar {exit} di sebelah kiri',
      'exit_destination':
          'Ambil jalan keluar {exit} di sebelah kiri menuju {destination}',
    },
    'right': {
      'default': 'Ambil jalan melandai di sebelah kanan',
      'name': 'Ambil jalan melandai di sebelah kanan ke {way_name}',
      'destination':
          'Ambil jalan melandai di sebelah kanan menuju {destination}',
      'exit': 'Ambil jalan keluar {exit} di sebelah kanan',
      'exit_destination':
          'Ambil jalan keluar {exit} di sebelah kanan menuju {destination}',
    },
    'sharp left': {
      'default': 'Ambil jalan yang melandai di sebelah kiri',
      'name': 'Ambil jalan melandai di sebelah kiri ke arah {way_name}',
      'destination':
          'Ambil jalan melandai di sebelah kiri menuju {destination}',
      'exit': 'Ambil jalan keluar {exit} di sebelah kiri',
      'exit_destination':
          'Ambil jalan keluar {exit} di sebelah kiri menuju {destination}',
    },
    'sharp right': {
      'default': 'Ambil jalan melandai di sebelah kanan',
      'name': 'Ambil jalan melandai di sebelah kanan ke {way_name}',
      'destination':
          'Ambil jalan melandai di sebelah kanan menuju {destination}',
      'exit': 'Ambil jalan keluar {exit} di sebelah kanan',
      'exit_destination':
          'Ambil jalan keluar {exit} di sebelah kanan menuju {destination}',
    },
    'slight left': {
      'default': 'Ambil jalan yang melandai di sebelah kiri',
      'name': 'Ambil jalan melandai di sebelah kiri ke arah {way_name}',
      'destination':
          'Ambil jalan melandai di sebelah kiri menuju {destination}',
      'exit': 'Ambil jalan keluar {exit} di sebelah kiri',
      'exit_destination':
          'Ambil jalan keluar {exit} di sebelah kiri menuju {destination}',
    },
    'slight right': {
      'default': 'Ambil jalan melandai di sebelah kanan',
      'name': 'Ambil jalan melandai di sebelah kanan ke {way_name}',
      'destination':
          'Ambil jalan melandai di sebelah kanan menuju {destination}',
      'exit': 'Ambil jalan keluar {exit} di sebelah kanan',
      'exit_destination':
          'Ambil jalan keluar {exit} di sebelah kanan menuju {destination}',
    },
  },
  'on ramp': {
    'default': {
      'default': 'Ambil jalan melandai',
      'name': 'Ambil jalan melandai ke {way_name}',
      'destination': 'Ambil jalan melandai menuju {destination}',
    },
    'left': {
      'default': 'Ambil jalan yang melandai di sebelah kiri',
      'name': 'Ambil jalan melandai di sebelah kiri ke arah {way_name}',
      'destination':
          'Ambil jalan melandai di sebelah kiri menuju {destination}',
    },
    'right': {
      'default': 'Ambil jalan melandai di sebelah kanan',
      'name': 'Ambil jalan melandai di sebelah kanan ke {way_name}',
      'destination':
          'Ambil jalan melandai di sebelah kanan menuju {destination}',
    },
    'sharp left': {
      'default': 'Ambil jalan yang melandai di sebelah kiri',
      'name': 'Ambil jalan melandai di sebelah kiri ke arah {way_name}',
      'destination':
          'Ambil jalan melandai di sebelah kiri menuju {destination}',
    },
    'sharp right': {
      'default': 'Ambil jalan melandai di sebelah kanan',
      'name': 'Ambil jalan melandai di sebelah kanan ke {way_name}',
      'destination':
          'Ambil jalan melandai di sebelah kanan menuju {destination}',
    },
    'slight left': {
      'default': 'Ambil jalan yang melandai di sebelah kiri',
      'name': 'Ambil jalan melandai di sebelah kiri ke arah {way_name}',
      'destination':
          'Ambil jalan melandai di sebelah kiri menuju {destination}',
    },
    'slight right': {
      'default': 'Ambil jalan melandai di sebelah kanan',
      'name': 'Ambil jalan melandai di sebelah kanan ke {way_name}',
      'destination':
          'Ambil jalan melandai di sebelah kanan menuju {destination}',
    },
  },
  'rotary': {
    'default': {
      'default': {
        'default': 'Masuk bundaran',
        'name': 'Masuk bundaran dan keluar arah {way_name}',
        'destination': 'Masuk bundaran dan keluar menuju {destination}',
      },
      'name': {
        'default': 'Masuk {rotary_name}',
        'name': 'Masuk {rotary_name} dan keluar arah {way_name}',
        'destination': 'Masuk {rotary_name} dan keluar menuju {destination}',
      },
      'exit': {
        'default': 'Masuk bundaran dan ambil jalan keluar {exit_number}',
        'name':
            'Masuk bundaran dan ambil jalan keluar {exit_number} arah {way_name}',
        'destination':
            'Masuk bundaran dan ambil jalan keluar {exit_number} menuju {destination}',
      },
      'name_exit': {
        'default': 'Masuk {rotary_name} dan ambil jalan keluar {exit_number}',
        'name':
            'Masuk {rotary_name} dan ambil jalan keluar {exit_number} arah {way_name}',
        'destination':
            'Masuk {rotary_name} dan ambil jalan keluar {exit_number} menuju {destination}',
      },
    },
  },
  'roundabout': {
    'default': {
      'exit': {
        'default': 'Masuk bundaran dan ambil jalan keluar {exit_number}',
        'name':
            'Masuk bundaran dan ambil jalan keluar {exit_number} arah {way_name}',
        'destination':
            'Masuk bundaran dan ambil jalan keluar {exit_number} menuju {destination}',
      },
      'default': {
        'default': 'Masuk bundaran',
        'name': 'Masuk bundaran dan keluar arah {way_name}',
        'destination': 'Masuk bundaran dan keluar menuju {destination}',
      },
    },
  },
  'roundabout turn': {
    'default': {
      'default': 'Lakukan {modifier}',
      'name': 'Lakukan {modifier} ke arah {way_name}',
      'destination': 'Lakukan {modifier} menuju {destination}',
    },
    'left': {
      'default': 'Belok kiri',
      'name': 'Belok kiri ke {way_name}',
      'destination': 'Belok kiri menuju {destination}',
    },
    'right': {
      'default': 'Belok kanan',
      'name': 'Belok kanan ke {way_name}',
      'destination': 'Belok kanan menuju {destination}',
    },
    'straight': {
      'default': 'Lurus terus',
      'name': 'Tetap lurus ke {way_name}',
      'destination': 'Tetap lurus menuju {destination}',
    },
  },
  'exit roundabout': {
    'default': {
      'default': 'Keluar dari bundaran',
      'name': 'Keluar dari bundaran ke arah {way_name}',
      'destination': 'Keluar dari bundaran menuju {destination}',
    },
  },
  'exit rotary': {
    'default': {
      'default': 'Keluar dari bundaran',
      'name': 'Keluar dari bundaran ke arah {way_name}',
      'destination': 'Keluar dari bundaran menuju {destination}',
    },
  },
  'turn': {
    'default': {
      'default': 'Lakukan {modifier}',
      'name': 'Lakukan {modifier} ke arah {way_name}',
      'destination': 'Lakukan {modifier} menuju {destination}',
    },
    'left': {
      'default': 'Belok kiri',
      'name': 'Belok kiri ke {way_name}',
      'destination': 'Belok kiri menuju {destination}',
    },
    'right': {
      'default': 'Belok kanan',
      'name': 'Belok kanan ke {way_name}',
      'destination': 'Belok kanan menuju {destination}',
    },
    'straight': {
      'default': 'Lurus',
      'name': 'Lurus arah {way_name}',
      'destination': 'Lurus menuju {destination}',
    },
    'slight left': {
      'default': 'Belok agak ke kiri',
      'name': 'Belok agak ke kiri ke {way_name}',
      'destination': 'Belok agak ke kiri menuju {destination}',
    },
    'slight right': {
      'default': 'Belok agak ke kanan',
      'name': 'Belok agak ke kanan ke {way_name}',
      'destination': 'Belok agak ke kanan menuju {destination}',
    },
    'sharp left': {
      'default': 'Belok kiri tajam',
      'name': 'Belok kiri tajam ke {way_name}',
      'destination': 'Belok kiri tajam menuju {destination}',
    },
    'sharp right': {
      'default': 'Belok kanan tajam',
      'name': 'Belok kanan tajam ke {way_name}',
      'destination': 'Belok kanan tajam menuju {destination}',
    },
    'uturn': {
      'default': 'Putar balik',
      'name': 'Putar balik ke arah {way_name}',
      'destination': 'Putar balik menuju {destination}',
    },
  },
  'use lane': {
    'no_lanes': {
      'default': 'Lurus terus',
    },
    'default': {
      'default': 'Lurus terus',
    },
  },
};

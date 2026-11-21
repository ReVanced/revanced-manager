import 'package:flutter_test/flutter_test.dart';
import 'package:youtube_car_headunit/models/navigation.dart';
import 'package:youtube_car_headunit/services/route_instructions.dart';

void main() {
  group('compileInstruction', () {
    test('turns with and without a street name', () {
      expect(
        compileInstruction(const StepText(
            type: 'turn', modifier: 'left', name: 'Jalan Sudirman')),
        'Belok kiri ke Jalan Sudirman',
      );
      expect(compileInstruction(const StepText(type: 'turn', modifier: 'right')),
          'Belok kanan');
      expect(compileInstruction(const StepText(type: 'turn', modifier: 'uturn')),
          'Putar balik');
      expect(
          compileInstruction(const StepText(type: 'turn', modifier: 'slight left')),
          'Belok agak ke kiri');
    });

    test('destinations win over the street name', () {
      expect(
        compileInstruction(const StepText(
          type: 'on ramp',
          modifier: 'left',
          name: 'Jalan Tol',
          destinations: '2: Semanggi, Cawang',
        )),
        'Ambil jalan melandai di sebelah kiri menuju 2: Semanggi',
      );
    });

    test('name and ref are combined, ref alone names an unnamed road', () {
      expect(
        compileInstruction(const StepText(
            type: 'new name', modifier: 'straight', name: 'Jalan Gatot Subroto', ref: '2')),
        'Terus ke Jalan Gatot Subroto (2)',
      );
      expect(
        compileInstruction(
            const StepText(type: 'turn', modifier: 'right', name: '', ref: 'E1')),
        'Belok kanan ke E1',
      );
    });

    test('roundabouts name the exit', () {
      expect(
        compileInstruction(const StepText(
            type: 'roundabout', modifier: 'straight', exit: 2, name: 'Jalan Thamrin')),
        'Masuk bundaran dan ambil jalan keluar kedua arah Jalan Thamrin',
      );
      expect(
        compileInstruction(const StepText(
            type: 'rotary', exit: 3, rotaryName: 'Bundaran HI')),
        'Masuk Bundaran HI dan ambil jalan keluar ketiga',
      );
      expect(compileInstruction(const StepText(type: 'roundabout')), 'Masuk bundaran');
      expect(
        compileInstruction(const StepText(
            type: 'exit roundabout', modifier: 'straight', name: 'Jalan C3')),
        'Keluar dari bundaran ke arah Jalan C3',
      );
    });

    test('depart gives the compass direction, arrive the side', () {
      expect(
        compileInstruction(const StepText(
            type: 'depart', bearingAfter: 182, name: 'Jalan Thamrin')),
        'Arah selatan di Jalan Thamrin',
      );
      expect(compileInstruction(const StepText(type: 'depart', bearingAfter: 45)),
          'Arah timur laut');
      // Upstream's "tujuan ke-{nth}" has no ordinal for the final destination.
      expect(compileInstruction(const StepText(type: 'arrive')),
          'Anda telah tiba di tujuan');
      expect(
          compileInstruction(const StepText(type: 'arrive', name: 'Jalan Sudirman')),
          'Anda telah tiba di tujuan');
      expect(compileInstruction(const StepText(type: 'arrive', modifier: 'right')),
          'Anda telah tiba di tujuan, di sebelah kanan');
    });

    test('off ramp on the driving side omits the side', () {
      expect(
        compileInstruction(const StepText(
            type: 'off ramp', modifier: 'slight right', exits: '12A;12B')),
        'Ambil jalan keluar 12A',
      );
      expect(
        compileInstruction(const StepText(type: 'off ramp', modifier: 'slight left')),
        'Ambil jalan yang melandai di sebelah kiri',
      );
    });

    test('unknown types are treated as turns, ferries by mode', () {
      expect(
        compileInstruction(
            const StepText(type: 'teleport', modifier: 'left', name: 'Jalan X')),
        'Belok kiri ke Jalan X',
      );
      expect(
        compileInstruction(
            const StepText(type: 'turn', modifier: 'left', mode: 'ferry')),
        'Naik ferry',
      );
    });
  });

  test('distances read the way a driver hears them', () {
    expect(formatDistance(8), '10 m');
    expect(formatDistance(87), '90 m');
    expect(formatDistance(342), '350 m');
    expect(formatDistance(780), '800 m');
    expect(formatDistance(999), '1 km');
    expect(formatDistance(1240), '1,2 km');
    expect(formatDistance(15400), '15 km');
    expect(spokenDistance(300), '300 meter');
    expect(spokenDistance(1500), '1,5 kilometer');
    expect(spokenDistance(2000), '2 kilometer');
  });

  test('phrases for prompts ahead of a maneuver', () {
    expect(inDistance('Belok kiri ke Jalan Sudirman', 300),
        'Dalam 300 meter, belok kiri ke Jalan Sudirman');
    expect(linked('Belok kanan', 'Belok kiri ke Jalan Dua'),
        'Belok kanan, kemudian belok kiri ke Jalan Dua');
  });

  test('OSRM types and modifiers map to the enums', () {
    expect(maneuverTypeOf('new name'), ManeuverType.newName);
    expect(maneuverTypeOf('exit roundabout'), ManeuverType.exitRoundabout);
    expect(maneuverTypeOf('something new'), ManeuverType.turn);
    expect(maneuverModifierOf('slight left'), ManeuverModifier.slightLeft);
    expect(maneuverModifierOf(null), ManeuverModifier.none);
  });
}

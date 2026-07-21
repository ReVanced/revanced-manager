import 'package:flutter_test/flutter_test.dart';
import 'package:youtube_car_headunit/main.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const YouTubeCarApp());
    await tester.pump(const Duration(seconds: 1));
  });
}

// ignore_for_file: avoid_print

import 'dart:convert';
import 'dart:io';

T? removeBlankEntries<T>(T? json) {
  // This function is protected by BSD 3-Clause License
  // Changes made to this section are allow removing of '' values from JSON

  /*
  https://pub.dev/documentation/swiss_knife/latest/swiss_knife/removeEmptyEntries.html

  Copyright 2014, the Dart project authors. All rights reserved.
  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:

      * Redistributions of source code must retain the above copyright
        notice, this list of conditions and the following disclaimer.
      * Redistributions in binary form must reproduce the above
        copyright notice, this list of conditions and the following
        disclaimer in the documentation and/or other materials provided
        with the distribution.
      * Neither the name of Google Inc. nor the names of its
        contributors may be used to endorse or promote products derived
        from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
  if (json == null) {
    return null;
  }
  if (json is List) {
    json.removeWhere((e) => e == null);
    json.forEach(removeBlankEntries);
  } else if (json is Map) {
    json.removeWhere(
      (key, value) => key == null || value == null || value == '',
    );
    json.values.forEach(removeBlankEntries);
  }
  return json;
}

Future<void> processJsonFiles() async {
  final Directory directory = Directory.current;
  final List<FileSystemEntity> files = directory.listSync();

  for (final file in files) {
    try {
      if (file is File && file.path.endsWith('.json')) {
        final String contents = await file.readAsString();
        final dynamic json = jsonDecode(contents);
        final dynamic processedJson = removeBlankEntries(json);

        file.writeAsString(
          const JsonEncoder.withIndent('  ').convert(processedJson),
        );
        print('🥞 Task successful on: ${file.path}');
      }
    } catch (e) {
      print('💥 Task failed on: ${file.path}: $e');
    }
  }
}

void main() async {
  processJsonFiles();
}

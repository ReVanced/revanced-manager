// ignore_for_file: avoid_print, prefer_foreach

import 'dart:convert';
import 'dart:io';

dynamic removeBlankEntries(dynamic json) {
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
    for (int i = json.length - 1; i >= 0; i--) {
      final processedElement = removeBlankEntries(json[i]);
      if (processedElement == null) {
        json.removeAt(i);
      } else {
        json[i] = processedElement;
      }
    }
    return json.isEmpty ? null : json;
  } else if (json is Map) {
    final keysToRemove = <dynamic>{};
    final keys = json.keys.toList();

    for (final key in keys) {
      if (key == null) {
        keysToRemove.add(key);
        continue;
      }
      final processedValue = removeBlankEntries(json[key]);
      if (processedValue == null || processedValue == '') {
        keysToRemove.add(key);
      } else {
        json[key] = processedValue;
      }
    }
    for (final key in keysToRemove) {
      json.remove(key);
    }
    return json.isEmpty ? null : json;
  }

  if (json is String && json.isEmpty) {
    return null;
  }

  return json;
}

Future<void> processJsonFiles() async {
  final Directory directory = Directory.current;
  final List<FileSystemEntity> files = directory.listSync();

  for (final file in files) {
    if (!file.path.endsWith('.json') || file is! File) {
      continue;
    }
    try {
      final contents = await file.readAsString();
      if (contents.trim().isEmpty) {
        print('🗑️ File is empty, deleting: ${file.path}');
        await file.delete();
        continue;
      }

      dynamic jsonInput;
      try {
        jsonInput = jsonDecode(contents);
      } on FormatException catch (e, stackTrace) {
        print('💥 Invalid JSON in file: ${file.path}: $e');
        print(stackTrace);
        continue;
      }

      final dynamic processedJson = removeBlankEntries(jsonInput);
      if (processedJson == null) {
        await file.delete();
        print('🗑️ File resulted in empty JSON, deleted: ${file.path}');
      } else {
        final prettyJson = const JsonEncoder.withIndent(
          '  ',  // Two spaces
        ).convert(processedJson);
        await file.writeAsString(prettyJson);
        print('🥞 Task successful on: ${file.path}');
      }
    } catch (e, stackTrace) {
      print('💥 Task failed on: ${file.path}: $e');
      print(stackTrace);
    }
  }
}

void main() async {
  await processJsonFiles();
}

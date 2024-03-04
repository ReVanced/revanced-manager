# 🛠️ Building from source

Learn how to build ReVanced Manager from source.

1. Setup the Flutter environment for your [platform](https://docs.flutter.dev/get-started/install)

2. Clone the repository

   ```sh
   git clone https://github.com/revanced/revanced-manager.git && cd revanced-manager
   ```

3. Get dependencies

   ```sh
   flutter pub get
   ```

4. Generate temporary files

   ```sh
   dart run slang
   dart run build_runner build --delete-conflicting-outputs
   ```

5. Build the APK

   ```sh
   flutter build apk
   ```

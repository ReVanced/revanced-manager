# 🛠 Building from source

1. Setup the Flutter environment for your [platform](https://docs.flutter.dev/get-started/install)

2. Clone the repository
   ```sh
   git clone https://github.com/revanced/revanced-manager.git && cd revanced-manager
   ```

3. Create a GitHub PAT with the `read:packages` scope [here](https://github.com/settings/tokens/new?scopes=read:packages&description=Revanced) and add your token in `android/gradle.properties`

   ```properties
   gpr.user = YourUsername
   gpr.key = ghp_longrandomkey
   ```
   
4. Download project packages

   ```sh
   flutter pub get
   ```

5. Delete conflicting outputs

   > **Note**: must be run everytime you sync your local repository with the remote's

   ```sh
   flutter packages pub run build_runner build --delete-conflicting-outputs
   ```

6. Build the APK
   ```sh
   flutter build apk
   ```

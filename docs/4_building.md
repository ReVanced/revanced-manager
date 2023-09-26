# 🛠️ Building from source

This page will guide you through building ReVanced Manager from source.

1. Download Java SDK 17 ([Azul JDK](https://www.azul.com/downloads/?version=java-17-lts&package=jdk#zulu) or [OpenJDK](https://jdk.java.net/java-se-ri/17)) and add it to path

2. Clone the repository

   ```sh
   git clone https://github.com/revanced/revanced-manager.git && cd revanced-manager
   ```

3. Create a GitHub personal access token with the `read:packages` scope [here](https://github.com/settings/tokens/new?scopes=read:packages&description=ReVanced)

4. Add your GitHub username and the token to `~/.gradle/gradle.properties`

   ```properties
   gpr.user = YourUsername
   gpr.key = ghp_longrandomkey
   ```

5. Set the `sdk.dir` property in `local.properties` to your Android SDK location

   ```properties
   sdk.dir = /path/to/android/sdk
   ```

6. Build the APK

   Debug:
   ```sh
   ./gradlew assembleDebug
   ```

   Release:
   ```sh
   ./gradlew assembleRelease -Psign
   ```

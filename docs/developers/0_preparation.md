# 💼 Preparing a development environment
In order to compile ReVanced Manager, certain requirements must be met.

## 📝 Prerequisites
* Kotlin IDE such as [Android Studio](https://developer.android.com/studio)
* Knowledge of [Android](https://android.com) app development, and the [Kotlin](https://kotlinlang.org/) language.
* At least JDK 17 of any vendors

## 🏃 Prepare the environment
1. Clone the repository
   ```sh
   git clone https://github.com/ReVanced/revanced-manager.git && cd revanced-manager
   ```
2. Build the APK
   ```sh
   gradlew assembleRelease -Psign
   ```

## ⏭️ What's next
The next page will introduce you to the basic overview of ReVanced Manager

Continue: [💁 Overview](1_overview.md)

# 💼 Preparing a developer environment
In order to compile ReVanced Manager, certain requirements must be met.

## 📝 Prerequisites
* Kotlin IDE such as [Android Studio](https://developer.android.com/studio)
* Understanding of [Android](https://android.com) development, [Kotlin](https://kotlinlang.org/) language and [Jetpack Compose](https://developer.android.com/jetpack/compose).
* At least Java Development Kit version 17 of any vendors

## 🏃 Prepare the environment
1. Clone the repository
   ```sh
   git clone https://github.com/ReVanced/revanced-manager.git && cd revanced-manager
   ```
<!-- This assume that you can use Maven repository -->
2. Build the APK
   Release variant:
   ```sh
   gradlew assembleRelease -Psign
   ```

## ⏭️ What's next
The next page will introduce you to basic overview of ReVanced Manager

Continue: [💁 Overview](1_overview.md)

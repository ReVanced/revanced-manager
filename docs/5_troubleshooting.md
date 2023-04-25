# 🛟 Troubleshooting

This is a collection of common errors and fixes for the ReVanced Manager application.

- ### 💥 App not installed as package conflicts with an existing package

   Remove the previous installation to solve the conflict, then you can try installing the patched application again.

- ### 🚫 Non-root install is not possible with the current patches selection

   Navigate back to the **Patcher** page, tap the **Select patches** card, and tap the **Recommended** or **Default** chip.

- ### ❗️ Error code `135` or `139` when patching the application

   ReVanced Manager doesn't fully support patching with device that reported as ARM32.

   You can try using ReVanced Manager on other devices, or use the [ReVanced CLI](https://github.com/revanced/revanced-cli) to patch the application on a computer

   > **Warning**: We do **NOT** recommend downloading prebuilt APKs from internet as **they can be malicious**.

- ### 🚨 Patched application is crashing

   You can try following these steps:

   1. Select the app you want to patch
   2. Check if your current version matches the recommended version. If that's not the case, download the recommended APK version and save it to your device.
   3. Tap the **Select an application** card
   4. Tap the **Storage** button and select the downloaded APK through the file picker
   5. Tap on the **Select patches** card, and tap the **Recommended** or **Default** chip

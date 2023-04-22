# 🛟 Troubleshooting

This is a collection of errors and fixes for the ReVanced Manager application.

- ### 💥 App not installed as package conflicts with an existing package

   Remove the previous installation to solve the conflict.

- ### 🚫 Non-root install is not possible with the current patches selection

   Open up Patcher, select the **Recommended** or **Default** chip in **Select patches**.

- ### 🪢 Signature mismatch when updating the application

   Uninstall the app that you're trying to update and then reinstalling it from ReVanced Manager.

- ### ❗️Error code `135` or `139` when patching the application

   ReVanced Manager doesn't fully support patching with device that reported as ARM32.

   You can use [ReVanced CLI](https://github.com/revanced/revanced-cli) to patch the application.

   > **Warning**: We don't recommend that you download prebuilt APK from internet as they can be malicious.

- ### 🚨 Patched application from ReVanced Manager crashing

   Just follow these steps in ReVanced Manager:

   1. Select the app you want to patch.
   2. Check if your current version matches the recommended version.
      1. If not, download the recommended APK version and save it to your device.
      2. Tap the **Select an application** card.
      3. click on the **Storage** button and select the APK through file picker.
   3. Go to "Select patches" and press the **Recommended** or **Default** chip.

   That's it! You should now be good to go.

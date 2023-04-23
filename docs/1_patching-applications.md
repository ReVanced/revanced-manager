# 🧩 Patching applications
Here's everything you need to patch an application.

# 🪜 How to use ReVanced Manager to patch applications 

1. Tap the **Patcher** button in the bottom navigation bar
2. Tap the **Select an application** card
3. Here, you have the option to choose between selecting an already-installed app on your device or selecting an APK file from storage.
   * For selecting on-device applications, tap on the desired app from the shown list. For an app to show up in this list, the app must be already installed on the device.
     > **Warning**: This feature is incomplete for non-root users as ReVanced has yet to add split APK support. We suggest you pick your APK from storage for now.  
     **However**, this will **NOT** work for rooted installations. You'll have to install an APK over your current app installation and select the app from the list.
   * To select from storage, click on the **Storage** on the bottom right of the screen button and select the APK through the file picker.
     > **Note**: We recommend downloading APK files from [APKMirror](https://www.apkmirror.com/) as a temporary solution as `.apkm` and `.apks` files are not supported at the moment.
   * Verify that the selected application is a version supported by ReVanced. [See the list of supported versions for apps here.](https://github.com/revanced/revanced-patches#-patches)
4. After selecting an application, you will be brought back to the **Patcher** screen again. Tap the **Select patches** card.
5. Select your desired patches
   * You can tap the **Default** chip to select the recommended patches.
   * If you are selecting patches manually, be aware that certain patches are **required** for non-root installations. (e.g. **MicroG Support** patch for YouTube)
   * If you wish to do a rooted installation, you'll need to **deselect** patches mentioned above.
6. Tap the **Done** button
7. You will be brought back to the **Patcher** screen again. Tap the **Patch** button to start patching.
8. The app is now patching. This process can range from 2-10 minutes depending on your device. Refrain from closing the Manager or switching tabs while the process is still running.
9.  When patching is complete, tap on the **Install** button. You may also tap on the three vertical dots in the top right to share logs or export the patched APK.
   > **Warning**: If you close the Manager after patching, the patched APK file will be **automatically deleted from your device**!

## ⏭️ What's next

The next page will guide you through managing your installed patched applications.

Continue: [🧰 Managing patched applications](2_managing-patched-applications.md)
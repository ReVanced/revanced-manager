# ⚙️ Configuring ReVanced Manager

Learn how to configure ReVanced Manager.

## 🎛️ Essential settings

- ### 🪛 Allow changing patch selection

  By default, you can not change the patch selection and the default selection is used. Enabling this will allow you to change the patch selection.

  > ⚠️ Warning  
  > Changing the selection may cause unexpected issues. Unless you know what you are doing, it is recommended to keep this disabled.

- ### 📦 Require suggested app version

  By default, ReVanced Manager allows you to patch an app if the suggested version to patch matches the version of the app you selected. Disabling this will allow you to patch an app even if the suggested version does not match the version of the app you selected.

  > ⚠️ Warning  
  > Patches are more likely to fail on versions that are not suggested by ReVanced Manager. Unless you know what you are doing, it is recommended to keep this enabled.

- ### ✅ Version compatibility check

  By default, ReVanced Manager does not allow you to select patches that are not compatible with the version of the app you selected. Disabling this will allow you to select patches that are not compatible with the version of the app you selected.

  > ⚠️ Warning  
  > Patches are more likely to fail on versions they are not compatible with. Unless you know what you are doing, it is recommended to keep this enabled.

- ### 🧑‍🔬 Show universal patches

  By default, ReVanced Manager only shows patches that are compatible with specific apps. Enabling this will show patches that are intended to work on all apps.

  > ⚠️ Warning  
  > Patches that are intended to may not work on all apps. Unless you know what you are doing, it is recommended to keep this disabled.

- ### 🔗 API URL

  Configure the API URL to use. The API is used to download patches and updates.

- ### 🧬 Use alternative source

  Use alternative sources for patches and integrations instead of the API.

  > ℹ️ Note  
  > ReVanced Manager will still use the API for updates.

- ### 💾 Import, export and reset options

  You can import, export or reset saved settings. This includes:

  - 🔑 Keystore used to sign patched apps
  - 📄 Remembered selection of patches for each app
  - ⚙️ Remembered patch options
  - 🛠️ Remembered settings

  > ℹ️ Note  
  > These can be used to backup and restore or reset settings to default in case of issues.

- ### ❓ About

  View information about your device and ReVanced Manager. This includes the version of ReVanced Manager and architectures your device supports.

## ⏭️ What's next

The next page will explain how to troubleshoot issues with ReVanced Manager.

Continue: [❔ Troubleshooting](3_troubleshooting.md)

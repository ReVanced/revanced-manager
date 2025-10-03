# ⚙️ Configuring ReVanced Manager

Learn how to configure ReVanced Manager.

## 🔧 Settings

- **Downloads**: Enable or disable ReVanced Manager downloader and manage past downloaded apps here
- **Import & export**: Import or export patch selections, patch options and the signing keystore
- **Advanced**:
  - **API URL**: Set the URL of the ReVanced API, ReVanced Manager will use
  - **Disable version compatibility check**: Patching versions of apps the patches are explicitly compatible with is enforced.
    Disabling this will allow patching versions of apps the patches are not explicitly compatible with
    >   ⚠️ Warning  
    >   Patches may fail on app versions they are not explicitly compatible with.
    >   Unless you know what you are doing, it is recommended to keep this enabled.
  - **Require suggested app version**: Specific versions of apps is enforced based on the patch selection automatically.
    Disabling this will allow you to patch any version of apps
    >   ⚠️ Warning  
    >   Patches not compatible with the selected version of the app will not be used.
  - **Allow changing patch selection and options**: The default selection of patches is enforced.
    Enabling this will allow you to change the patch selection
    >   ⚠️ Warning  
    >   Changing the selection may cause unexpected issues.
    >   Unless you know what you are doing, it is recommended to keep this disabled.
    >   Unless you know what you are doing, it is recommended to keep this enabled.
  - **Allow using universal patches**: Patches that do not specify compatibility with an app are forcibly disabled.
    Enabling this will allow selecting such patches
    >   ⚠️ Warning  
    >   Universal patches do not specify compatibility with an app and may not work on all apps regardless.
    >   Unless you know what you are doing, it is recommended to keep this disabled.
- **About**: View more information and links about ReVanced and ReVanced Manager.

## ⏭️ What's next

The next page will explain how to troubleshoot issues with ReVanced Manager.

Continue: [❓ Troubleshooting](3_troubleshooting.md)

# ✒️ Verification and Authenticity of ReVanced Manager

> [!NOTE]
> This information is more relevant to alternative store providers

To distribute ReVanced Manager safely and recommended way is to provide the users with a link to 
[1. ⬇️ Installation, ✒️ Verifying authenticity of ReVanced Manager (user-side)][installation authenticity].

The certificate SHA-256 of the APK will always be `b6362c6ea7888efd15c0800f480786ad0f5b133b4f84e12d46afba5f9eac1223` unless otherwise noted.

[installation authenticity]: /docs/1_installation.md#%EF%B8%8F-verifying-authenticity-of-revanced-manager

## `libaapt2.so`

ReVanced Manager includes prebuilt binaries of [`libaapt2.so`][location of libaapt2.so] from https://github.com/ReVanced/aapt2 

which fixes issue on ARM32-based system, attestation is provided here: https://github.com/ReVanced/aapt2/attestations

[location of libaapt2.so]: /app/src/main/jniLibs/

[![GitHub Downloads](https://img.shields.io/github/downloads/cssnr/tibs3dprints-android/total?logo=github)](https://github.com/cssnr/tibs3dprints-android/releases/latest/download/tibs3dprints.apk)
[![GitHub Release Version](https://img.shields.io/github/v/release/cssnr/tibs3dprints-android?logo=github)](https://github.com/cssnr/tibs3dprints-android/releases/latest)
[![Action Lint](https://img.shields.io/github/actions/workflow/status/cssnr/tibs3dprints-android/release.yaml?logo=github&logoColor=white&label=lint)](https://github.com/cssnr/tibs3dprints-android/actions/workflows/lint.yaml)
[![Action Release](https://img.shields.io/github/actions/workflow/status/cssnr/tibs3dprints-android/lint.yaml?logo=github&logoColor=white&label=release)](https://github.com/cssnr/tibs3dprints-android/actions/workflows/release.yaml)
[![GitHub Top Language](https://img.shields.io/github/languages/top/cssnr/tibs3dprints-android?logo=htmx)](https://github.com/cssnr/tibs3dprints-android)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/cssnr/tibs3dprints-android?logo=github&label=updated)](https://github.com/cssnr/tibs3dprints-android/graphs/commit-activity)
[![GitHub Repo Size](https://img.shields.io/github/repo-size/cssnr/tibs3dprints-android?logo=bookstack&logoColor=white&label=repo%20size)](https://github.com/cssnr/tibs3dprints-android)
[![GitHub Discussions](https://img.shields.io/github/discussions/cssnr/tibs3dprints-android)](https://github.com/cssnr/tibs3dprints-android/discussions)
[![GitHub Forks](https://img.shields.io/github/forks/cssnr/tibs3dprints-android?style=flat&logo=github)](https://github.com/cssnr/tibs3dprints-android/forks)
[![GitHub Repo Stars](https://img.shields.io/github/stars/cssnr/tibs3dprints-android?style=flat&logo=github)](https://github.com/cssnr/tibs3dprints-android/stargazers)
[![GitHub Org Stars](https://img.shields.io/github/stars/cssnr?style=flat&logo=github&label=org%20stars)](https://cssnr.github.io/)
[![Discord](https://img.shields.io/discord/899171661457293343?logo=discord&logoColor=white&label=discord&color=7289da)](https://discord.gg/wXy6m2X8wY)

# Tibs3DPrints Android

[![GitHub Release](https://img.shields.io/github/v/release/cssnr/tibs3dprints-android?style=for-the-badge&logo=android&label=Download%20Android%20APK&color=A4C639)](https://github.com/cssnr/tibs3dprints-android/releases/latest/download/tibs3dprints.apk)

- [Install](#Install)
- [Development](#Development)
  - [Android Studio](#Android-Studio)
  - [Command Line](#Command-Line)
- [Support](#Support)
- [Contributing](#Contributing)

Android Application for Tibs3DPrints: https://tibs3dprints.com/

- Supports Android 8 (API 26) 2017 or Newer.

## Install

> [!TIP]  
> To install, download and open the [latest release](https://github.com/cssnr/tibs3dprints-android/releases/latest).
>
> [![GitHub Release](https://img.shields.io/github/v/release/cssnr/tibs3dprints-android?style=for-the-badge&logo=android&label=Download%20Android%20APK&color=A4C639)](https://github.com/cssnr/tibs3dprints-android/releases/latest/download/tibs3dprints.apk)

<details><summary>📸 View QR Code</summary>

[![QR Code](https://raw.githubusercontent.com/smashedr/repo-images/refs/heads/master/tibs3dprints/qr-code-download.png)](https://github.com/cssnr/tibs3dprints-android/releases/latest/download/tibs3dprints.apk)

</details>

_Note: Until published on the play store, you may need to allow installation of apps from unknown sources._

Downloading and Installing the [apk](https://github.com/cssnr/tibs3dprints-android/releases/latest/download/tibs3dprints.apk)
should take you to the settings area to allow installation if not already enabled.

<details><summary>View Manual Steps to Install from Unknown Sources</summary>

1. Go to your device settings.
2. Search for "Install unknown apps" or similar.
3. Choose the app you will install the apk file from.
   - Select your web browser to install directly from it.
   - Select your file manager to open it, locate the apk and install from there.
4. Download the [Latest Release](https://github.com/cssnr/tibs3dprints-android/releases/latest/download/tibs3dprints.apk).
5. Open the download apk in the app you selected in step #3.
6. Choose Install and Accept any Play Protect notifications.
7. The app is now installed. Proceed to the [Setup](#Setup) section below.

</details>

# Development

This section briefly covers running and building in [Android Studio](#Android-Studio) and the [Command Line](#Command-Line).

## Android Studio

1. Download and Install Android Studio.

   https://developer.android.com/studio

2. Ensure that usb or wifi debugging is enabled in the Android developer settings and verify.

3. Then build or run the app on your device.
   - Import the Project
   - Run Gradle Sync

To Run: Select device and press Play ▶️

To Build:

- Select the Build Variant (debug or release)
- Build > Generate App Bundles or APK > Generate APKs

## Command Line

_Note: This section is a WIP! For more details see the [release.yaml](.github/workflows/release.yaml)._

You will need to have [ADB](https://developer.android.com/tools/adb) installed.

<details><summary>Click Here to Download and Install a Release</summary>

```shell
$ wget https://github.com/cssnr/tibs3dprints-android/releases/latest/download/tibs3dprints.apk
$ ls
tibs3dprints.apk

$ which adb
C:\Users\Shane\Android\sdk\platform-tools\adb.EXE

$ adb devices
List of devices attached
RF9M33Z1Q0M     device

$ adb -s RF9M33Z1Q0M install tibs3dprints.apk
Performing Incremental Install
Serving...
All files should be loaded. Notifying the device.
Success
Install command complete in 917 ms
```

See below for more details...

</details>

1. Download and Install the Android SDK Platform Tools.

https://developer.android.com/tools/releases/platform-tools#downloads

Ensure that `adb` is in your PATH.

2. List and verify the device is connected with:

```shell
$ adb devices
List of devices attached
RF9M33Z1Q0M     device
```

3. Build a debug or release apk.

```shell
./gradlew assemble
./gradlew assembleRelease
```

_Note: Use `gradlew.bat` for Windows._

4. Then install the apk to your device with adb.

```shell
$ cd app/build/outputs/apk/debug
$ adb -s RF9M33Z1Q0M install app-debug.apk
```

```shell
$ cd app/build/outputs/apk/release
$ adb -s RF9M33Z1Q0M install app-release-unsigned.apk
```

_Note: you may have to uninstall before installing due to different certificate signatures._

For more details, see the [ADB Documentation](https://developer.android.com/tools/adb#move).

# Support

For general help or to request a feature, see:

- Q&A Discussion: https://github.com/cssnr/tibs3dprints-android/discussions/categories/q-a
- Request a Feature: https://github.com/cssnr/tibs3dprints-android/discussions/categories/feature-requests

If you are experiencing an issue/bug or getting unexpected results, you can:

- Report an Issue: https://github.com/cssnr/tibs3dprints-android/issues
- Chat with us on Discord: https://discord.gg/wXy6m2X8wY
- Provide General Feedback: [https://cssnr.github.io/feedback/](https://cssnr.github.io/feedback/?app=Tibs3DPrints%20Android%20App)

# Contributing

Currently, the best way to contribute to this project is to star this project on GitHub.

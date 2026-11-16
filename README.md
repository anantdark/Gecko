<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" alt="Gecko" width="120" />
</p>

<h1 align="center">Gecko</h1>

<p align="center">
  <strong>Rewind the room. Catch what just happened.</strong><br />
  Time-travelling audio recorder for Android — a rolling mic buffer so you can save the past, then optionally keep recording forward.
</p>

<p align="center">
  <a href="https://github.com/anantdark/Gecko/releases/latest"><img src="https://img.shields.io/badge/Download-GitHub%20Releases-1FA97A?style=for-the-badge&logo=github&logoColor=white" alt="Download from GitHub Releases" /></a>
  &nbsp;
  <a href="https://f-droid.org/packages/com.anant.gecko/"><img src="https://img.shields.io/badge/Get%20it%20on-F--Droid-1976D2?style=for-the-badge&logo=f-droid&logoColor=white" alt="Get it on F-Droid" /></a>
</p>

<p align="center">
  <a href="https://anantdark.github.io/Gecko/"><img src="https://img.shields.io/badge/Website-anantdark.github.io%2FGecko-0E2A1F?style=flat-square" alt="Website" /></a>
  <a href="https://github.com/anantdark/Gecko/actions/workflows/ci.yml"><img src="https://github.com/anantdark/Gecko/actions/workflows/ci.yml/badge.svg" alt="CI" /></a>
  <a href="https://github.com/anantdark/Gecko/actions/workflows/release.yml"><img src="https://github.com/anantdark/Gecko/actions/workflows/release.yml/badge.svg" alt="Release" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License: GPL v3" /></a>
</p>

<p align="center">
  <img src="docs/screenshots/01-home-biolume.jpg" alt="Home — Biolume pack" width="22%" />
  <img src="docs/screenshots/03-home-sonar.jpg" alt="Home — Sonar pack" width="22%" />
  <img src="docs/screenshots/04-home-volt.jpg" alt="Home — Volt pack" width="22%" />
  <img src="docs/screenshots/02-settings.jpg" alt="Settings" width="22%" />
</p>

## Download

| Channel | Link |
| --- | --- |
| **GitHub Releases** | [Latest APK](https://github.com/anantdark/Gecko/releases/latest) (`Gecko-latest.apk`) |
| **F-Droid** | [com.anant.gecko](https://f-droid.org/packages/com.anant.gecko/) |

Same package id on both channels. Prefer F-Droid for automatic updates; use GitHub for the newest signed build.

## What it does

Gecko listens into an in-memory ring buffer. When something worth keeping just happened, tap a duration chip to export the last N minutes (or everything held). Press-and-hold a chip to save history **and** keep recording forward.

- Continuous listening via a foreground mic service
- Save last 1 / 5 / 30 minutes, 2 / 6 hours, a custom span, or the full buffer
- UI packs, launcher icons, RAM budget, sample rate (8 / 16 / 44.1 kHz)
- WAV export to `Music/Gecko` via MediaStore
- Restarts after reboot if listening was left on
- No ads, no analytics — audio stays on-device

Inspired by [Echo](https://github.com/mafik/echo).

## UI packs

Custom composables (not Material skins). Each pack swaps colors, shapes, dial, surface, type, and motion.

| Pack | Feel |
| --- | --- |
| **Biolume** | Mint canopy, lichen glow |
| **Sonar** | Night ocean console |
| **Oxide** | Slate deck, copper |
| **Volt** | Ink void, acid chartreuse |

## Battery

Defaults favor clear speech without burning the battery:

| Choice | Why |
| --- | --- |
| **16 kHz** / **256 MB** buffer | Speech-clear; drop to 8 kHz for quieter battery |
| Power-saving mic mode | Platform favors battery over latency |
| 80 ms blocking reads | Fewer wakeups; saves still drain the HW buffer first |
| Quiet capture when UI unbound | No StateFlow spam in the background |
| Silent low-importance notification | No sound/vibration while listening |

Reliability: mic FGS, `START_STICKY`, boot receiver, swipe restart, pause-and-drain before every export.

## Build

Flavors: `github` (default releases) and `fdroid`. Needs JDK 17+ (CI uses 21), Android SDK 36.

```bash
./gradlew :app:assembleGithubDebug
./gradlew :app:installGithubDebug
```

Release signing: see [DISTRIBUTION.md](DISTRIBUTION.md).

```bash
./gradlew :app:assembleGithubRelease
```

## License

[GPL-3.0](LICENSE)
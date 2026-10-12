# Publishing Gecko

Package: `com.anant.gecko`  
Display name: **Gecko**

## Flavors

| Flavor | `applicationId` | Purpose |
|--------|-----------------|--------|
| `github` | `com.anant.gecko` | GitHub Releases / sideload (CI default) |
| `fdroid` | `com.anant.gecko` | F-Droid source builds (`IS_FDROID=true`) |
| `*Debug` | `com.anant.gecko.debug` | Side-by-side with release |

## 1. Local signing

```bash
cp /Users/user/Desktop/FreeScale-signing-backup/freescale-release.jks .
cp /Users/user/Desktop/FreeScale-signing-backup/keystore.properties .
# or: cp keystore.properties.example keystore.properties  # then fill passwords
```

`*.jks` and `keystore.properties` are gitignored.

## 2. GitHub Actions secrets

| Secret | Value |
|--------|-------|
| `RELEASE_KEYSTORE_BASE64` | Base64 of `freescale-release.jks` |
| `RELEASE_STORE_PASSWORD` | Store password |
| `RELEASE_KEY_ALIAS` | `freescale` |
| `RELEASE_KEY_PASSWORD` | Key password |

CI writes a temporary `keystore.properties` on the runner from these secrets.

## 3. Build

```bash
# Debug (github flavor)
./gradlew :app:assembleGithubDebug
./gradlew :app:installGithubDebug

# Release APK / AAB (github flavor, signed if keystore.properties present)
./gradlew :app:assembleGithubRelease
./gradlew :app:bundleGithubRelease
```

Outputs:
- APK: `app/build/outputs/apk/github/release/Gecko-<versionName>.apk`
- AAB: `app/build/outputs/bundle/githubRelease/app-github-release.aab`

## 4. Versioning

- Local/dev: `1.0.0-dev` / `versionCode` 1
- CI on `main`: `-PappVersionCode=$GITHUB_RUN_NUMBER` / `-PappVersionName=1.<minor>.<patch>`
- F-Droid flavor: fixed `versionCode` / `versionName` in `app/build.gradle.kts` (bump by hand)

## 5. Channels

- **GitHub Latest** — `release.yml` on `main` (`Gecko-latest.apk`, `make_latest: true`)
- **F-Droid** — `fdroid-release.yml` (workflow_dispatch; prerelease tags, `make_latest: false`)

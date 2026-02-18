# Sound-Meter
Sound-meter is an android application that measures sound level in decibel<br/>

![image](https://github.com/jeff2900/Sound-Meter/blob/master/screenshots/sm_en.jpg)<br>
<br/>
Based on halibobo's project
https://github.com/halibobo/SoundMeter<br/>

## Features
- Real-time sound level measurement in decibels
- Visual gauge display
- Historical data tracking with charts
- Webhook integration for external notifications
- Multi-language support (English, French, Chinese)

## Building
To build the APK:
```bash
./gradlew assembleDebug   # For debug builds
./gradlew assembleRelease # For release builds
```

## Release Process
This project uses automated releases via GitHub Actions:

1. Update version in `app/build.gradle`:
   - Increment `versionCode`
   - Update `versionName`
2. Update `CHANGELOG.md` with release notes
3. Commit changes to master branch
4. Create and push a version tag:
   ```bash
   git tag -a v1.4.1 -m "Release version 1.4.1"
   git push origin v1.4.1
   ```
5. GitHub Actions will automatically:
   - Build the release APK
   - Create a GitHub release
   - Upload the APK as a release asset

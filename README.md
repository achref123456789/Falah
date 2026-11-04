# Falah Android app (Kotlin)

An Android starter app for prayer-time awareness and user-configured app interruption. Default coordinates are Tunis, Tunisia (36.8065, 10.1815). The app calculates approximate local prayer times on-device and requests location permission to update coordinates.

## Build an APK with GitHub Actions
1. Create a GitHub repository and upload the **contents** of this folder (including `.github`).
2. Open the repository's **Actions** tab.
3. Select **Build Falah APK** and press **Run workflow** (or push to `main`).
4. When the run completes, open it and download the `Falah-debug-apk` artifact. Extract it and install `app-debug.apk`.

## Included in this project
- Native Kotlin Android app and prayer-time calculation prototype.
- Tunis default location, manual coordinates and optional GPS update.
- Daily five-prayer check-off and a 90-day elapsed-day counter.
- User-selectable app list and Accessibility-based interruption screen.
- Green prayer interruption screen with `I PRAYED` and `NOT YET` actions.
- Alarm scheduling for the next calculated prayer times.
- Spoken English reminder using Android TextToSpeech 10 minutes before each prayer.
- Notification at the calculated prayer time.

## Important limitations (read before relying on it)
- Prayer times use a simplified solar calculation (Fajr 18°, Isha 17°, standard Asr), not an official Tunisian Ministry timetable. Confirm against your local mosque or official timetable and adjust coordinates/settings as needed.
- The project does **not** include a licensed Adhan recording. The prayer-time event is a notification, not a recited Adhan. The ten-minute spoken reminder is Android TextToSpeech in English; it is not an Adhan.
- Android exact-alarm, notification, location and Accessibility permissions must be granted. Some phone manufacturers may delay background alarms. App blocking is a best-effort interruption using Accessibility events; it is not tamper-proof and must never be relied on for emergency or safety-critical restrictions.
- The 90-day feature currently tracks elapsed days and the five daily checkboxes; it is not a sophisticated analytics/achievement system.
- Tasbih, Quran reader/tracker, Qibla compass, Hijri calendar, multilingual translations and a verified Dua catalogue are **not implemented** in this build.
- This source has not been compiled or device-tested in this environment. GitHub Actions is intended to compile it; resolve any build errors shown in the Actions log before installing.

# Lock PC – Admin App for Android

The remote control for **Lock PC**. Manage when your Windows computers are allowed to be used, straight from your phone.

## What you can do

- **See your computers** – every PC linked to your account, and whether it's online.
- **Set lock times** – add, edit or remove blocked periods, and choose which days of the week they apply to.
- **Pause a lock** – give a bit of extra time, either for a set number of minutes or until the current block ends.
- **Extend a session** – push a lock back when plans change.
- **See where the reminders phone is** – view the last known location of the phone running the reminders app on a map.

## Getting started

1. Install the app and log in with the same account you use on the Lock PC dashboard.
2. Your linked computers appear automatically.
3. Any changes you make are sent to the computers right away.

## The Lock PC family

| Project | What it is |
|---|---|
| [lock-pc](https://github.com/sysqo82/lock-pc) | The Windows app that does the locking |
| [lock-pc-local](https://github.com/sysqo82/lock-pc-local) | The server and web dashboard, run from home |
| **lock-pc-android** | Phone app for managing schedules on the go (this one) |
| [lock-pc-android-reminders](https://github.com/sysqo82/lock-pc-android-reminders) | Phone app that receives reminders |
| [lock-pc-server](https://github.com/sysqo82/lock-pc-server) | The earlier cloud-hosted version of the server |

## How it's built

- **Language:** Kotlin, native Android.
- **Talking to the server:** **Retrofit** for normal requests, plus **Socket.IO** for live updates on which PCs are online.
- **Map:** the location map uses **Leaflet** with **OpenStreetMap**.
- **Security:** your login is kept in Android's **encrypted storage**, with optional **fingerprint / face unlock**.
- **Server address:** set in `ApiConfig.kt`.

## Building it yourself

1. Open the project in **Android Studio** and let it sync.
2. If you're running your own server, update the address in `ApiConfig.kt`.
3. Press **Run**.

Tests live in `app/src/test` and can be run with `./gradlew test`.

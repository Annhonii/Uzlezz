# ClearTube

Native Android shell (Kotlin + Jetpack Compose) around **m.youtube.com**.

- Your own UI: floating search bar, pill top tabs, spring animations, shimmer loading (same look as Cleantube).
- YouTube itself runs inside a WebView, so login, history, subscriptions and **YouTube's own recommendations** work.
- Ads: ad data is removed from YouTube's responses (`assets/clean.js`), ad requests are blocked
  (`YtBrowser.isAdRequest`) and ad elements are hidden (`assets/clean.css`).

## Build
Open in Android Studio (JDK 17) or `./gradlew assembleDebug`. Pushing to GitHub `main` builds an APK
via `.github/workflows/build.yml` (Releases page).

## When something breaks (it will, now and then)
YouTube changes its page. Connect the phone, open `chrome://inspect` on a PC, inspect the page inside the app,
then fix the selectors in `assets/clean.css` / the keys in `assets/clean.js`. No Kotlin change needed.

Unofficial; against YouTube's ToS. Not for Play Store. Use an account you can afford to risk.

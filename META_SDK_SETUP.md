# Meta SDK (Audience Network) Setup

Meta ads are served through **AdMob mediation** — AdMob runs the auction and Meta Audience Network
is one of the bidding/waterfall ad sources. There is no separate Meta ad-loading code: once the
dashboards are configured, AdMob fetches Meta ads automatically through the adapter.

- **App package:** `com.translator.keyboard.translate.all.langugaes`
- **Adapter:** `com.google.ads.mediation:facebook:6.20.0.2` (`app/build.gradle`)
- **Meta SDK:** `com.facebook.android:audience-network-sdk:6.20.0` (`app/build.gradle`)
- **Meta app name:** `Translator pro`
- **Meta App ID:** `1648900333357605`
- **Client Token:** held separately (kept out of the repo — it is a credential, and the
  AdMob-mediated path does not need it in the committed code).

> These identifiers are entered in the **AdMob dashboard** when adding Meta as a mediation ad
> source (see §4); they are **not** placed in the app manifest/code for the mediation path.

---

## 1. What's already done in code ✅

| Item | Where |
|------|-------|
| AdMob SDK + UMP consent flow | `PreSplashScreenActivity.kt` |
| Meta mediation adapter dependency | `app/build.gradle` |
| Meta Audience Network SDK dependency | `app/build.gradle` |
| Meta consent / test-mode config | `app/src/main/java/.../classes/ads/MetaAdsConsent.kt` |
| `MetaAdsConsent.configure()` wired before `MobileAds.initialize()` | `PreSplashScreenActivity.kt` |

**No further app code is required** to serve Meta ads — mediation is configured server-side in the
AdMob and Meta dashboards. The only optional code touch is pasting a test-device hash (see §6).

---

## 2. What we need from the client

These are blockers — the integration cannot go live without them:

1. ~~**Meta App ID**~~ — ✅ received: `1648900333357605` (+ Client Token, held separately).
2. **Audience Network Placement IDs** — ⏳ still needed. One per ad format we mediate (banner /
   interstitial / native / rewarded); format `1648900333357605_xxxxxxxxxx`.
3. **Access to the Meta Business account** — add our developer account as **Admin/Developer**.
4. **Access to the AdMob account** — to add Meta as a mediation ad source per ad unit.
5. **Payout account on Audience Network** — the client's business/bank details (Meta pays the
   account owner; this cannot be ours).

> The client's Meta app and Audience Network account must own these — ad revenue pays out to the
> account owner, and the Play Store app is tied to the client's developer identity.

---

## 3. Meta Audience Network dashboard (client / us with access)

In **Monetization Manager** (business.facebook.com/monetization_manager):

1. Add the app as a **Property** (register package `com.translator.keyboard.translate.all.langugaes`).
2. Create **Placements** for each ad format → these generate the **Placement IDs**.
3. Complete **payment / payout** setup.
4. Submit the app for Audience Network **review** (usually needs the app live on Play, with test
   ads confirmed working first).

---

## 4. AdMob dashboard (client / us with access)

For **each** ad unit that should include Meta:

1. **Mediation** → create or edit a mediation group for that ad format.
2. Add **Meta Audience Network** as an ad source.
3. Enter the **App ID** + **Placement ID** from §3.
4. Prefer **Bidding** over Waterfall when available (real-time auction, no manual eCPM to maintain).
   If Waterfall, set the eCPM per placement manually.
5. (Optional, recommended) Authorize AdMob to pull Meta's reporting (needs a Meta login) so AdMob
   can auto-optimize eCPM.

---

## 5. Key hash

> A key hash is primarily a **Facebook Login** requirement. Pure **Audience Network** mediation
> identifies the app by **package name + App ID**, not key hash. Register a key hash only if the
> client's Meta app also enables Login or another product that needs it.

**Debug key hash** (from `~/.android/debug.keystore`):

```
cwCaNE88ZpnbTXmETm/1HN+dU40=
```

**Release key hash** — generate from the release keystore, or base64-encode the SHA-1 from
Play Console → *App integrity → App signing key certificate*:

```bash
keytool -exportcert -alias YOUR_ALIAS -keystore path/to/release.jks \
  -storepass STORE_PASS -keypass KEY_PASS | openssl sha1 -binary | openssl base64
```

Register the resulting hash(es) under the Meta app's Android platform settings.

---

## 6. Consent & privacy

Handled by `MetaAdsConsent.kt`:

- **GDPR / EEA** — automatic. The Meta SDK reads the IAB TCF v2 consent string that the existing
  Google UMP form writes to `SharedPreferences` (`IABTCF_*` keys). No extra code.
- **US state privacy (CCPA/CPRA)** — the `ENABLE_LIMITED_DATA_USE` flag (default `false` = normal
  processing). Set it to `true` only if the client's privacy policy / legal counsel requires Meta's
  Limited Data Use signal. With country/state = 0, Meta geolocates and applies LDU only where
  required.
- **COPPA** — declared as a mixed/adult (not child-directed) audience.
- The app's **Play Data Safety** form must reflect that Meta Audience Network may collect
  advertising data — confirm with the client.

---

## 7. Testing

1. Run a **debug** build — `MetaAdsConsent` enables Meta test mode automatically.
2. On first ad request from a device, Audience Network logs a line to logcat containing that
   device's **hashed ID**.
3. Paste the hash into `TEST_DEVICE_HASHES` in `MetaAdsConsent.kt` to receive Meta test ads on
   that device.
4. Verify in AdMob's mediation test suite / ad inspector that Meta fills.

> Note: AdMob **test ad unit IDs** return Google test ads, not mediated Meta ads. To see real Meta
> fills you need the live AdMob units with Meta mediation configured (§4) plus test mode above.

---

## 8. Go-live checklist

- [x] Meta App ID received (`1648900333357605`)
- [ ] Meta App ID added to AdMob mediation
- [ ] Placement IDs created and mapped to each ad unit in AdMob
- [ ] Payout account set up on Audience Network
- [ ] App passes Audience Network review
- [ ] Key hash registered (only if Login/other Meta product is used)
- [ ] `ENABLE_LIMITED_DATA_USE` decision confirmed with client
- [ ] Play Data Safety form updated
- [ ] Meta fills verified via AdMob ad inspector

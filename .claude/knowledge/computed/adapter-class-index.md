# Google Mobile Ads (GMA) — Vungle Android Mediation Adapter Class Index

## Adapter Entry Point

| Class | Superclass | Package |
|-------|-----------|---------|
| `VungleMediationAdapter` | `Adapter` (Google) | `com.google.ads.mediation.vungle` |

**Initialization**: `initialize(context, initConfig, callback)` → calls `VungleAds.init(context, appId)`
**Singleton**: `VungleInitializer` manages shared initialization state across all format instances

## Format Classes

| Format | Class | Google Interface |
|--------|-------|-----------------|
| Interstitial | `VungleInterstitialAdapter` | `MediationInterstitialAd` |
| Rewarded | `VungleRewardedAdapter` | `MediationRewardedAd` |
| Banner | `VungleBannerAdapter` | `MediationBannerAd` |

## VungleInitializer (Singleton)

```
VungleInitializer.getInstance()
  └─ initialize(context, appId, callback)
     ├─ Already initialized → immediate callback
     ├─ Initializing → queue callback
     └─ Not initialized → VungleAds.init() + queue callback
```

## Callback Mapping (Vungle → Google GMA)

| Vungle Callback | GMA Callback | Context |
|----------------|-------------|---------|
| `onAdLoaded(ad)` | `onSuccess(adapter)` | Load success |
| `onAdFailedToLoad(ad, error)` | `onFailure(adError)` | Load failure |
| `onAdStart(ad)` | `reportAdImpression()` | Impression |
| `onAdClicked(ad)` | `reportAdClicked()` | Click |
| `onAdEnd(ad)` | `onAdDismissedFullScreenContent()` | Fullscreen closed |
| `onAdRewarded(ad)` | `onUserEarnedReward(rewardItem)` | Reward granted |
| `onAdFailedToPlay(ad, error)` | `onAdFailedToShowFullScreenContent(error)` | Show failure |

## Bidding Support

- Implements `RtbAdapter` for real-time bidding
- `collectSignals(signalConfig, callback)` → `VungleAds.getBiddingToken(context)`
- `loadRtbInterstitialAd()` / `loadRtbRewardedAd()` / `loadRtbBannerAd()` for bid response loading

## Key Patterns

1. **Singleton VungleInitializer**: Thread-safe initialization with callback queuing, prevents duplicate init calls
2. **RtbAdapter**: Extends base `Adapter` with RTB signal collection for Google Open Bidding
3. **Error code mapping**: Vungle error codes mapped to `AdError` with Google-standard codes
4. **Banner size mapping**: Google `AdSize` → Vungle `BannerAdSize` via `VungleBannerAdSizeUtil`
5. **Server parameters**: App ID and placement ID extracted from `MediationConfiguration.getServerParameters()`
6. **Privacy**: GDPR/CCPA/COPPA forwarded via `VunglePrivacySettings` from mediation extras

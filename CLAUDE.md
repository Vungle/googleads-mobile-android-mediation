# Google Ads Mobile Android Mediation

## Overview
Google-maintained Android mediation adapters for the Google Mobile Ads SDK. Contains 21 third-party network adapters enabling mediation through AdMob/Ad Manager.

## Language
- **Java** (100%)

## Build System
- **Gradle** with Android Gradle Plugin 3.5.4
- Android Library modules per adapter
- Maven/Bintray publishing for distribution

## Architecture
- Directory structure: `ThirdPartyAdapters/{Network}/` with adapter module
- Adapters implement Google mediation interfaces: `MediationInterstitialAdapter`, `MediationBannerAdapter`, `MediationRewardedVideoAdAdapter`
- Ad formats: Rewarded Video, Interstitial, Banner

## Vungle Adapter
- Main class: `VungleMediationAdapter`
- Supporting classes: `VungleManager` (SDK lifecycle), `VungleInitializer` (singleton init), format-specific callback classes
- Adapter version: 6.9.1.0
- Pattern: Adapter + Manager + Initializer + per-format callbacks

## Platform Requirements
- Target SDK: 29
- Min SDK: 16
- Google Mobile Ads SDK: 19.7.0

## Example App
- Sample adapter implementation
- Custom event example
- Test application for manual verification

## Key Conventions
- One module per network under `ThirdPartyAdapters/`
- Singleton initialization pattern via dedicated Initializer class
- Manager class handles shared SDK lifecycle
- Format-specific callback classes (not inline)

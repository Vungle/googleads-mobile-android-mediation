# ADR-001: Google Mediation Adapter Interface

## Status
Accepted

## Context
Google Mobile Ads SDK mediates ads from third-party networks on Android. Each network adapter must conform to Google's mediation interfaces so the Google SDK can load and display ads uniformly regardless of the underlying network.

## Decision
Adapters implement Google's mediation interfaces:
- `MediationInterstitialAdapter` for interstitial ads
- `MediationBannerAdapter` for banner ads
- `MediationRewardedVideoAdAdapter` for rewarded video ads
- Newer `Adapter` base class for the updated mediation framework

The main adapter class (`{Network}MediationAdapter`) serves as the entry point. It delegates to format-specific classes that handle loading, displaying, and callback forwarding for each ad type.

## Consequences
- Strict conformance to Google's interface is required; no custom extensions
- Google controls the adapter lifecycle (instantiation, initialization, ad requests)
- Format-specific classes keep the main adapter manageable
- Must map network-specific error codes to Google AdMob error codes
- Adapters are instantiated by reflection, requiring a no-arg constructor

# ADR-002: Dual Framework Support (Legacy and New)

## Status
Accepted

## Context
Google transitioned from a legacy mediation framework (individual format interfaces like `MediationInterstitialAdapter`) to a newer unified `Adapter` base class. During the transition period, adapters need to support both frameworks to maintain backward compatibility.

## Decision
Adapters support both the legacy and new mediation frameworks:
- **Legacy**: Implement `MediationInterstitialAdapter`, `MediationBannerAdapter`, `MediationRewardedVideoAdAdapter` directly
- **New**: Extend the `Adapter` base class and implement `MediationInterstitialAd`, `MediationBannerAd`, `MediationRewardedAd`

The main adapter class bridges both frameworks, delegating to appropriate format-specific classes depending on which framework path is invoked.

## Consequences
- Backward compatibility is maintained during the framework transition
- Increased code complexity from supporting two calling conventions
- Format-specific classes may have legacy and new variants
- Eventually legacy support can be removed once migration is complete
- Testing must cover both framework paths

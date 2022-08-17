package com.vungle.mediation

import androidx.annotation.Keep
import com.google.android.gms.ads.mediation.rtb.RtbAdapter

/**
 * A [RtbAdapter] to load and show Vungle rewarded video ads using Google Mobile Ads SDK
 * mediation.
 */
@Keep
class VungleAdapter : VungleMediationAdapter()
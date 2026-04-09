// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.vungle.waterfall

import com.google.ads.mediation.vungle.VungleFactory
import com.google.ads.mediation.vungle.renderers.VungleInterstitialAd
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.vungle.ads.AdConfig

/** Loads Waterfall Interstitial ads. */
class VungleWaterfallInterstitialAd(
  mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>,
  vungleFactory: VungleFactory,
) : VungleInterstitialAd(mediationAdLoadCallback, vungleFactory) {

  override fun getAdMarkup(
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration
  ): String? {
    // No adMarkup for waterfall ads.
    return null
  }

  override fun maybeAddWatermarkToVungleAdConfig(
    adConfig: AdConfig,
    mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
  ) {
    // No need to add watermark for waterfall ads.
  }
}

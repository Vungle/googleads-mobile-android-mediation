// Copyright 2021 Google LLC
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

package com.google.ads.mediation.vungle.rtb;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.vungle.VungleFactory;
import com.google.ads.mediation.vungle.renderers.VungleInterstitialAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.vungle.ads.AdConfig;
import com.vungle.ads.InterstitialAdListener;
import org.jetbrains.annotations.NotNull;

public class VungleRtbInterstitialAd extends VungleInterstitialAd
    implements MediationInterstitialAd, InterstitialAdListener {

  public VungleRtbInterstitialAd(
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
              mediationAdLoadCallback,
      @NonNull VungleFactory vungleFactory) {
    super(mediationAdLoadCallback, vungleFactory);
  }

  @Override
  @Nullable
  public String getAdMarkup(
      @NotNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration) {
    return mediationInterstitialAdConfiguration.getBidResponse();
  }

  @Override
  public void maybeAddWatermarkToVungleAdConfig(
      @NotNull AdConfig adConfig,
      @NotNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration) {
    // Should add watermark for RTB.
    String watermark = mediationInterstitialAdConfiguration.getWatermark();
    if (!TextUtils.isEmpty(watermark)) {
      adConfig.setWatermark(watermark);
    }
  }
}

// Copyright 2019 Google LLC
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

package com.google.ads.mediation.vungle;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.RequestConfiguration;
import com.vungle.ads.InitializationListener;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleAds.WrapperFramework;
import com.vungle.ads.VunglePrivacySettings;

public class VungleInitializer {

  private static final VungleInitializer instance = new VungleInitializer();

  @NonNull
  public static VungleInitializer getInstance() {
    return instance;
  }

  private VungleInitializer() {
    VungleAds.setIntegrationName(
        WrapperFramework.admob,
        com.vungle.mediation.BuildConfig.ADAPTER_VERSION.replace('.', '_'));
  }

  public void initialize(
      final @NonNull String appId,
      final @NonNull Context context,
      @NonNull InitializationListener listener) {
    VungleSdkWrapper.delegate.init(context, appId, listener);
  }

  public void updateCoppaStatus(int configuration) {
    switch (configuration) {
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE:
        VunglePrivacySettings.setCOPPAStatus(true);
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE:
        VunglePrivacySettings.setCOPPAStatus(false);
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED:
      default:
        // Vungle SDK only supports updating a user's COPPA status with true and false
        // values. If you haven't specified how you would like your content treated with
        // respect to COPPA in ad requests, you must indicate in the Liftoff Monetize Publisher
        // Dashboard whether your app is directed toward children under age 13.
        break;
    }
  }

}

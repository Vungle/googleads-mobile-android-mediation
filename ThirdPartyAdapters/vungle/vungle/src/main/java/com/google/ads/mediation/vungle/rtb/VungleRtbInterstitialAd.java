package com.google.ads.mediation.vungle.rtb;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_APP_ID;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BaseAd;
import com.vungle.ads.InterstitialAd;
import com.vungle.ads.InterstitialAdListener;
import com.vungle.ads.VungleException;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.PlacementFinder;

public class VungleRtbInterstitialAd implements MediationInterstitialAd, InterstitialAdListener {

  @NonNull
  private final MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration;

  @NonNull
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      mediationAdLoadCallback;

  @Nullable
  private MediationInterstitialAdCallback mediationInterstitialAdCallback;

  private InterstitialAd interstitialAd;

  public VungleRtbInterstitialAd(
      @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
      @NonNull
          MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          mediationAdLoadCallback) {
    this.mediationInterstitialAdConfiguration = mediationInterstitialAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  public void render() {
    Bundle mediationExtras = mediationInterstitialAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationInterstitialAdConfiguration.getServerParameters();

    String appID = serverParameters.getString(KEY_APP_ID);

    if (TextUtils.isEmpty(appID)) {
      AdError error =
          new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid App ID.", ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String placement = PlacementFinder.findPlacement(mediationExtras, serverParameters);
    if (placement == null || placement.isEmpty()) {
      AdError error =
          new AdError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to load ad from Vungle. Missing or Invalid Placement ID.",
              ERROR_DOMAIN);
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String adMarkup = mediationInterstitialAdConfiguration.getBidResponse();

    AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);

    VungleInitializer.getInstance()
        .initialize(
            appID,
            mediationInterstitialAdConfiguration.getContext(),
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                interstitialAd = new InterstitialAd(placement, adConfig);
                interstitialAd.setAdListener(VungleRtbInterstitialAd.this);

                interstitialAd.load(adMarkup);
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.w(TAG, error.toString());
                mediationAdLoadCallback.onFailure(error);
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    interstitialAd.play();
  }

  @Override
  public void adClick(@NonNull BaseAd baseAd) {
    if (mediationInterstitialAdCallback != null) {
      mediationInterstitialAdCallback.reportAdClicked();
    }
  }

  @Override
  public void adEnd(@NonNull BaseAd baseAd) {
    if (mediationInterstitialAdCallback != null) {
      mediationInterstitialAdCallback.onAdClosed();
    }
  }

  @Override
  public void adImpression(@NonNull BaseAd baseAd) {
    if (mediationInterstitialAdCallback != null) {
      mediationInterstitialAdCallback.reportAdImpression();
    }
  }

  @Override
  public void adLoaded(@NonNull BaseAd baseAd) {
    mediationInterstitialAdCallback =
        mediationAdLoadCallback.onSuccess(VungleRtbInterstitialAd.this);
  }

  @Override
  public void adStart(@NonNull BaseAd baseAd) {
    if (mediationInterstitialAdCallback != null) {
      mediationInterstitialAdCallback.onAdOpened();
    }
  }

  @Override
  public void error(@NonNull BaseAd baseAd, @NonNull VungleException e) {
    AdError error = VungleMediationAdapter.getAdError(e);
    Log.w(TAG, error.toString());
    if (mediationInterstitialAdCallback != null) {
      mediationInterstitialAdCallback.onAdFailedToShow(error);
    } else {
      mediationAdLoadCallback.onFailure(error);
    }
  }

  @Override
  public void onAdLeftApplication(@NonNull BaseAd baseAd) {
    if (mediationInterstitialAdCallback != null) {
      mediationInterstitialAdCallback.onAdLeftApplication();
    }
  }
}

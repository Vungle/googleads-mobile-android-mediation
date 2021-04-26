package com.google.ads.mediation.vungle.rtb;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.vungle.mediation.AdapterParametersParser;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.VungleManager;
import com.vungle.warren.AdConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class VungleRtbInterstitialRenderer implements MediationInterstitialAd, LoadAdCallback,
    PlayAdCallback {

  private static final String TAG = VungleRtbInterstitialRenderer.class.getSimpleName();

  private String mPlacement;
  private AdConfig mAdConfig;
  private String mAdMarkup;
  private final Handler mHandler = new Handler(Looper.getMainLooper());

  private static final HashMap<String, WeakReference<VungleRtbInterstitialRenderer>> mPlacementsInUse =
      new HashMap<>();

  private final MediationInterstitialAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
      adLoadCallback;
  private MediationInterstitialAdCallback mInterstitialAdCallback;

  public VungleRtbInterstitialRenderer(MediationInterstitialAdConfiguration adConfiguration,
      MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
          callback) {
    this.adConfiguration = adConfiguration;
    this.adLoadCallback = callback;
  }

  public void render() {
    Bundle mediationExtras = adConfiguration.getMediationExtras();
    Bundle serverParameters = adConfiguration.getServerParameters();

    AdapterParametersParser.Config config;
    try {
      config = AdapterParametersParser.parse(mediationExtras, serverParameters);
    } catch (IllegalArgumentException e) {
      String message = "Failed to load ad from Vungle: " + e.getMessage();
      Log.w(TAG, message);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, message,
          VungleRtbBannerRenderer.class.getName());
      if (adLoadCallback != null) {
        adLoadCallback.onFailure(error);
      }
      return;
    }

    mPlacement = VungleManager.getInstance().findPlacement(mediationExtras, serverParameters);
    if (TextUtils.isEmpty(mPlacement)) {
      String logMessage = "Failed to load ad from Vungle: Missing or invalid Placement ID.";
      Log.w(TAG, logMessage);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, logMessage,
          VungleRtbInterstitialRenderer.this.getClass().getName());
      adLoadCallback.onFailure(error);
      return;
    }

    if (mPlacementsInUse.containsKey(mPlacement)
        && mPlacementsInUse.get(mPlacement).get() != null) {
      String logMessage = "Only a maximum of one ad can be loaded per placement.";
      Log.w(TAG, logMessage);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, logMessage,
          VungleRtbInterstitialRenderer.this.getClass().getName());
      adLoadCallback.onFailure(error);
      return;
    }

    mAdMarkup = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(mAdMarkup)) {
      mAdMarkup = null;
    }

    // Unmute full-screen ads by default.
    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);
    VungleInitializer.getInstance()
        .initialize(
            config.getAppId(),
            adConfiguration.getContext(),
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                mPlacementsInUse
                    .put(mPlacement, new WeakReference<>(VungleRtbInterstitialRenderer.this));

                if (Vungle.canPlayAd(mPlacement)) {
                  mInterstitialAdCallback =
                      adLoadCallback.onSuccess(VungleRtbInterstitialRenderer.this);
                  return;
                }

                Vungle.loadAd(mPlacement, mAdMarkup, mAdConfig, VungleRtbInterstitialRenderer.this);
              }

              @Override
              public void onInitializeError(String errorMessage) {
                Log.w(TAG, "Failed to load ad from Vungle: " + errorMessage);
                if (adLoadCallback != null) {
                  AdError error = new AdError(AdRequest.ERROR_CODE_INTERNAL_ERROR,
                      "" + errorMessage,
                      VungleRtbInterstitialRenderer.this.getClass().getName());
                  adLoadCallback.onFailure(error);
                }
                mPlacementsInUse.remove(mPlacement);
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    if (Vungle.canPlayAd(mPlacement)) {
      Vungle.playAd(mPlacement, mAdMarkup, mAdConfig, this);
    } else {
      if (mInterstitialAdCallback != null) {
        AdError error = new AdError(AdRequest.ERROR_CODE_NO_FILL, "Not ready.",
            VungleRtbInterstitialRenderer.this.getClass().getName());
        mInterstitialAdCallback.onAdFailedToShow(error);
      }
      mPlacementsInUse.remove(mPlacement);
    }
  }

  /**
   * {@link LoadAdCallback} implementation from Vungle
   */
  @Override
  public void onAdLoad(final String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (adLoadCallback != null) {
              mInterstitialAdCallback =
                  adLoadCallback.onSuccess(VungleRtbInterstitialRenderer.this);
            }
            mPlacementsInUse
                .put(mPlacement, new WeakReference<>(VungleRtbInterstitialRenderer.this));
          }
        });
  }

  @Override
  public void creativeId(String creativeId) {
    // no-op
  }

  /**
   * {@link PlayAdCallback} implementation from Vungle
   */
  @Override
  public void onAdStart(String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mInterstitialAdCallback != null) {
              mInterstitialAdCallback.onAdOpened();
            }
          }
        });
  }

  @Override
  @Deprecated
  public void onAdEnd(String placementId, boolean completed, boolean isCTAClicked) {
  }

  @Override
  public void onAdEnd(final String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mInterstitialAdCallback != null) {
              mInterstitialAdCallback.onAdClosed();
            }
            mPlacementsInUse.remove(placementId);
          }
        });
  }

  @Override
  public void onAdClick(String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mInterstitialAdCallback != null) {
              mInterstitialAdCallback.reportAdClicked();
            }
          }
        });
  }

  @Override
  public void onAdRewarded(String placementId) {
    // no-op
  }

  @Override
  public void onAdLeftApplication(String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mInterstitialAdCallback != null) {
              mInterstitialAdCallback.onAdLeftApplication();
            }
          }
        });
  }

  @Override
  public void onError(final String placementId, final VungleException throwable) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            AdError error = new AdError(throwable.getExceptionCode(),
                "" + throwable.getLocalizedMessage(),
                VungleRtbInterstitialRenderer.this.getClass().getName());
            if (adLoadCallback != null) {
              Log.w(TAG, "Failed to load ad from Vungle", throwable);
              adLoadCallback.onFailure(error);
            }

            if (mInterstitialAdCallback != null) {
              mInterstitialAdCallback.onAdFailedToShow(error);
            }
            mPlacementsInUse.remove(placementId);
          }
        });
  }

  @Override
  public void onAdViewed(final String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mInterstitialAdCallback != null) {
              mInterstitialAdCallback.reportAdImpression();
            }
          }
        });
  }
}

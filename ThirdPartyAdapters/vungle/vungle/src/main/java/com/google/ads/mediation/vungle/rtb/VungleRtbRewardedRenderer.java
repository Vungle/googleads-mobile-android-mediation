package com.google.ads.mediation.vungle.rtb;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
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

public class VungleRtbRewardedRenderer implements MediationRewardedAd, LoadAdCallback,
    PlayAdCallback {

  private static final String TAG = VungleRtbRewardedRenderer.class.getSimpleName();

  private AdConfig mAdConfig;
  private String mPlacement;
  private String mAdMarkup;
  private final Handler mHandler = new Handler(Looper.getMainLooper());

  private static final HashMap<String, WeakReference<VungleRtbRewardedRenderer>> mPlacementsInUse =
      new HashMap<>();

  private final MediationRewardedAdConfiguration mAdConfiguration;
  private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;
  private MediationRewardedAdCallback mRewardedAdCallback;

  public VungleRtbRewardedRenderer(MediationRewardedAdConfiguration adConfiguration,
      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          callback) {
    this.mAdConfiguration = adConfiguration;
    this.mAdLoadCallback = callback;
  }

  public void render() {
    Bundle mediationExtras = mAdConfiguration.getMediationExtras();
    Bundle serverParameters = mAdConfiguration.getServerParameters();

    final AdapterParametersParser.Config config;
    try {
      config = AdapterParametersParser.parse(mediationExtras, serverParameters);
    } catch (IllegalArgumentException e) {
      String message = "Failed to load ad from Vungle: " + e.getMessage();
      Log.w(TAG, message);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, message,
          VungleRtbBannerRenderer.class.getName());
      if (mAdLoadCallback != null) {
        mAdLoadCallback.onFailure(error);
      }
      return;
    }

    mPlacement = VungleManager.getInstance().findPlacement(mediationExtras, serverParameters);
    if (TextUtils.isEmpty(mPlacement)) {
      String logMessage = "Failed to load ad from Vungle: Missing or invalid Placement ID.";
      Log.w(TAG, logMessage);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, logMessage,
          VungleRtbRewardedRenderer.this.getClass().getName());
      mAdLoadCallback.onFailure(error);
      return;
    }

    if (mPlacementsInUse.containsKey(mPlacement)
        && mPlacementsInUse.get(mPlacement).get() != null) {
      String logMessage = "Only a maximum of one ad can be loaded per placement.";
      Log.w(TAG, logMessage);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, logMessage,
          VungleRtbRewardedRenderer.this.getClass().getName());
      mAdLoadCallback.onFailure(error);
      return;
    }

    mAdMarkup = mAdConfiguration.getBidResponse();
    if (TextUtils.isEmpty(mAdMarkup)) {
      mAdMarkup = null;
    }

    // Unmute full-screen ads by default.
    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);
    VungleInitializer.getInstance()
        .initialize(
            config.getAppId(),
            mAdConfiguration.getContext(),
            new VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                Vungle.setIncentivizedFields(config.getUserId(), null, null, null, null);
                mPlacementsInUse
                    .put(mPlacement, new WeakReference<>(VungleRtbRewardedRenderer.this));

                if (Vungle.canPlayAd(mPlacement)) {
                  mRewardedAdCallback =
                      mAdLoadCallback.onSuccess(VungleRtbRewardedRenderer.this);
                  return;
                }

                Vungle.loadAd(mPlacement, mAdMarkup, mAdConfig, VungleRtbRewardedRenderer.this);
              }

              @Override
              public void onInitializeError(String errorMessage) {
                Log.w(TAG, errorMessage);
                if (mAdLoadCallback != null) {
                  AdError error = new AdError(AdRequest.ERROR_CODE_INTERNAL_ERROR,
                      "" + errorMessage,
                      VungleRtbRewardedRenderer.this.getClass().getName());
                  mAdLoadCallback.onFailure(error);
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
      if (mRewardedAdCallback != null) {
        AdError error = new AdError(AdRequest.ERROR_CODE_NO_FILL, "Not ready.",
            VungleRtbRewardedRenderer.this.getClass().getName());
        mRewardedAdCallback.onAdFailedToShow(error);
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
            if (mAdLoadCallback != null) {
              mRewardedAdCallback =
                  mAdLoadCallback.onSuccess(VungleRtbRewardedRenderer.this);
            }
            mPlacementsInUse.put(mPlacement, new WeakReference<>(VungleRtbRewardedRenderer.this));
          }
        });
  }

  @Override
  public void creativeId(String creativeId) {
    // no-op
  }

  /**
   * {@link PlayAdCallback} implemenatation from Vungle
   */
  @Override
  public void onAdStart(final String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mRewardedAdCallback != null) {
              mRewardedAdCallback.onAdOpened();
            }
          }
        });
  }

  @Override
  @Deprecated
  public void onAdEnd(
      final String placementId,
      final boolean wasSuccessfulView,
      final boolean wasCallToActionClicked) {
  }

  @Override
  public void onAdEnd(final String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mRewardedAdCallback != null) {
              mRewardedAdCallback.onAdClosed();
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
            if (mRewardedAdCallback != null) {
              mRewardedAdCallback.reportAdClicked();
            }
          }
        });
  }

  @Override
  public void onAdRewarded(String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mRewardedAdCallback != null) {
              mRewardedAdCallback.onVideoComplete();
              mRewardedAdCallback
                  .onUserEarnedReward(new VungleReward("vungle", 1));
            }
          }
        });
  }

  @Override
  public void onAdLeftApplication(String placementId) {
    // no op
  }

  // Vungle's LoadAdCallback and PlayAdCallback shares the same onError() call; when an
  // ad request to Vungle fails, and when an ad fails to play.
  @Override
  public void onError(final String placementId, final VungleException throwable) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            AdError error = new AdError(throwable.getExceptionCode(),
                "" + throwable.getLocalizedMessage(),
                VungleRtbRewardedRenderer.this.getClass().getName());
            if (mAdLoadCallback != null) {
              Log.w(TAG, "Failed to load ad from Vungle", throwable);
              mAdLoadCallback.onFailure(error);
            }

            if (mRewardedAdCallback != null) {
              mRewardedAdCallback.onAdFailedToShow(error);
            }
            mPlacementsInUse.remove(placementId);
          }
        });
  }

  @Override
  public void onAdViewed(String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mRewardedAdCallback != null) {
              mRewardedAdCallback.onVideoStart();
              mRewardedAdCallback.reportAdImpression();
            }
          }
        });
  }

  /**
   * This class is used to map Vungle rewarded video ad rewards to Google Mobile Ads SDK rewards.
   */
  private static class VungleReward implements RewardItem {

    private final String mType;
    private final int mAmount;

    VungleReward(String type, int amount) {
      mType = type;
      mAmount = amount;
    }

    @Override
    public int getAmount() {
      return mAmount;
    }

    @Override
    @NonNull
    public String getType() {
      return mType;
    }
  }
}

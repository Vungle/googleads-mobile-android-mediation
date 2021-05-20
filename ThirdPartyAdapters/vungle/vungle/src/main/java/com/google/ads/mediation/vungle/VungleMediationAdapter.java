package com.google.ads.mediation.vungle;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.VungleManager;
import com.vungle.warren.AdConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;
import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Mediation network adapter for Vungle.
 */
public class VungleMediationAdapter extends VungleBaseAdapter
    implements MediationRewardedAd, LoadAdCallback, PlayAdCallback {

  private static final String TAG = VungleMediationAdapter.class.getSimpleName();

  private AdConfig mAdConfig;
  private String mUserID;
  private String mPlacement;
  private final Handler mHandler = new Handler(Looper.getMainLooper());

  private static final HashMap<String, WeakReference<VungleMediationAdapter>> mPlacementsInUse =
      new HashMap<>();

  private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
      mMediationAdLoadCallback;
  private MediationRewardedAdCallback mMediationRewardedAdCallback;

  @Override
  public void loadRewardedAd(
      @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    mMediationAdLoadCallback = mediationAdLoadCallback;

    Bundle mediationExtras = mediationRewardedAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

    if (mediationExtras != null) {
      mUserID = mediationExtras.getString(VungleExtrasBuilder.EXTRA_USER_ID);
    }

    mPlacement = VungleManager.getInstance().findPlacement(mediationExtras, serverParameters);
    if (TextUtils.isEmpty(mPlacement)) {
      String logMessage = "Failed to load ad from Vungle: Missing or invalid Placement ID.";
      Log.w(TAG, logMessage);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, logMessage,
          VungleMediationAdapter.this.getClass().getName());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    if (mPlacementsInUse.containsKey(mPlacement)
        && mPlacementsInUse.get(mPlacement).get() != null) {
      String logMessage = "Only a maximum of one ad can be loaded per placement.";
      Log.w(TAG, logMessage);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, logMessage,
          VungleMediationAdapter.this.getClass().getName());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    String appID = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(appID)) {
      String logMessage = "Failed to load ad from Vungle: Missing or Invalid App ID.";
      Log.w(TAG, logMessage);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, logMessage,
          VungleMediationAdapter.this.getClass().getName());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    // Unmute full-screen ads by default.
    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);
    VungleInitializer.getInstance()
        .initialize(
            appID,
            mediationRewardedAdConfiguration.getContext(),
            new VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                Vungle.setIncentivizedFields(mUserID, null, null, null, null);
                mPlacementsInUse.put(mPlacement, new WeakReference<>(VungleMediationAdapter.this));

                if (Vungle.canPlayAd(mPlacement)) {
                  mMediationRewardedAdCallback =
                      mMediationAdLoadCallback.onSuccess(VungleMediationAdapter.this);
                  return;
                }

                Vungle.loadAd(mPlacement, VungleMediationAdapter.this);
              }

              @Override
              public void onInitializeError(String errorMessage) {
                Log.w(TAG, errorMessage);
                AdError error = new AdError(AdRequest.ERROR_CODE_INTERNAL_ERROR,
                    "" + errorMessage,
                    VungleMediationAdapter.this.getClass().getName());
                mMediationAdLoadCallback.onFailure(error);
                mPlacementsInUse.remove(mPlacement);
              }
            });
  }

  @Override
  public void showAd(@NonNull Context context) {
    if (Vungle.canPlayAd(mPlacement)) {
      Vungle.playAd(mPlacement, mAdConfig, VungleMediationAdapter.this);
    } else {
      if (mMediationRewardedAdCallback != null) {
        AdError error = new AdError(AdRequest.ERROR_CODE_NO_FILL, "Not ready.",
            VungleMediationAdapter.this.getClass().getName());
        mMediationRewardedAdCallback.onAdFailedToShow(error);
      }
      mPlacementsInUse.remove(mPlacement);
    }
  }

  /**
   * {@link LoadAdCallback} implemenatation from Vungle.
   */
  @Override
  public void onAdLoad(final String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationAdLoadCallback != null) {
              mMediationRewardedAdCallback =
                  mMediationAdLoadCallback.onSuccess(VungleMediationAdapter.this);
            }
            mPlacementsInUse.put(mPlacement, new WeakReference<>(VungleMediationAdapter.this));
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
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onAdOpened();
            }
          }
        });
  }

  @Override
  @Deprecated
  public void onAdEnd(final String placementId, final boolean wasSuccessfulView,
      final boolean wasCallToActionClicked) {
  }

  @Override
  public void onAdEnd(final String placementId) {
    mHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onAdClosed();
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
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.reportAdClicked();
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
            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onVideoComplete();
              mMediationRewardedAdCallback.onUserEarnedReward(new VungleReward("vungle", 1));
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
                VungleMediationAdapter.this.getClass().getName());
            if (mMediationAdLoadCallback != null) {
              Log.w(TAG, "Failed to load ad from Vungle", throwable);
              mMediationAdLoadCallback.onFailure(error);
            }

            if (mMediationRewardedAdCallback != null) {
              mMediationRewardedAdCallback.onAdFailedToShow(error);
            }
            mPlacementsInUse.remove(placementId);
          }
        });
  }

  @Override
  public void onAdViewed(String placementId) {
    mMediationRewardedAdCallback.onVideoStart();
    mMediationRewardedAdCallback.reportAdImpression();
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

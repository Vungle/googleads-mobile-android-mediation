package com.vungle.mediation;

import static com.vungle.warren.AdConfig.AdSize.BANNER;
import static com.vungle.warren.AdConfig.AdSize.BANNER_LEADERBOARD;
import static com.vungle.warren.AdConfig.AdSize.BANNER_SHORT;
import static com.vungle.warren.AdConfig.AdSize.VUNGLE_MREC;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.vungle.warren.AdConfig;
import com.vungle.warren.Banners;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.VungleNativeAd;
import com.vungle.warren.error.VungleException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * A helper class used to load and show Vungle banner ads using Google Mobile Ads SDK mediation.
 */
class VungleBannerAdapter {

  private static final String TAG = VungleBannerAdapter.class.getSimpleName();

  /**
   * Context of the Ad Request.
   */
  private Context context;

  /**
   * Listener to forward ad events to the Google Mobile Ads SDK.
   */
  private WeakReference<MediationBannerListener> bannerListener;

  /**
   * Google Mobile Ads Mediation Banner adapter.
   */
  private WeakReference<MediationBannerAdapter> adapter;

  /**
   * Vungle banner placement ID.
   */
  private String mPlacementId;

  /**
   * Unique Vungle banner request ID.
   */
  @Nullable
  private String mUniquePubRequestId;
  /**
   * Ad container for Vungle's banner ad.
   */
  private WeakReference<RelativeLayout> adLayout;

  /**
   * Vungle ad configuration settings.
   */
  @NonNull
  private AdConfig mAdConfig;

  /**
   * Vungle ad object for non-MREC banner ads.
   */
  @Nullable
  private VungleBanner mVungleBannerAd;

  /**
   * Vungle ad object for MREC banner ads.
   */
  @Nullable
  private VungleNativeAd mVungleNativeAd;

  /**
   * Indicates whether a Vungle banner ad request is in progress.
   */
  private boolean mPendingRequestBanner = false;

  /**
   * Indicates the Vungle banner ad's visibility.
   */
  private boolean mVisibility = true;

  VungleBannerAdapter(@NonNull Context context, @NonNull MediationBannerAdapter adapter,
      @NonNull MediationBannerListener mediationBannerListener, @NonNull RelativeLayout layout) {
    this.context = context.getApplicationContext();
    this.bannerListener = new WeakReference<>(mediationBannerListener);
    this.adapter = new WeakReference<>(adapter);
    this.adLayout = new WeakReference<>(layout);
  }

  void requestBannerAd(@NonNull AdSize adSize, @NonNull MediationAdRequest mediationAdRequest,
      @NonNull Bundle serverParameters, @Nullable Bundle mediationExtras) {
    Log.d(TAG, "requestBannerAd");

    AdapterParametersParser.Config config;
    try {
      config = AdapterParametersParser.parse(mediationExtras, serverParameters);
    } catch (IllegalArgumentException exception) {
      Log.w(TAG, "Failed to load ad from Vungle.", exception);
      MediationBannerListener listener = bannerListener.get();
      if (listener != null && adapter.get() != null) {
        listener.onAdFailedToLoad(adapter.get(), AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    mPlacementId = VungleManager.getInstance().findPlacement(mediationExtras, serverParameters);
    Log.d(TAG,
        "requestBannerAd for Placement: "
            + mPlacementId
            + " ###  Adapter instance: "
            + this.hashCode());

    if (TextUtils.isEmpty(mPlacementId)) {
      String message = "Failed to load ad from Vungle: Missing or Invalid Placement ID.";
      Log.w(TAG, message);
      MediationBannerListener listener = bannerListener.get();
      if (listener != null && adapter.get() != null) {
        listener.onAdFailedToLoad(adapter.get(), AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    AdConfig.AdSize vungleAdSize = getSupportedAdSize(context, adSize);
    if (vungleAdSize == null) {
      String message = "Failed to load ad from Vungle: Invalid banner size.";
      Log.w(TAG, message);
      MediationBannerListener listener = bannerListener.get();
      if (listener != null && adapter.get() != null) {
        listener.onAdFailedToLoad(adapter.get(), AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }
    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, true);
    mAdConfig.setAdSize(vungleAdSize);

    mUniquePubRequestId = config.getRequestUniqueId();
    if (VungleManager.getInstance().isBannerAdActive(mPlacementId, mUniquePubRequestId)) {
      // Adapter does not support multiple Banner instances playing for same placement except for
      // Refresh
      MediationBannerListener listener = bannerListener.get();
      if (listener != null && adapter.get() != null) {
        listener.onAdFailedToLoad(adapter.get(), AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    VungleManager.getInstance().storeActiveBannerAd(mPlacementId, VungleBannerAdapter.this, false);
    Log.d(TAG, "Requesting banner with ad size: " + mAdConfig.getAdSize());
    mPendingRequestBanner = true;
    VungleInitializer.getInstance().initialize(config.getAppId(), context.getApplicationContext(),
        new VungleInitializer.VungleInitializationListener() {
          @Override
          public void onInitializeSuccess() {
            loadBanner();
          }

          @Override
          public void onInitializeError(String errorMessage) {
            Log.d(TAG, "SDK init failed: " + VungleBannerAdapter.this);
            VungleManager.getInstance().removeActiveBannerAd(mPlacementId);
            MediationBannerListener listener = bannerListener.get();
            if (mPendingRequestBanner && listener != null && adapter.get() != null) {
              listener.onAdFailedToLoad(adapter.get(), AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
          }
        });
  }

  void onDestroy() {
    Log.d(TAG, "onDestroy: " + hashCode());
    VungleManager.getInstance().destroyBannerAd(this);
  }

  void onPause() {
    Log.d(TAG, "onPause");
    updateVisibility(false);
  }

  void onResume() {
    Log.d(TAG, "onResume");
    updateVisibility(true);
  }

  @NonNull
  String getPlacementId() {
    return mPlacementId;
  }

  @Nullable
  String getUniquePubRequestId() {
    return mUniquePubRequestId;
  }

  boolean isActive() {
    return adapter.get() != null;
  }

  void destroy() {
    Log.d(TAG, "Vungle banner adapter destroy:" + this);
    mVisibility = false;
    VungleManager.getInstance().removeActiveBannerAd(mPlacementId);
    cleanUp();
    mPendingRequestBanner = false;
  }

  void cleanUp() {
    Log.d(TAG, "Vungle banner adapter try to cleanUp:" + this);

    if (mVungleBannerAd != null) {
      Log.d(TAG, "Vungle banner adapter cleanUp: destroyAd # " + mVungleBannerAd.hashCode());
      mVungleBannerAd.destroyAd();
      detach();
      mVungleBannerAd = null;
    }

    if (mVungleNativeAd != null) {
      Log.d(TAG,
          "Vungle banner adapter cleanUp: finishDisplayingAd # " + mVungleNativeAd.hashCode());
      mVungleNativeAd.finishDisplayingAd();
      detach();
      mVungleNativeAd = null;
    }
  }

  private void preCache() {
    if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
      Banners.loadBanner(mPlacementId, mAdConfig.getAdSize(), null);
    } else {
      Vungle.loadAd(mPlacementId, null);
    }
  }

  private void updateVisibility(boolean visible) {
    this.mVisibility = visible;
    if (mVungleBannerAd != null) {
      mVungleBannerAd.setAdVisibility(visible);
    }
    if (mVungleNativeAd != null) {
      mVungleNativeAd.setAdVisibility(visible);
    }
  }

  private void loadBanner() {
    Log.d(TAG, "loadBanner:" + this);
    if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
      Banners.loadBanner(mPlacementId, mAdConfig.getAdSize(), mAdLoadCallback);
    } else {
      Vungle.loadAd(mPlacementId, mAdLoadCallback);
    }
  }

  private LoadAdCallback mAdLoadCallback =
      new LoadAdCallback() {
        @Override
        public void onAdLoad(String id) {
          createBanner();
        }

        @Override
        public void onError(String id, VungleException exception) {
          Log.d(TAG, "Ad load failed:" + VungleBannerAdapter.this);
          VungleManager.getInstance().removeActiveBannerAd(mPlacementId);
          MediationBannerListener listener = bannerListener.get();
          if (mPendingRequestBanner && listener != null && adapter.get() != null) {
            listener.onAdFailedToLoad(adapter.get(), AdRequest.ERROR_CODE_NO_FILL);
          }
        }
      };

  private PlayAdCallback mAdPlayCallback =
      new PlayAdCallback() {
        @Override
        public void onAdStart(String placementId) {
          // let's load it again to mimic auto-cache, don't care about errors
          if (mPendingRequestBanner) {
            preCache();
          }
        }

        @Override
        @Deprecated
        public void onAdEnd(String placementId, boolean completed, boolean isCTAClicked) {
        }

        @Override
        public void onAdEnd(String placementId) {
          MediationBannerListener listener = bannerListener.get();
          if (mPendingRequestBanner && listener != null && adapter.get() != null) {
            listener.onAdClosed(adapter.get());
          }
        }

        @Override
        public void onAdClick(String placementId) {
          MediationBannerListener listener = bannerListener.get();
          if (mPendingRequestBanner && listener != null && adapter.get() != null) {
            listener.onAdClicked(adapter.get());
            listener.onAdOpened(adapter.get());
          }
        }

        @Override
        public void onAdRewarded(String placementId) {
        }

        @Override
        public void onAdLeftApplication(String placementId) {
          MediationBannerListener listener = bannerListener.get();
          if (mPendingRequestBanner && listener != null && adapter.get() != null) {
            listener.onAdLeftApplication(adapter.get());
          }
        }

        @Override
        public void onError(String placementId, VungleException exception) {
          Log.w(TAG,
              "Ad playback error Placement: " + placementId + ";" + VungleBannerAdapter.this);
          VungleManager.getInstance().removeActiveBannerAd(mPlacementId);
        }
      };

  private void createBanner() {
    Log.d(TAG, "create banner:" + this);
    if (!mPendingRequestBanner) {
      return;
    }

    VungleManager.getInstance().cleanUpActiveBannerAd(mPlacementId);
    RelativeLayout.LayoutParams adParams =
        new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    adParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
    adParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
    MediationBannerListener listener = bannerListener.get();

    if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
      mVungleBannerAd = Banners.getBanner(mPlacementId, mAdConfig.getAdSize(), mAdPlayCallback);
      if (mVungleBannerAd != null) {
        Log.d(TAG, "display banner:" + mVungleBannerAd.hashCode() + this);
        VungleManager.getInstance().storeActiveBannerAd(mPlacementId, this, true);
        updateVisibility(mVisibility);
        mVungleBannerAd.setLayoutParams(adParams);
        // don't add to parent here
        if (listener != null && adapter.get() != null) {
          listener.onAdLoaded(adapter.get());
        }
      } else {
        // missing resources
        if (listener != null && adapter.get() != null) {
          listener.onAdFailedToLoad(adapter.get(), AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
      }
    } else {
      View adView = null;
      mVungleNativeAd = Vungle.getNativeAd(mPlacementId, mAdConfig, mAdPlayCallback);
      if (mVungleNativeAd != null) {
        adView = mVungleNativeAd.renderNativeView();
        VungleManager.getInstance().storeActiveBannerAd(mPlacementId, this, true);
      }
      if (adView != null) {
        Log.d(TAG, "display MREC:" + mVungleNativeAd.hashCode() + this);
        updateVisibility(mVisibility);
        adView.setLayoutParams(adParams);
        // don't add to parent here
        if (listener != null && adapter.get() != null) {
          listener.onAdLoaded(adapter.get());
        }
      } else {
        // missing resources
        if (listener != null && adapter.get() != null) {
          listener.onAdFailedToLoad(adapter.get(), AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
      }
    }
  }

  @NonNull
  @Override
  public String toString() {
    return " [placementId="
        + mPlacementId
        + " # uniqueRequestId="
        + mUniquePubRequestId
        + " # hashcode="
        + hashCode()
        + "] ";
  }

  void attach() {
    RelativeLayout adLayout = this.adLayout.get();
    if (adLayout != null) {
      if (mVungleBannerAd != null && mVungleBannerAd.getParent() == null) {
        adLayout.addView(mVungleBannerAd);
      }
      if (mVungleNativeAd != null) {
        View adView = mVungleNativeAd.renderNativeView();
        if (adView != null && adView.getParent() == null) {
          adLayout.addView(adView);
        }
      }
    }
  }

  void detach() {
    if (mVungleBannerAd != null && mVungleBannerAd.getParent() != null) {
      ((ViewGroup) mVungleBannerAd.getParent()).removeView(mVungleBannerAd);
    }
    if (mVungleNativeAd != null) {
      View adView = mVungleNativeAd.renderNativeView();
      if (adView != null && adView.getParent() != null) {
        ((ViewGroup) adView.getParent()).removeView(adView);
      }
    }
  }

  @Nullable
  private AdConfig.AdSize getSupportedAdSize(Context context, AdSize adSize) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(new AdSize(BANNER_SHORT.getWidth(), BANNER_SHORT.getHeight()));
    potentials.add(new AdSize(BANNER.getWidth(), BANNER.getHeight()));
    potentials.add(new AdSize(BANNER_LEADERBOARD.getWidth(), BANNER_LEADERBOARD.getHeight()));
    potentials.add(new AdSize(VUNGLE_MREC.getWidth(), VUNGLE_MREC.getHeight()));

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize == null) {
      return null;
    }

    if (closestSize.getWidth() == BANNER_SHORT.getWidth()
        && closestSize.getHeight() == BANNER_SHORT.getHeight()) {
      return BANNER_SHORT;
    } else if (closestSize.getWidth() == BANNER.getWidth()
        && closestSize.getHeight() == BANNER.getHeight()) {
      return BANNER;
    } else if (closestSize.getWidth() == BANNER_LEADERBOARD.getWidth()
        && closestSize.getHeight() == BANNER_LEADERBOARD.getHeight()) {
      return BANNER_LEADERBOARD;
    } else if (closestSize.getWidth() == VUNGLE_MREC.getWidth()
        && closestSize.getHeight() == VUNGLE_MREC.getHeight()) {
      return VUNGLE_MREC;
    }
    return null;
  }
}

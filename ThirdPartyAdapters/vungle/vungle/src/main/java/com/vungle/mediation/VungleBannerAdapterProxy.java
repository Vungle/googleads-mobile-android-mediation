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
import android.widget.RelativeLayout;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.vungle.warren.AdConfig;
import java.util.ArrayList;

/**
 * A helper class used to delegate AdMob banner request to Vungle Banners.
 */
class VungleBannerAdapterProxy {

  private static final String TAG = VungleBannerAdapterProxy.class.getSimpleName();

  private Context context;
  private volatile RelativeLayout adLayout;
  private MediationBannerListener mediationBannerListener;
  private VungleBannerAdapter mBannerAdapter;
  private MediationBannerAdapter adapter;

  VungleBannerAdapterProxy(Context context, MediationBannerAdapter adapter,
      MediationBannerListener mediationBannerListener) {
    this.context = context;
    this.adapter = adapter;
    this.mediationBannerListener = mediationBannerListener;
  }

  void requestBannerAd(
      AdSize adSize,
      MediationAdRequest mediationAdRequest,
      Bundle serverParameters,
      Bundle mediationExtras) {

    AdapterParametersParser.Config config;
    try {
      config = AdapterParametersParser.parse(mediationExtras, serverParameters);
    } catch (IllegalArgumentException e) {
      Log.w(TAG, "Failed to load ad from Vungle.", e);
      if (mediationBannerListener != null) {
        mediationBannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    VungleManager mVungleManager = VungleManager.getInstance();

    String placementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
    Log.d(
        TAG,
        "requestBannerAd for Placement: "
            + placementForPlay
            + " ###  Adapter instance: "
            + this.hashCode());

    if (TextUtils.isEmpty(placementForPlay)) {
      String message = "Failed to load ad from Vungle: Missing or Invalid Placement ID.";
      Log.w(TAG, message);
      mediationBannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }

    AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, true);
    if (!hasBannerSizeAd(context, adSize, adConfig)) {
      String message = "Failed to load ad from Vungle: Invalid banner size.";
      Log.w(TAG, message);
      mediationBannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }

    // Create the adLayout wrapper with the requested ad size, as Vungle's ad uses MATCH_PARENT for
    // its dimensions.
    adLayout =
        new RelativeLayout(context) {
          @Override
          protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (mBannerAdapter != null) {
              mBannerAdapter.attach();
            }
          }

          @Override
          protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (mBannerAdapter != null) {
              mBannerAdapter.detach();
            }
          }
        };
    int adLayoutHeight = adSize.getHeightInPixels(context);
    // If the height is 0 (e.g. for inline adaptive banner requests), use the closest supported size
    // as the height of the adLayout wrapper.
    if (adLayoutHeight <= 0) {
      float density = context.getResources().getDisplayMetrics().density;
      adLayoutHeight = Math.round(adConfig.getAdSize().getHeight() * density);
    }
    RelativeLayout.LayoutParams adViewLayoutParams =
        new RelativeLayout.LayoutParams(adSize.getWidthInPixels(context), adLayoutHeight);
    adLayout.setLayoutParams(adViewLayoutParams);

    mBannerAdapter =
        mVungleManager.getBannerAdapter(placementForPlay, config.getRequestUniqueId(), adConfig);
    if (mBannerAdapter == null) {
      // Adapter does not support multiple Banner instances playing for same placement except for
      // Refresh
      mediationBannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }

    mBannerAdapter.setAdLayout(adLayout);
    mBannerAdapter.setVungleListener(mVungleBannerListener);

    Log.d(TAG, "Requesting banner with ad size: " + adConfig.getAdSize());
    mBannerAdapter.requestBannerAd(context, config.getAppId());
  }

  void onDestroy() {
    if (mBannerAdapter != null) {
      mBannerAdapter.destroy(adLayout);
      mBannerAdapter = null;
    }
    adLayout = null;
  }

  void onPause() {
    if (mBannerAdapter != null) {
      mBannerAdapter.updateVisibility(false);
    }
  }

  void onResume() {
    if (mBannerAdapter != null) {
      mBannerAdapter.updateVisibility(true);
    }
  }

  private VungleListener mVungleBannerListener =
      new VungleListener() {
        @Override
        void onAdClick(String placementId) {
          if (mediationBannerListener != null) {
            mediationBannerListener.onAdClicked(adapter);
            mediationBannerListener.onAdOpened(adapter);
          }
        }

        @Override
        void onAdEnd(String placementId) {
          if (mediationBannerListener != null) {
            mediationBannerListener.onAdClosed(adapter);
          }
        }

        @Override
        void onAdLeftApplication(String placementId) {
          if (mediationBannerListener != null) {
            mediationBannerListener.onAdLeftApplication(adapter);
          }
        }

        @Override
        void onAdAvailable() {
          if (mediationBannerListener != null) {
            mediationBannerListener.onAdLoaded(adapter);
          }
        }

        @Override
        void onAdStart(String placement) {
          // let's load it again to mimic auto-cache, don't care about errors
          if (mBannerAdapter != null) {
            mBannerAdapter.preCache();
          }
        }

        @Override
        void onAdFail(String placement) {
          Log.w(TAG, "Ad playback error Placement: " + placement + ";" + mBannerAdapter);
        }

        @Override
        void onAdFailedToLoad(int errorCode) {
          Log.w(TAG, "Failed to load ad from Vungle: " + errorCode + ";" + mBannerAdapter);
          if (mediationBannerListener != null) {
            mediationBannerListener.onAdFailedToLoad(adapter, errorCode);
          }
        }
      };

  View getBannerView() {
    return adLayout;
  }

  private boolean hasBannerSizeAd(Context context, AdSize adSize, AdConfig adConfig) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(new AdSize(BANNER_SHORT.getWidth(), BANNER_SHORT.getHeight()));
    potentials.add(new AdSize(BANNER.getWidth(), BANNER.getHeight()));
    potentials.add(new AdSize(BANNER_LEADERBOARD.getWidth(), BANNER_LEADERBOARD.getHeight()));
    potentials.add(new AdSize(VUNGLE_MREC.getWidth(), VUNGLE_MREC.getHeight()));

    AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
    if (closestSize == null) {
      Log.i(TAG, "Not found closest ad size: " + adSize);
      return false;
    }
    Log.i(
        TAG,
        "Found closest ad size: " + closestSize.toString() + " for requested ad size: " + adSize);

    if (closestSize.getWidth() == BANNER_SHORT.getWidth()
        && closestSize.getHeight() == BANNER_SHORT.getHeight()) {
      adConfig.setAdSize(BANNER_SHORT);
    } else if (closestSize.getWidth() == BANNER.getWidth()
        && closestSize.getHeight() == BANNER.getHeight()) {
      adConfig.setAdSize(BANNER);
    } else if (closestSize.getWidth() == BANNER_LEADERBOARD.getWidth()
        && closestSize.getHeight() == BANNER_LEADERBOARD.getHeight()) {
      adConfig.setAdSize(BANNER_LEADERBOARD);
    } else if (closestSize.getWidth() == VUNGLE_MREC.getWidth()
        && closestSize.getHeight() == VUNGLE_MREC.getHeight()) {
      adConfig.setAdSize(VUNGLE_MREC);
    }

    return true;
  }
}

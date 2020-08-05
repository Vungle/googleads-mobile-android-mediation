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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.vungle.warren.AdConfig;
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
  private MediationBannerListener bannerListener;

  /**
   * Google Mobile Ads Mediation Banner adapter.
   */
  private MediationBannerAdapter adapter;

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
   * Vungle banner
   */
  private VungleBannerRequest bannerRequest;

  /**
   * Ad container for Vungle's banner ad.
   */
  private volatile RelativeLayout adLayout;

  /**
   * Vungle ad configuration settings.
   */
  @NonNull
  private AdConfig mAdConfig;

  VungleBannerAdapter(@NonNull Context context, @NonNull MediationBannerAdapter adapter,
      @NonNull MediationBannerListener mediationBannerListener) {
    this.context = context;
    this.bannerListener = mediationBannerListener;
    this.adapter = adapter;
  }

  void requestBannerAd(@NonNull AdSize adSize, @NonNull MediationAdRequest mediationAdRequest,
      @NonNull Bundle serverParameters, @Nullable Bundle mediationExtras) {
    Log.d(TAG, "requestBannerAd");

    AdapterParametersParser.Config config;
    try {
      config = AdapterParametersParser.parse(mediationExtras, serverParameters);
    } catch (IllegalArgumentException exception) {
      Log.w(TAG, "Failed to load ad from Vungle.", exception);
      if (bannerListener != null) {
        bannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
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
      if (bannerListener != null) {
        bannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    AdConfig.AdSize vungleAdSize = getSupportedAdSize(context, adSize);
    if (vungleAdSize == null) {
      String message = "Failed to load ad from Vungle: Invalid banner size.";
      Log.w(TAG, message);
      if (bannerListener != null) {
        bannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }
    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, true);
    mAdConfig.setAdSize(vungleAdSize);

    // Create the adLayout wrapper with the requested ad size, as Vungle's ad uses MATCH_PARENT for
    // its dimensions.
    adLayout =
        new RelativeLayout(context) {
          @Override
          protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (bannerRequest != null) {
              bannerRequest.attach();
            }
          }

          @Override
          protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (bannerRequest != null) {
              bannerRequest.detach();
            }
          }
        };
    int adLayoutHeight = adSize.getHeightInPixels(context);
    // If the height is 0 (e.g. for inline adaptive banner requests), use the closest supported size
    // as the height of the adLayout wrapper.
    if (adLayoutHeight <= 0) {
      float density = context.getResources().getDisplayMetrics().density;
      adLayoutHeight = Math.round(mAdConfig.getAdSize().getHeight() * density);
    }
    RelativeLayout.LayoutParams adViewLayoutParams =
        new RelativeLayout.LayoutParams(adSize.getWidthInPixels(context), adLayoutHeight);
    adLayout.setLayoutParams(adViewLayoutParams);

    mUniquePubRequestId = config.getRequestUniqueId();
    if (VungleManager.getInstance().isBannerAdActive(mPlacementId, mUniquePubRequestId)) {
      // Adapter does not support multiple Banner instances playing for same placement except for
      // Refresh
      if (bannerListener != null) {
        bannerListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INVALID_REQUEST);
      }
      return;
    }

    bannerRequest = VungleManager.getInstance().getBannerRequest(mPlacementId);
    if (bannerRequest == null) {
      bannerRequest = new VungleBannerRequest(mPlacementId, mUniquePubRequestId, mAdConfig);
      VungleManager.getInstance().storeActiveBannerAd(mPlacementId, bannerRequest);
    }

    bannerRequest.setAdLayout(adLayout);
    bannerRequest.setVungleListener(mVungleBannerListener);

    Log.d(TAG, "Requesting banner with ad size: " + mAdConfig.getAdSize());
    bannerRequest.requestBannerAd(context, config.getAppId());
  }

  void onDestroy() {
    if (bannerRequest != null) {
      bannerRequest.destroy(adLayout);
      bannerRequest = null;
    }
    adLayout = null;
  }

  void onPause() {
    if (bannerRequest != null) {
      bannerRequest.updateVisibility(false);
    }
  }

  void onResume() {
    if (bannerRequest != null) {
      bannerRequest.updateVisibility(true);
    }
  }

  private VungleListener mVungleBannerListener =
      new VungleListener() {
        @Override
        void onAdClick(String placementId) {
          if (bannerListener != null) {
            bannerListener.onAdClicked(adapter);
            bannerListener.onAdOpened(adapter);
          }
        }

        @Override
        void onAdEnd(String placementId) {
          if (bannerListener != null) {
            bannerListener.onAdClosed(adapter);
          }
        }

        @Override
        void onAdLeftApplication(String placementId) {
          if (bannerListener != null) {
            bannerListener.onAdLeftApplication(adapter);
          }
        }

        @Override
        void onAdAvailable() {
          if (bannerListener != null) {
            bannerListener.onAdLoaded(adapter);
          }
        }

        @Override
        void onAdStart(String placement) {
          // let's load it again to mimic auto-cache, don't care about errors
          if (bannerRequest != null) {
            bannerRequest.preCache();
          }
        }

        @Override
        void onAdFail(String placement) {
          Log.w(TAG, "Ad playback error Placement: " + placement + ";" + bannerRequest);
        }

        @Override
        void onAdFailedToLoad(int errorCode) {
          Log.w(TAG, "Failed to load ad from Vungle: " + errorCode + ";" + bannerRequest);
          if (bannerListener != null) {
            bannerListener.onAdFailedToLoad(adapter, errorCode);
          }
        }
      };

  View getBannerView() {
    return adLayout;
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

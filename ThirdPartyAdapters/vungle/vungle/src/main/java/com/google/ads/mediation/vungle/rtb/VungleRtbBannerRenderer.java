package com.google.ads.mediation.vungle.rtb;

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
import com.google.ads.mediation.vungle.VungleBannerAd;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.vungle.mediation.AdapterParametersParser;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.VungleManager;
import com.vungle.warren.AdConfig;
import com.vungle.warren.BannerAdConfig;
import com.vungle.warren.Banners;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.error.VungleException;
import java.util.ArrayList;

public class VungleRtbBannerRenderer implements MediationBannerAd, LoadAdCallback, PlayAdCallback {

  private static final String TAG = VungleRtbBannerRenderer.class.getSimpleName();

  private final MediationBannerAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback;
  private MediationBannerAdCallback mBannerAdCallback;

  private String placementId;
  private String uniqueRequestId;
  private AdConfig mAdConfig;
  private RelativeLayout adLayout;
  private VungleBannerAd vungleBannerAd;
  private boolean mPendingRequestBanner = false;
  private String mAdMarkup;
  private boolean mVisibility = true;

  public VungleRtbBannerRenderer(MediationBannerAdConfiguration mediationBannerAdConfiguration,
      MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
    this.adConfiguration = mediationBannerAdConfiguration;
    this.callback = mediationAdLoadCallback;
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
      if (callback != null) {
        callback.onFailure(error);
      }
      return;
    }

    placementId = VungleManager.getInstance()
        .findPlacement(mediationExtras, serverParameters);
    Log.d(TAG, "requestBannerAd for Placement: " + placementId
        + " ### Adapter instance: " + this.hashCode());

    if (TextUtils.isEmpty(placementId)) {
      String message = "Failed to load ad from Vungle: Missing or Invalid Placement ID.";
      Log.w(TAG, message);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, message,
          VungleRtbBannerRenderer.class.getName());
      if (callback != null) {
        callback.onFailure(error);
      }
      return;
    }

    mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, true);
    if (!hasBannerSizeAd(adConfiguration.getContext(), adConfiguration.getAdSize(), mAdConfig)) {
      String message = "Failed to load ad from Vungle: Invalid banner size.";
      Log.w(TAG, message);
      AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, message,
          VungleRtbBannerRenderer.class.getName());
      if (callback != null) {
        callback.onFailure(error);
      }
      return;
    }

    // Adapter does not support multiple Banner instances playing for same placement except for
    // refresh.
    uniqueRequestId = config.getRequestUniqueId();
    AdError adError = VungleManager.getInstance()
        .canRequestBannerAd(placementId, uniqueRequestId);
    if (adError != null) {
      if (callback != null) {
        callback.onFailure(adError);
      }
      return;
    }

    mAdMarkup = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(mAdMarkup)) {
      mAdMarkup = null;
    }

    // Create the adLayout wrapper with the requested ad size, as Vungle's ad uses MATCH_PARENT for
    // its dimensions.
    adLayout =
        new RelativeLayout(adConfiguration.getContext()) {
          @Override
          protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (vungleBannerAd != null) {
              vungleBannerAd.attach();
            }
          }

          @Override
          protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (vungleBannerAd != null) {
              vungleBannerAd.detach();
            }
          }
        };

    Context context = adConfiguration.getContext();
    AdSize adSize = adConfiguration.getAdSize();
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

    Log.d(TAG, "requestBannerAd: " + this);
    mPendingRequestBanner = true;

    VungleInitializer.getInstance()
        .initialize(
            config.getAppId(),
            context.getApplicationContext(),
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                Banners.loadBanner(placementId, mAdMarkup, new BannerAdConfig(mAdConfig),
                    VungleRtbBannerRenderer.this);
              }

              @Override
              public void onInitializeError(String errorMessage) {
                Log.d(TAG, "SDK init failed: " + errorMessage);
                VungleManager.getInstance().removeActiveBannerAd(placementId, vungleBannerAd);
                if (mPendingRequestBanner && callback != null) {
                  AdError error = new AdError(AdRequest.ERROR_CODE_INTERNAL_ERROR,
                      "" + errorMessage,
                      VungleRtbBannerRenderer.class.getName());
                  callback.onFailure(error);
                }
              }
            });
  }

  @NonNull
  @Override
  public View getView() {
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

  private void updateVisibility(boolean visible) {
    if (vungleBannerAd == null) {
      return;
    }

    this.mVisibility = visible;
    if (vungleBannerAd.getVungleBanner() != null) {
      vungleBannerAd.getVungleBanner().setAdVisibility(visible);
    }
  }

  private void createBanner() {
    Log.d(TAG, "create banner: " + this);
    if (!mPendingRequestBanner) {
      return;
    }

    RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    adParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
    adParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);

    vungleBannerAd = VungleManager.getInstance().getVungleBannerAd(placementId);

    if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
      VungleBanner vungleBanner = Banners
          .getBanner(placementId, mAdMarkup, new BannerAdConfig(mAdConfig), this);
      if (vungleBanner != null) {
        Log.d(TAG, "display banner:" + vungleBanner.hashCode() + this);
        if (vungleBannerAd != null) {
          vungleBannerAd.setVungleBanner(vungleBanner);
        }

        updateVisibility(mVisibility);
        vungleBanner.setLayoutParams(adParams);
        // don't add to parent here
        if (callback != null) {
          mBannerAdCallback = callback.onSuccess(this);
        }
      } else {
        // missing resources
        if (callback != null) {
          AdError error = new AdError(AdRequest.ERROR_CODE_INTERNAL_ERROR, "Play banner error",
              VungleRtbBannerRenderer.class.getName());
          callback.onFailure(error);
        }
      }
    } else {
      if (callback != null) {
        AdError error = new AdError(AdRequest.ERROR_CODE_INVALID_REQUEST, "Invalidate Banner Size",
            VungleRtbBannerRenderer.class.getName());
        callback.onFailure(error);
      }
    }
  }

  @Override
  public void onAdLoad(String placementId) {
    createBanner();
  }

  @Override
  public void creativeId(String creativeId) {
    // no-op
  }

  @Override
  public void onAdStart(String placementId) {
    if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize()) && mAdMarkup == null) {
      Banners.loadBanner(placementId, null, new BannerAdConfig(mAdConfig), null);
    }
  }

  @Override
  @Deprecated
  public void onAdEnd(String placementId, boolean completed, boolean isCTAClicked) {
  }

  @Override
  public void onAdEnd(String placementId) {
    // No-op for banner ads.
  }

  @Override
  public void onAdClick(String placementId) {
    if (mBannerAdCallback != null) {
      mBannerAdCallback.reportAdClicked();
      mBannerAdCallback.onAdOpened();
    }
  }

  @Override
  public void onAdRewarded(String placementId) {
    // No-op for banner ads.
  }

  @Override
  public void onAdLeftApplication(String placementId) {
    if (mBannerAdCallback != null) {
      mBannerAdCallback.onAdLeftApplication();
    }
  }

  @Override
  public void onError(String placementId, VungleException exception) {
    AdError error = new AdError(exception.getExceptionCode(),
        "" + exception.getLocalizedMessage(),
        VungleRtbBannerRenderer.class.getName());
    if (mPendingRequestBanner && callback != null) {
      Log.w(TAG, "Failed to load ad from Vungle", exception);
      callback.onFailure(error);
    }
    if (mBannerAdCallback != null) {
      Log.w(TAG, "Failed to play ad from Vungle", exception);
      VungleManager.getInstance().removeActiveBannerAd(placementId, vungleBannerAd);
    }
  }

  @Override
  public void onAdViewed(String placementId) {
    // No-op for banner ads.
  }

  @Override
  @NonNull
  public String toString() {
    return " [placementId="
        + placementId
        + " # uniqueRequestId="
        + uniqueRequestId
        + " # hashcode="
        + hashCode()
        + "] ";
  }
}

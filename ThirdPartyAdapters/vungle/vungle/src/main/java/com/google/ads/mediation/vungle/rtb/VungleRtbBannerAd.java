package com.google.ads.mediation.vungle.rtb;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_VUNGLE_BANNER_NULL;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_APP_ID;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

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
import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.VungleManager;
import com.vungle.warren.AdConfig;
import com.vungle.warren.BannerAdConfig;
import com.vungle.warren.Banners;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.error.VungleException;

public class VungleRtbBannerAd implements MediationBannerAd {

  private final MediationBannerAdConfiguration mediationBannerAdConfiguration;
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback;

  private MediationBannerAdCallback bannerAdCallback;

  private String placementId;
  private AdConfig adConfig;
  private String adMarkup;
  private RelativeLayout adLayout;
  @Nullable
  private VungleBanner vungleBanner;

  public VungleRtbBannerAd(@NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mediationAdLoadCallback) {
    this.mediationBannerAdConfiguration = mediationBannerAdConfiguration;
    this.mediationAdLoadCallback = mediationAdLoadCallback;
  }

  public void render() {
    Bundle mediationExtras = mediationBannerAdConfiguration.getMediationExtras();
    Bundle serverParameters = mediationBannerAdConfiguration.getServerParameters();

    String appID = serverParameters.getString(KEY_APP_ID);

    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid App ID configured for this ad source instance in the "
              + "AdMob or Ad Manager UI.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    placementId = VungleManager.getInstance().findPlacement(mediationExtras, serverParameters);
    Log.d(TAG,
        "requestBannerAd for Placement: " + placementId + " ### Adapter instance: " + this
            .hashCode());

    if (TextUtils.isEmpty(placementId)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Missing or invalid Placement ID configured for this ad source instance in the "
              + "AdMob or Ad Manager UI.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    Context context = mediationBannerAdConfiguration.getContext();
    AdSize adSize = mediationBannerAdConfiguration.getAdSize();

    adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, true);
    if (!VungleManager.getInstance().hasBannerSizeAd(context, adSize, adConfig)) {
      AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH,
          String.format("The requested banner size: %s is not supported by Vungle SDK.", adSize),
          ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      mediationAdLoadCallback.onFailure(error);
      return;
    }

    adMarkup = mediationBannerAdConfiguration.getBidResponse();
    Log.d(TAG, "Render banner mAdMarkup=" + adMarkup);

    adLayout =
        new RelativeLayout(context) {
          @Override
          protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (vungleBanner != null && vungleBanner.getParent() == null) {
              adLayout.addView(vungleBanner);
            }
          }

          @Override
          protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (vungleBanner != null && vungleBanner.getParent() != null) {
              ((ViewGroup) vungleBanner.getParent()).removeView(vungleBanner);
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

    VungleInitializer.getInstance().initialize(appID, context, new VungleInitializationListener() {
      @Override
      public void onInitializeSuccess() {
        loadBanner();
      }

      @Override
      public void onInitializeError(AdError error) {
        Log.w(TAG, error.toString());
        mediationAdLoadCallback.onFailure(error);
      }
    });
  }

  @NonNull
  @Override
  public View getView() {
    Log.d(TAG, "getBannerView # instance: " + hashCode());
    return adLayout;
  }

  private void loadBanner() {
    Log.d(TAG, "loadBanner: " + this);
    Banners.loadBanner(placementId, adMarkup, new BannerAdConfig(adConfig), adLoadCallback);
  }

  private void createBanner() {
    Log.d(TAG, "create banner: " + this);
    RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    adParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
    adParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);

    vungleBanner = Banners
        .getBanner(placementId, adMarkup, new BannerAdConfig(adConfig), playAdCallback);
    if (vungleBanner != null) {
      Log.d(TAG, "display banner:" + vungleBanner.hashCode() + this);
      vungleBanner.setLayoutParams(adParams);
      // Don't add to parent here.
      bannerAdCallback = mediationAdLoadCallback.onSuccess(this);
    } else {
      AdError error = new AdError(ERROR_VUNGLE_BANNER_NULL,
          "Vungle SDK returned a successful load callback, but Banners.getBanner() "
              + "returned null.",
          ERROR_DOMAIN);
      Log.d(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
    }
  }

  //region Vungle callbacks
  private final LoadAdCallback adLoadCallback =
      new LoadAdCallback() {
        @Override
        public void onAdLoad(String id) {
          createBanner();
        }

        @Override
        public void onError(String id, VungleException exception) {
          AdError error = VungleMediationAdapter.getAdError(exception);
          Log.w(TAG, error.toString());
          mediationAdLoadCallback.onFailure(error);
        }
      };

  private final PlayAdCallback playAdCallback = new PlayAdCallback() {
    @Override
    public void creativeId(String creativeId) {
      // no-op
    }

    @Override
    public void onAdStart(String placementId) {
      // no-op
    }

    @Override
    public void onAdEnd(String placementId, boolean completed, boolean isCTAClicked) {
      // no-op
    }

    @Override
    public void onAdEnd(String placementId) {
      // no-op
    }

    @Override
    public void onAdClick(String placementId) {
      if (bannerAdCallback != null) {
        bannerAdCallback.reportAdClicked();
        bannerAdCallback.onAdOpened();
      }
    }

    @Override
    public void onAdRewarded(String placementId) {
      // no-op
    }

    @Override
    public void onAdLeftApplication(String placementId) {
      if (bannerAdCallback != null) {
        bannerAdCallback.onAdLeftApplication();
      }
    }

    @Override
    public void onError(String placementId, VungleException exception) {
      AdError error = VungleMediationAdapter.getAdError(exception);
      Log.w(TAG, error.toString());
      mediationAdLoadCallback.onFailure(error);
    }

    @Override
    public void onAdViewed(String placementId) {
      if (bannerAdCallback != null) {
        bannerAdCallback.reportAdImpression();
      }
    }
  };

}

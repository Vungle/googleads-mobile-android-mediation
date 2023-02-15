package com.google.ads.mediation.yahoo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.yahoo.ads.ActivityStateManager;
import com.yahoo.ads.YASAds;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;

public class YahooMediationAdapter extends Adapter implements MediationBannerAdapter,
    MediationInterstitialAdapter, MediationNativeAdapter {

  public static final String TAG = YahooMediationAdapter.class.getSimpleName();

  /**
   * The pixel-to-dpi scale for images downloaded Yahoo Mobile SDK.
   */
  static final double YAS_IMAGE_SCALE = 1.0;

  /**
   * Weak reference of context.
   */
  private WeakReference<Context> contextWeakRef;

  /**
   * The Yahoo interstitial ad renderer.
   */
  private YahooInterstitialRenderer yahooInterstitialRenderer;

  /**
   * The Yahoo rewarded ad renderer.
   */
  private YahooRewardedRenderer yahooRewardedRenderer;

  /**
   * The Yahoo banner ad renderer.
   */
  private YahooBannerRenderer yahooBannerRenderer;

  /**
   * The Yahoo native ad renderer.
   */
  private YahooNativeRenderer yahooNativeRenderer;

  @NonNull
  @Override
  public VersionInfo getVersionInfo() {
    String versionString = BuildConfig.ADAPTER_VERSION;
    String[] splits = versionString.split("\\.");
    if (splits.length >= 4) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String.format(
        "Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
        versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @NonNull
  @Override
  public VersionInfo getSDKVersionInfo() {
    String versionString = YASAds.getSDKInfo().version;
    String[] splits = versionString.split("\\.");
    if (splits.length >= 3) {
      int major = Integer.parseInt(splits[0]);
      int minor = Integer.parseInt(splits[1]);
      int micro = Integer.parseInt(splits[2]);
      return new VersionInfo(major, minor, micro);
    }

    String logMessage = String.format(
        "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", versionString);
    Log.w(TAG, logMessage);
    return new VersionInfo(0, 0, 0);
  }

  @Override
  public void initialize(@NonNull Context context,
      @NonNull InitializationCompleteCallback initializationCompleteCallback,
      @NonNull List<MediationConfiguration> mediationConfigurations) {

    if (!(context instanceof Activity)) {
      initializationCompleteCallback.onInitializationFailed(
          "Yahoo Mobile SDK requires an Activity context to initialize");
      return;
    }

    HashSet<String> siteIDs = new HashSet<>();
    for (MediationConfiguration mediationConfiguration : mediationConfigurations) {
      String siteID = YahooAdapterUtils.getSiteId(mediationConfiguration.getServerParameters(),
          (Bundle) null);
      if (!TextUtils.isEmpty(siteID)) {
        siteIDs.add(siteID);
      }
    }
    int count = siteIDs.size();
    if (count <= 0) {
      String logMessage = "Initialization failed: Missing or invalid Site ID";
      Log.e(TAG, logMessage);
      initializationCompleteCallback.onInitializationFailed(logMessage);
      return;
    }
    String siteID = siteIDs.iterator().next();
    if (count > 1) {
      String message = String.format("Multiple '%s' entries found: %s. " +
              "Using '%s' to initialize Yahoo Mobile SDK.", YahooAdapterUtils.SITE_KEY, siteIDs,
          siteID);
      Log.w(TAG, message);
    }
    if (initializeSDK(context, siteID)) {
      initializationCompleteCallback.onInitializationSucceeded();
    } else {
      initializationCompleteCallback.onInitializationFailed(
          "Yahoo Mobile SDK initialization failed");
    }
  }

  @Override
  public void requestBannerAd(@NonNull final Context context,
      @NonNull final MediationBannerListener listener, @NonNull final Bundle serverParameters,
      @NonNull AdSize adSize, @NonNull final MediationAdRequest mediationAdRequest,
      @Nullable final Bundle mediationExtras) {
    yahooBannerRenderer = new YahooBannerRenderer(this);
    yahooBannerRenderer.render(context, listener, serverParameters, adSize, mediationAdRequest,
        mediationExtras);
  }

  @NonNull
  @Override
  public View getBannerView() {
    return yahooBannerRenderer.getBannerView();
  }

  @Override
  public void requestInterstitialAd(@NonNull final Context context,
      @NonNull final MediationInterstitialListener listener, @NonNull final Bundle serverParameters,
      @NonNull final MediationAdRequest mediationAdRequest,
      @Nullable final Bundle mediationExtras) {
    setContext(context);
    yahooInterstitialRenderer = new YahooInterstitialRenderer(this);
    yahooInterstitialRenderer.render(context, listener, mediationAdRequest, serverParameters,
        mediationExtras);
  }

  @Override
  public void showInterstitial() {
    Context context = getContext();
    if (context == null) {
      Log.e(TAG, "Failed to show: context is null");
      return;
    }
    yahooInterstitialRenderer.showInterstitial(context);
  }

  @Override
  public void loadRewardedAd(
      @NonNull final MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
      @NonNull final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
          mediationAdLoadCallback) {
    yahooRewardedRenderer = new YahooRewardedRenderer(mediationRewardedAdConfiguration,
        mediationAdLoadCallback);
    yahooRewardedRenderer.render();
  }

  @Override
  public void requestNativeAd(@NonNull final Context context,
      @NonNull final MediationNativeListener listener, @NonNull final Bundle serverParameters,
      @NonNull final NativeMediationAdRequest mediationAdRequest,
      @Nullable final Bundle mediationExtras) {
    yahooNativeRenderer = new YahooNativeRenderer(this);
    yahooNativeRenderer.render(context, listener, serverParameters, mediationAdRequest,
        mediationExtras);
  }

  @Override
  public void onDestroy() {
    Log.i(TAG, "Aborting.");
    if (yahooInterstitialRenderer != null) {
      yahooInterstitialRenderer.destroy();
    }
    if (yahooBannerRenderer != null) {
      yahooBannerRenderer.destroy();
    }
    if (yahooNativeRenderer != null) {
      yahooNativeRenderer.destroy();
    }
    if (yahooRewardedRenderer != null) {
      yahooRewardedRenderer.destroy();
    }
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }

  /**
   * Checks whether Yahoo Mobile SDK is initialized, if not initializes Yahoo Mobile SDK.
   */
  protected static boolean initializeSDK(@NonNull final Context context,
      @NonNull final String siteId) {
    boolean success = true;
    if (!YASAds.isInitialized()) {
      if (!(context instanceof Activity)) {
        Log.e(TAG, "YASAds.initialize must be explicitly called with an Activity context.");
        return false;
      }
      if (TextUtils.isEmpty(siteId)) {
        Log.e(TAG, "Yahoo Mobile SDK Site ID must be set in mediation extras or server parameters");
        return false;
      }
      try {
        Application application = ((Activity) context).getApplication();
        Log.d(TAG, "Initializing using site ID: " + siteId);
        success = YASAds.initialize(application, siteId);
      } catch (Exception e) {
        Log.w(TAG, "Error occurred initializing Yahoo Mobile SDK.", e);
        return false;
      }
    }

    YASAds.getActivityStateManager()
        .setState((Activity) context, ActivityStateManager.ActivityState.RESUMED);
    return success;
  }

  private void setContext(@NonNull Context context) {
    contextWeakRef = new WeakReference<>(context);
  }

  @Nullable
  private Context getContext() {
    if (contextWeakRef == null) {
      return null;
    }
    return contextWeakRef.get();
  }
}
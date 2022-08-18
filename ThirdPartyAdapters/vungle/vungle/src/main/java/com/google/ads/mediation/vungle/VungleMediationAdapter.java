package com.google.ads.mediation.vungle;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.vungle.VungleInitializer.VungleInitializationListener;
import com.google.ads.mediation.vungle.rtb.VungleRtbInterstitialAd;
import com.google.ads.mediation.vungle.rtb.VungleRtbRewardedAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.mediation.VersionInfo;
import com.google.android.gms.ads.mediation.rtb.RtbAdapter;
import com.google.android.gms.ads.mediation.rtb.RtbSignalData;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BannerAd;
import com.vungle.ads.BaseAd;
import com.vungle.ads.BaseAdListener;
import com.vungle.ads.InitializationListener;
import com.vungle.ads.RewardedAd;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleException;
import com.vungle.ads.VungleSettings;
import com.vungle.mediation.BuildConfig;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.VungleManager;
import com.vungle.mediation.VungleNativeAdapter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Mediation network adapter for Vungle.
 */
public class VungleMediationAdapter extends RtbAdapter {

    public static final String TAG = VungleMediationAdapter.class.getSimpleName();
    public static final String KEY_APP_ID = "appid";

    private VungleRtbInterstitialAd rtbInterstitialAd;
    private VungleRtbRewardedAd rtbRewardedAd;
    private VungleRtbRewardedAd rtbRewardedInterstitialAd;

    private AdConfig mAdConfig;
    private String mUserID;
    private String mPlacement;

    private static final HashMap<String, WeakReference<VungleMediationAdapter>> mPlacementsInUse =
            new HashMap<>();

    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
            mMediationAdLoadCallback;
    private MediationRewardedAdCallback mMediationRewardedAdCallback;

    /**
     * Vungle adapter error domain.
     */
    public static final String ERROR_DOMAIN = "com.google.ads.mediation.vungle";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                    ERROR_INVALID_SERVER_PARAMETERS,
                    ERROR_BANNER_SIZE_MISMATCH,
                    ERROR_REQUIRES_ACTIVITY_CONTEXT,
                    ERROR_AD_ALREADY_LOADED,
                    ERROR_VUNGLE_BANNER_NULL,
                    ERROR_INITIALIZATION_FAILURE,
                    ERROR_CANNOT_PLAY_AD,
            })

    public @interface AdapterError {

    }

    /**
     * Server parameters, such as app ID or placement ID, are invalid.
     */
    public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

    /**
     * The requested ad size does not match a Vungle supported banner size.
     */
    public static final int ERROR_BANNER_SIZE_MISMATCH = 102;

    /**
     * Vungle requires an {@link android.app.Activity} context to request ads.
     */
    public static final int ERROR_REQUIRES_ACTIVITY_CONTEXT = 103;

    /**
     * Vungle SDK cannot load multiple ads for the same placement ID.
     */
    public static final int ERROR_AD_ALREADY_LOADED = 104;

    /**
     * Vungle SDK failed to initialize.
     */
    public static final int ERROR_INITIALIZATION_FAILURE = 105;

    /**
     * Vungle SDK returned a successful load callback, but Banners.getBanner() or Vungle.getNativeAd()
     * returned null.
     */
    public static final int ERROR_VUNGLE_BANNER_NULL = 106;

    /**
     * Vungle SDK is not ready to play the ad.
     */
    public static final int ERROR_CANNOT_PLAY_AD = 107;

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

        String logMessage =
                String.format(
                        "Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
                        versionString);
        Log.w(TAG, logMessage);
        return new VersionInfo(0, 0, 0);
    }

    @NonNull
    @Override
    public VersionInfo getSDKVersionInfo() {
        String versionString = com.vungle.ads.BuildConfig.VERSION_NAME;
        String[] splits = versionString.split("\\.");

        if (splits.length >= 3) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]);
            return new VersionInfo(major, minor, micro);
        }

        String logMessage =
                String.format(
                        "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", versionString);
        Log.w(TAG, logMessage);
        return new VersionInfo(0, 0, 0);
    }

    @Override
    public void collectSignals(@NonNull RtbSignalData rtbSignalData,
                               @NonNull SignalCallbacks signalCallbacks) {
//        String token = Vungle.getAvailableBidTokens(rtbSignalData.getContext());
//        Log.d(TAG, "token=" + token);
//        signalCallbacks.onSuccess(token);
    }

    @Override
    public void initialize(@NonNull Context context,
                           @NonNull final InitializationCompleteCallback initializationCompleteCallback,
                           @NonNull List<MediationConfiguration> mediationConfigurations) {

        if (VungleAds.isInitialized()) {
            initializationCompleteCallback.onInitializationSucceeded();
            return;
        }
        HashSet<String> appIDs = new HashSet<>();
        for (MediationConfiguration configuration : mediationConfigurations) {
            Bundle serverParameters = configuration.getServerParameters();
            String appIDFromServer = serverParameters.getString(KEY_APP_ID);

            if (!TextUtils.isEmpty(appIDFromServer)) {
                appIDs.add(appIDFromServer);
            }
        }

        int count = appIDs.size();
        if (count <= 0) {
            if (initializationCompleteCallback != null) {
                AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid App ID.",
                        ERROR_DOMAIN);
                Log.w(TAG, error.getMessage());
                initializationCompleteCallback.onInitializationFailed(error.getMessage());
            }
            return;
        }

        String appID = appIDs.iterator().next();
        if (count > 1) {
            String logMessage =
                    String.format(
                            "Multiple '%s' entries found: %s. Using '%s' to initialize the Vungle SDK.",
                            KEY_APP_ID, appIDs, appID);
            Log.w(TAG, logMessage);
        }

        VungleAds.init(
                context,
                appID,
                new InitializationListener() {

                    @Override
                    public void onSuccess() {
                        initializationCompleteCallback.onInitializationSucceeded();
                    }

                    @Override
                    public void onError(@NonNull VungleException e) {
                        Log.w(TAG, e.getMessage());
                        initializationCompleteCallback.onInitializationFailed(e.getMessage());
                    }
                },
                new VungleSettings()
        );
    }

    @Override
    public void loadRewardedAd(
            @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
                    mediationAdLoadCallback) {
        mMediationAdLoadCallback = mediationAdLoadCallback;

        Bundle mediationExtras = mediationRewardedAdConfiguration.getMediationExtras();
        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();

        mUserID = mediationExtras.getString(VungleExtrasBuilder.EXTRA_USER_ID);

        mPlacement = VungleManager.getInstance().findPlacement(mediationExtras, serverParameters);
        if (TextUtils.isEmpty(mPlacement)) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Failed to load ad from Vungle. Missing or invalid Placement ID.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mediationAdLoadCallback.onFailure(error);
            return;
        }

        if (mPlacementsInUse.containsKey(mPlacement)
                && mPlacementsInUse.get(mPlacement).get() != null) {
            AdError error = new AdError(ERROR_AD_ALREADY_LOADED,
                    "Only a maximum of one ad can be loaded per placement.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mediationAdLoadCallback.onFailure(error);
            return;
        }

        String appID = serverParameters.getString(KEY_APP_ID);
        if (TextUtils.isEmpty(appID)) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Failed to load ad from Vungle. Missing or Invalid App ID.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mediationAdLoadCallback.onFailure(error);
            return;
        }

        // Unmute full-screen ads by default.
        mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);

        VungleRtbRewardedAd ad = new VungleRtbRewardedAd(
                mediationRewardedAdConfiguration,
                mediationAdLoadCallback
        );
        ad.render();
    }

    /**
     * This class is used to map Vungle rewarded video ad rewards to Google Mobile Ads SDK rewards.
     */
    public static class VungleReward implements RewardItem {

        private final String mType;
        private final int mAmount;

        public VungleReward(String type, int amount) {
            mType = type;
            mAmount = amount;
        }

        @Override
        public int getAmount() {
            return mAmount;
        }

        @NonNull
        @Override
        public String getType() {
            return mType;
        }
    }

    @Override
    public void loadNativeAd(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
                             @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
        Log.d(TAG, "loadNativeAd()...");
        VungleInitializer.getInstance()
                .updateCoppaStatus(mediationNativeAdConfiguration.taggedForChildDirectedTreatment());
        VungleNativeAdapter nativeAdapter = new VungleNativeAdapter(mediationNativeAdConfiguration,
                callback);
        nativeAdapter.render();
    }

    public void loadRtbRewardedAd(@NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
                                  @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {
        Log.d(TAG, "loadRtbRewardedAd()...");
        VungleInitializer.getInstance()
                .updateCoppaStatus(mediationRewardedAdConfiguration.taggedForChildDirectedTreatment());
        rtbRewardedAd = new VungleRtbRewardedAd(
                mediationRewardedAdConfiguration, mediationAdLoadCallback);
        rtbRewardedAd.render();
    }

    @Override
    public void loadRtbInterstitialAd(
            @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
            @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
        Log.d(TAG, "loadRtbInterstitialAd()...");
        VungleInitializer.getInstance()
                .updateCoppaStatus(mediationInterstitialAdConfiguration.taggedForChildDirectedTreatment());
        rtbInterstitialAd = new VungleRtbInterstitialAd(
                mediationInterstitialAdConfiguration, mediationAdLoadCallback);
        rtbInterstitialAd.render();
    }

    @Override
    public void loadRtbNativeAd(@NonNull MediationNativeAdConfiguration adConfiguration,
                                @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
        Log.d(TAG, "loadRtbNativeAd()...");
        VungleInitializer.getInstance()
                .updateCoppaStatus(adConfiguration.taggedForChildDirectedTreatment());
        VungleNativeAdapter nativeAdapter = new VungleNativeAdapter(adConfiguration, callback);
        nativeAdapter.render();
    }

    @Override
    public void loadRtbRewardedInterstitialAd(
            @NonNull MediationRewardedAdConfiguration adConfiguration,
            @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
        Log.d(TAG, "loadRtbRewardedInterstitialAd()...");
        VungleInitializer.getInstance()
                .updateCoppaStatus(adConfiguration.taggedForChildDirectedTreatment());
        rtbRewardedInterstitialAd = new VungleRtbRewardedAd(adConfiguration, callback);
        rtbRewardedInterstitialAd.render();
    }

    @Override
    public void loadRewardedInterstitialAd(
            @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
        Log.d(TAG, "loadRewardedInterstitialAd()...");
        loadRewardedAd(mediationRewardedAdConfiguration, callback);
    }
}

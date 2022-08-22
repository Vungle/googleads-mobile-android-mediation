package com.vungle.mediation;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_AD_ALREADY_LOADED;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_APP_ID;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.vungle.VungleBannerAd;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.util.ErrorUtil;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BaseAd;
import com.vungle.ads.BaseAdListener;
import com.vungle.ads.InterstitialAd;
import com.vungle.ads.VungleException;

import java.util.ArrayList;


/**
 * A {@link MediationInterstitialAdapter} used to load and show Vungle interstitial ads using Google
 * Mobile Ads SDK mediation.
 */
@Keep
public class VungleInterstitialAdapter
        implements MediationInterstitialAdapter, MediationBannerAdapter, BaseAdListener {

    private MediationInterstitialListener mMediationInterstitialListener;
    private VungleManager mVungleManager;
    private AdConfig mAdConfig;
    private String mPlacement;

    // banner/MREC
    private MediationBannerListener mMediationBannerListener;
    private VungleBannerAdapter vungleBannerAdapter;
    private InterstitialAd ad;

    @Override
    public void requestInterstitialAd(@NonNull Context context,
                                      @NonNull MediationInterstitialListener mediationInterstitialListener,
                                      @NonNull Bundle serverParameters, @NonNull MediationAdRequest mediationAdRequest,
                                      @Nullable Bundle mediationExtras) {
        String appID = serverParameters.getString(KEY_APP_ID);
        if (TextUtils.isEmpty(appID)) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Missing or invalid App ID.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
            return;
        }

        mMediationInterstitialListener = mediationInterstitialListener;
        mVungleManager = VungleManager.getInstance();
        mPlacement = mVungleManager.findPlacement(mediationExtras, serverParameters);
        if (TextUtils.isEmpty(mPlacement)) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Failed to load ad from Vungle. Missing or Invalid Placement ID.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mMediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
            return;
        }

        VungleInitializer.getInstance()
                .updateCoppaStatus(mediationAdRequest.taggedForChildDirectedTreatment());

        AdapterParametersParser.Config config = AdapterParametersParser.parse(appID, mediationExtras);
        // Unmute full-screen ads by default.
        mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);
        VungleInitializer.getInstance()
                .initialize(
                        config.getAppId(),
                        context.getApplicationContext(),
                        new VungleInitializer.VungleInitializationListener() {
                            @Override
                            public void onInitializeSuccess() {
                                loadAd();
                            }

                            @Override
                            public void onInitializeError(AdError error) {
                                if (mMediationInterstitialListener != null) {
                                    mMediationInterstitialListener
                                            .onAdFailedToLoad(VungleInterstitialAdapter.this, error);
                                    Log.e(TAG, error.getMessage());
                                }
                            }
                        });
    }

    private void loadAd() {
        ad = new InterstitialAd(mPlacement, mAdConfig);
        ad.setAdListener(this);
        if (ad.canPlayAd()) {
            mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
            return;
        }
        ad.load();
    }

    @Override
    public void showInterstitial() {
        ad.play();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: " + hashCode());
        if (vungleBannerAdapter != null) {
            vungleBannerAdapter.destroy();
            vungleBannerAdapter = null;
        }
    }

    // banner
    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        if (vungleBannerAdapter != null) {
            vungleBannerAdapter.updateVisibility(false);
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        if (vungleBannerAdapter != null) {
            vungleBannerAdapter.updateVisibility(true);
        }
    }

    @Override
    public void requestBannerAd(@NonNull Context context,
                                @NonNull final MediationBannerListener mediationBannerListener,
                                @NonNull Bundle serverParameters, @NonNull AdSize adSize,
                                @NonNull MediationAdRequest mediationAdRequest, @Nullable Bundle mediationExtras) {
        mMediationBannerListener = mediationBannerListener;
        String appID = serverParameters.getString(KEY_APP_ID);
        AdapterParametersParser.Config config;
        config = AdapterParametersParser.parse(appID, mediationExtras);

        if (TextUtils.isEmpty(appID)) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Failed to load ad from Vungle. Missing or invalid app ID.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
            return;
        }

        VungleInitializer.getInstance()
                .updateCoppaStatus(mediationAdRequest.taggedForChildDirectedTreatment());

        mVungleManager = VungleManager.getInstance();

        String placement = mVungleManager.findPlacement(mediationExtras, serverParameters);
        Log.d(TAG,
                "requestBannerAd for Placement: " + placement + " ### Adapter instance: " + this
                        .hashCode());

        if (TextUtils.isEmpty(placement)) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Failed to load ad from Vungle. Missing or Invalid placement ID.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
            return;
        }

        AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, true);
        if (!hasBannerSizeAd(context, adSize, adConfig)) {
            AdError error = new AdError(ERROR_BANNER_SIZE_MISMATCH,
                    "Failed to load ad from Vungle. Invalid banner size.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, error);
            return;
        }

        // Adapter does not support multiple Banner instances playing for same placement except for
        // refresh.
        String uniqueRequestId = config.getRequestUniqueId();


        vungleBannerAdapter = new VungleBannerAdapter(placement, uniqueRequestId, adConfig,
                VungleInterstitialAdapter.this);
        Log.d(TAG, "New banner adapter: " + vungleBannerAdapter + "; size: " + adConfig.getAdSize());

        VungleBannerAd vungleBanner = new VungleBannerAd(placement, vungleBannerAdapter);

        Log.d(TAG, "Requesting banner with ad size: " + adConfig.getAdSize());
        vungleBannerAdapter
                .requestBannerAd(context, config.getAppId(), adSize, mMediationBannerListener);
    }

    @NonNull
    @Override
    public View getBannerView() {
        Log.d(TAG, "getBannerView # instance: " + hashCode());
        return vungleBannerAdapter.getAdLayout();
    }

    @Override
    public void adClick(@NonNull BaseAd baseAd) {
        if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdClicked(VungleInterstitialAdapter.this);
        }
    }

    @Override
    public void adEnd(@NonNull BaseAd baseAd) {
        if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
        }
    }

    @Override
    public void adImpression(@NonNull BaseAd baseAd) {

    }

    @Override
    public void adLoaded(@NonNull BaseAd baseAd) {
        if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
        }
    }

    @Override
    public void adStart(@NonNull BaseAd baseAd) {
        if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdOpened(VungleInterstitialAdapter.this);
        }
    }

    @Override
    public void error(@Nullable BaseAd baseAd, @Nullable VungleException e) {
        AdError error = ErrorUtil.getAdError(e);
        Log.e(TAG, error.getMessage());
        if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
        }
    }

    @Override
    public void onAdLeftApplication(@NonNull BaseAd baseAd) {
        if (mMediationInterstitialListener != null) {
            mMediationInterstitialListener.onAdLeftApplication(VungleInterstitialAdapter.this);
        }
    }

    public boolean hasBannerSizeAd(Context context, AdSize adSize, AdConfig adConfig) {
        ArrayList<AdSize> potentials = new ArrayList<>();
        com.vungle.ads.AdSize[] sizes = new com.vungle.ads.AdSize[] {
                com.vungle.ads.AdSize.VUNGLE_MREC,
                com.vungle.ads.AdSize.BANNER,
                com.vungle.ads.AdSize.BANNER_SHORT,
                com.vungle.ads.AdSize.BANNER_LEADERBOARD
        };

        for (com.vungle.ads.AdSize size : sizes) {
            potentials.add(new AdSize(size.getWidth(), size.getHeight()));
        }
        AdSize closestSize = MediationUtils.findClosestSize(context, adSize, potentials);
        if (closestSize == null) {
            Log.i(TAG, "Not found closest ad size: " + adSize);
            return false;
        }
        Log.i(TAG, "Found closest ad size: " + closestSize + " for requested ad size: " + adSize);

        if (closestSize.getWidth() == com.vungle.ads.AdSize.BANNER_SHORT.getWidth()
                && closestSize.getHeight() == com.vungle.ads.AdSize.BANNER_SHORT.getHeight()) {
            adConfig.setAdSize(com.vungle.ads.AdSize.BANNER_SHORT);
        } else if (closestSize.getWidth() == com.vungle.ads.AdSize.BANNER.getWidth()
                && closestSize.getHeight() == com.vungle.ads.AdSize.BANNER.getHeight()) {
            adConfig.setAdSize(com.vungle.ads.AdSize.BANNER);
        } else if (closestSize.getWidth() == com.vungle.ads.AdSize.BANNER_LEADERBOARD.getWidth()
                && closestSize.getHeight() == com.vungle.ads.AdSize.BANNER_LEADERBOARD.getHeight()) {
            adConfig.setAdSize(com.vungle.ads.AdSize.BANNER_LEADERBOARD);
        } else if (closestSize.getWidth() == com.vungle.ads.AdSize.VUNGLE_MREC.getWidth()
                && closestSize.getHeight() == com.vungle.ads.AdSize.VUNGLE_MREC.getHeight()) {
            adConfig.setAdSize(com.vungle.ads.AdSize.VUNGLE_MREC);
        }
        return true;
    }
}

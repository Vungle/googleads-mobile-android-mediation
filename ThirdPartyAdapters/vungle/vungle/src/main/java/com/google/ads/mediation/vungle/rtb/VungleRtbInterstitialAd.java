package com.google.ads.mediation.vungle.rtb;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_APP_ID;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.util.ErrorUtil;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BaseAd;
import com.vungle.ads.BaseAdListener;
import com.vungle.ads.InterstitialAd;
import com.vungle.ads.VungleException;
import com.vungle.mediation.AdapterParametersParser;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.PlacementFinder;

public class VungleRtbInterstitialAd implements MediationInterstitialAd, BaseAdListener {

    @NonNull
    private final MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration;
    @NonNull
    private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback;
    @Nullable
    private MediationInterstitialAdCallback mediationInterstitialAdCallback;

    private String placementId;
    private String adMarkup;

    private InterstitialAd ad = null;

    public VungleRtbInterstitialAd(
            @NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration,
            @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mediationAdLoadCallback) {
        this.mediationInterstitialAdConfiguration = mediationInterstitialAdConfiguration;
        this.mediationAdLoadCallback = mediationAdLoadCallback;
    }

    public void render() {
        Bundle mediationExtras = mediationInterstitialAdConfiguration.getMediationExtras();
        Bundle serverParameters = mediationInterstitialAdConfiguration.getServerParameters();

        String appID = serverParameters.getString(KEY_APP_ID);

        if (TextUtils.isEmpty(appID)) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Missing or invalid App ID.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mediationAdLoadCallback.onFailure(error);
            return;
        }

        placementId = PlacementFinder.findPlacement(mediationExtras, serverParameters);
        if (TextUtils.isEmpty(placementId)) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Failed to load ad from Vungle. Missing or Invalid Placement ID.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mediationAdLoadCallback.onFailure(error);
            return;
        }

        adMarkup = mediationInterstitialAdConfiguration.getBidResponse();
        Log.d(TAG, "Render interstitial mAdMarkup=" + adMarkup);

        AdapterParametersParser.Config config = AdapterParametersParser.parse(appID, mediationExtras);
        // Unmute full-screen ads by default.
        AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);
        VungleInitializer.getInstance()
                .initialize(
                        config.getAppId(),
                        mediationInterstitialAdConfiguration.getContext(),
                        new VungleInitializer.VungleInitializationListener() {
                            @Override
                            public void onInitializeSuccess() {
                                loadAd(adConfig, adMarkup, mediationAdLoadCallback);
                            }

                            @Override
                            public void onInitializeError(AdError error) {
                                Log.w(TAG, error.getMessage());
                                mediationAdLoadCallback.onFailure(error);
                            }
                        });
    }

    private void loadAd(AdConfig adConfig,
                        String adMarkup,
                        MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mMediationAdLoadCallback) {
        ad = new InterstitialAd(placementId, adConfig);
        ad.setAdListener(this);
        if (ad.canPlayAd()) {
            mediationInterstitialAdCallback = mMediationAdLoadCallback.onSuccess(this);
            return;
        }
        ad.load(adMarkup);
    }

    @Override
    public void showAd(@NonNull Context context) {
        if (ad != null && ad.canPlayAd()) {
            ad.play();
        }
    }

    @Override
    public void adClick(@NonNull BaseAd baseAd) {
        if (mediationInterstitialAdCallback != null) {
            mediationInterstitialAdCallback.reportAdClicked();
        }
    }

    @Override
    public void adEnd(@NonNull BaseAd baseAd) {
        if (mediationInterstitialAdCallback != null) {
            mediationInterstitialAdCallback.onAdClosed();
        }
    }

    @Override
    public void adImpression(@NonNull BaseAd baseAd) {
        if (mediationInterstitialAdCallback != null) {
            mediationInterstitialAdCallback.reportAdImpression();
        }
    }

    @Override
    public void adLoaded(@NonNull BaseAd baseAd) {
        mediationInterstitialAdCallback = mediationAdLoadCallback.onSuccess(this);
    }

    @Override
    public void adStart(@NonNull BaseAd baseAd) {
        if (mediationInterstitialAdCallback != null) {
            mediationInterstitialAdCallback.onAdOpened();
        }
    }

    @Override
    public void error(@NonNull BaseAd baseAd, @NonNull VungleException e) {
        Log.e(TAG, e.getMessage());
        mediationAdLoadCallback.onFailure(ErrorUtil.getAdError(e));
    }

    @Override
    public void onAdLeftApplication(@NonNull BaseAd baseAd) {
        if (mediationInterstitialAdCallback != null) {
            mediationInterstitialAdCallback.onAdLeftApplication();
        }
    }
}

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

import com.google.ads.mediation.vungle.util.ErrorUtil;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BaseAd;
import com.vungle.ads.BaseAdListener;
import com.vungle.ads.RewardedAd;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleException;
import com.vungle.mediation.VungleExtrasBuilder;
import com.vungle.mediation.PlacementFinder;

public class VungleRtbRewardedAd implements MediationRewardedAd, BaseAdListener {

    @NonNull
    private final MediationRewardedAdConfiguration mediationRewardedAdConfiguration;
    @NonNull
    private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback;

    @Nullable
    private MediationRewardedAdCallback mediationRewardedAdCallback = null;

    private RewardedAd rewardedAd;

    public VungleRtbRewardedAd(
            @NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration,
            @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mediationAdLoadCallback) {
        this.mediationRewardedAdConfiguration = mediationRewardedAdConfiguration;
        this.mediationAdLoadCallback = mediationAdLoadCallback;
    }

    public void render() {
        Bundle mediationExtras = mediationRewardedAdConfiguration.getMediationExtras();
        Bundle serverParameters = mediationRewardedAdConfiguration.getServerParameters();
        String userId = mediationExtras.getString(VungleExtrasBuilder.EXTRA_USER_ID);

        String appID = serverParameters.getString(KEY_APP_ID);

        if (TextUtils.isEmpty(appID)) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Missing or invalid App ID.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mediationAdLoadCallback.onFailure(error);
            return;
        }

        String placement = PlacementFinder.findPlacement(mediationExtras, serverParameters);
        if (TextUtils.isEmpty(placement)) {
            AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
                    "Failed to load ad from Vungle. Missing or invalid Placement ID.", ERROR_DOMAIN);
            Log.w(TAG, error.getMessage());
            mediationAdLoadCallback.onFailure(error);
            return;
        }

        String adMarkup = mediationRewardedAdConfiguration.getBidResponse();
        Log.d(TAG, "Render rewarded mAdMarkup=" + adMarkup);

        VungleAds.setIncentivizedFields(userId, null, null, null, null);
        // Unmute full-screen ads by default.
        AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false);

        rewardedAd = new RewardedAd(placement, adConfig);
        rewardedAd.setAdListener(this);
        if (rewardedAd.canPlayAd()) {
            mediationRewardedAdCallback =
                    mediationAdLoadCallback.onSuccess(VungleRtbRewardedAd.this);
            return;
        }

        rewardedAd.load(adMarkup);
    }

    @Override
    public void showAd(@NonNull Context context) {
        rewardedAd.play();
    }

    @Override
    public void adClick(@NonNull BaseAd baseAd) {
        if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.reportAdClicked();
        }
    }

    @Override
    public void adEnd(@NonNull BaseAd baseAd) {
        if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.onAdClosed();
        }
    }

    @Override
    public void adImpression(@NonNull BaseAd baseAd) {
        if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.onVideoStart();
            mediationRewardedAdCallback.reportAdImpression();
        }
    }

    @Override
    public void adLoaded(@NonNull BaseAd baseAd) {
        mediationRewardedAdCallback = mediationAdLoadCallback.onSuccess(VungleRtbRewardedAd.this);
    }

    @Override
    public void adStart(@NonNull BaseAd baseAd) {
        if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.onAdOpened();
        }
    }

    @Override
    public void error(@NonNull BaseAd baseAd, @NonNull VungleException e) {
        AdError error = ErrorUtil.getAdError(e);
        Log.e(TAG, error.getMessage());
        if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.onAdFailedToShow(error);
        } else {
            mediationAdLoadCallback.onFailure(error);
        }
    }

    @Override
    public void onAdLeftApplication(@NonNull BaseAd baseAd) {
        // no-op
    }

    // TODO: add callback for this in rewarded ad
    //  @Override
    //  public void onAdRewarded(String placementId) {
    //    if (mMediationRewardedAdCallback != null) {
    //      mMediationRewardedAdCallback.onVideoComplete();
    //      mMediationRewardedAdCallback.onUserEarnedReward(new VungleMediationAdapter.VungleReward("vungle", 1));
    //    }
    //  }
}
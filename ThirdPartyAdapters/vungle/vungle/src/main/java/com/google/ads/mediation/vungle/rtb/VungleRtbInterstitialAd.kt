package com.google.ads.mediation.vungle.rtb

import android.content.Context
import com.vungle.mediation.VungleBannerAdapter.isRequestPending
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.vungle.ads.AdConfig
import android.os.Bundle
import com.google.ads.mediation.vungle.VungleMediationAdapter
import android.text.TextUtils
import android.util.Log
import com.google.android.gms.ads.AdError
import com.vungle.mediation.VungleManager
import com.vungle.mediation.AdapterParametersParser
import com.vungle.mediation.VungleExtrasBuilder
import com.vungle.mediation.VungleBannerAdapter
import com.google.ads.mediation.vungle.VungleBannerAd

class VungleRtbInterstitialAd(
    private val mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
    private val mMediationAdLoadCallback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
) : MediationInterstitialAd {
    private val mediationInterstitialAdCallback: MediationInterstitialAdCallback? = null
    private var mAdConfig: AdConfig? = null
    private var mPlacement: String? = null
    private var mAdMarkup: String? = null
    fun render() {
        val mediationExtras = mediationInterstitialAdConfiguration.mediationExtras
        val serverParameters = mediationInterstitialAdConfiguration.serverParameters
        val appID = serverParameters.getString(VungleMediationAdapter.KEY_APP_ID)
        if (TextUtils.isEmpty(appID)) {
            val error = AdError(
                VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
                "Missing or invalid App ID.", VungleMediationAdapter.ERROR_DOMAIN
            )
            Log.w(VungleMediationAdapter.TAG, error.message)
            mMediationAdLoadCallback.onFailure(error)
            return
        }
        mPlacement = VungleManager.instance.findPlacement(mediationExtras, serverParameters)
        if (TextUtils.isEmpty(mPlacement)) {
            val error = AdError(
                VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
                "Failed to load ad from Vungle. Missing or Invalid Placement ID.",
                VungleMediationAdapter.ERROR_DOMAIN
            )
            Log.w(VungleMediationAdapter.TAG, error.message)
            mMediationAdLoadCallback.onFailure(error)
            return
        }
        mAdMarkup = mediationInterstitialAdConfiguration.bidResponse
        Log.d(VungleMediationAdapter.TAG, "Render interstitial mAdMarkup=$mAdMarkup")
        val config = AdapterParametersParser.parse(appID!!, mediationExtras)
        // Unmute full-screen ads by default.
        mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false)
        //    VungleAds.init(mediationInterstitialAdConfiguration.getContext(),
//            config.getAppId(),
//            this, new VungleSettings());
//    VungleInitializer
//        .initialize(
//            config.getAppId(),
//            mediationInterstitialAdConfiguration.getContext(),
//            new VungleInitializer.VungleInitializationListener() {
//              @Override
//              public void onInitializeSuccess() {
//                loadAd();
//              }
//
//              @Override
//              public void onInitializeError(AdError error) {
//                Log.w(TAG, error.getMessage());
//                mMediationAdLoadCallback.onFailure(error);
//              }
//            });
    }

    private fun loadAd() {
//    if (Vungle.canPlayAd(mPlacement, mAdMarkup)) {
//      mediationInterstitialAdCallback = mMediationAdLoadCallback
//          .onSuccess(VungleRtbInterstitialAd.this);
//      return;
//    }

//    Vungle.loadAd(mPlacement, mAdMarkup, mAdConfig, new LoadAdCallback() {
//      @Override
//      public void onAdLoad(String placementID) {
//        mediationInterstitialAdCallback = mMediationAdLoadCallback
//            .onSuccess(VungleRtbInterstitialAd.this);
//      }
//
//      @Override
//      public void onError(String placementID, VungleException exception) {
//        AdError error = VungleMediationAdapter.getAdError(exception);
//        Log.w(TAG, error.getMessage());
//        mMediationAdLoadCallback.onFailure(error);
//      }
//    });
    }

    override fun showAd(context: Context) {
//    Vungle.playAd(mPlacement, mAdMarkup, mAdConfig, new PlayAdCallback() {
//
//      @Override
//      public void creativeId(String creativeId) {
//        // no-op
//      }
//
//      @Override
//      public void onAdStart(String placementID) {
//        if (mediationInterstitialAdCallback != null) {
//          mediationInterstitialAdCallback.onAdOpened();
//        }
//      }
//
//      @Override
//      public void onAdEnd(String placementID, boolean completed, boolean isCTAClicked) {
//        // Deprecated, no-op.
//      }
//
//      @Override
//      public void onAdEnd(String placementID) {
//        if (mediationInterstitialAdCallback != null) {
//          mediationInterstitialAdCallback.onAdClosed();
//        }
//      }
//
//      @Override
//      public void onAdClick(String placementID) {
//        if (mediationInterstitialAdCallback != null) {
//          mediationInterstitialAdCallback.reportAdClicked();
//        }
//      }

//      @Override
//      public void onAdRewarded(String placementID) {
//        // No-op for interstitial ads.
//      }
//
//      @Override
//      public void onAdLeftApplication(String placementID) {
//        if (mediationInterstitialAdCallback != null) {
//          mediationInterstitialAdCallback.onAdLeftApplication();
//        }
//      }
//
//      @Override
//      public void onError(String placementID, VungleException exception) {
//        AdError error = VungleMediationAdapter.getAdError(exception);
//        Log.w(TAG, error.getMessage());
//        if (mediationInterstitialAdCallback != null) {
//          mediationInterstitialAdCallback.onAdClosed();
//        }
//      }
//
//      @Override
//      public void onAdViewed(String id) {
//        if (mediationInterstitialAdCallback != null) {
//          mediationInterstitialAdCallback.reportAdImpression();
//        }
//      }
//    });
    }
}
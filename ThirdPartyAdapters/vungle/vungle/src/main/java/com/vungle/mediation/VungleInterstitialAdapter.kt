//package com.vungle.mediation
//
//import android.content.Context
//import android.util.Log
//import android.view.View
//import androidx.annotation.Keep
//import com.google.ads.mediation.vungle.VungleInitializer
//import com.google.android.gms.ads.AdSize
//import com.vungle.warren.AdConfig
//
///**
// * A [MediationInterstitialAdapter] used to load and show Vungle interstitial ads using Google
// * Mobile Ads SDK mediation.
// */
//@Keep
//class VungleInterstitialAdapter : MediationInterstitialAdapter, MediationBannerAdapter {
//    private var mMediationInterstitialListener: MediationInterstitialListener? = null
//    private var mVungleManager: VungleManager? = null
//    private var mAdConfig: AdConfig? = null
//    private var mPlacement: String? = null
//
//    // banner/MREC
//    private var mMediationBannerListener: MediationBannerListener? = null
//    private var vungleBannerAdapter: VungleBannerAdapter? = null
//    override fun requestInterstitialAd(
//        context: Context,
//        mediationInterstitialListener: MediationInterstitialListener,
//        serverParameters: Bundle, mediationAdRequest: MediationAdRequest,
//        mediationExtras: Bundle?
//    ) {
//        val appID: String = serverParameters.getString(VungleMediationAdapter.Companion.KEY_APP_ID)
//        if (TextUtils.isEmpty(appID)) {
//            if (mediationInterstitialListener != null) {
//                val error = AdError(
//                    VungleMediationAdapter.Companion.ERROR_INVALID_SERVER_PARAMETERS,
//                    "Missing or invalid App ID.", VungleMediationAdapter.Companion.ERROR_DOMAIN
//                )
//                Log.w(VungleMediationAdapter.Companion.TAG, error.getMessage())
//                mediationInterstitialListener.onAdFailedToLoad(
//                    this@VungleInterstitialAdapter,
//                    error
//                )
//            }
//            return
//        }
//        mMediationInterstitialListener = mediationInterstitialListener
//        mVungleManager = VungleManager.instance
//        mPlacement = mVungleManager!!.findPlacement(mediationExtras, serverParameters)
//        if (TextUtils.isEmpty(mPlacement)) {
//            val error = AdError(
//                VungleMediationAdapter.Companion.ERROR_INVALID_SERVER_PARAMETERS,
//                "Failed to load ad from Vungle. Missing or Invalid Placement ID.",
//                VungleMediationAdapter.Companion.ERROR_DOMAIN
//            )
//            Log.w(VungleMediationAdapter.Companion.TAG, error.getMessage())
//            mMediationInterstitialListener.onAdFailedToLoad(this@VungleInterstitialAdapter, error)
//            return
//        }
//        VungleInitializer.getInstance()
//            .updateCoppaStatus(mediationAdRequest.taggedForChildDirectedTreatment())
//        val config = AdapterParametersParser.parse(appID, mediationExtras)
//        // Unmute full-screen ads by default.
//        mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false)
//        VungleInitializer.getInstance()
//            .initialize(
//                config.appId,
//                context.applicationContext,
//                object : VungleInitializationListener {
//                    override fun onInitializeSuccess() {
//                        loadAd()
//                    }
//
//                    override fun onInitializeError(error: AdError?) {
//                        if (mMediationInterstitialListener != null) {
//                            mMediationInterstitialListener
//                                .onAdFailedToLoad(this@VungleInterstitialAdapter, error)
//                            Log.w(VungleMediationAdapter.Companion.TAG, error.getMessage())
//                        }
//                    }
//                })
//    }
//
//    private fun loadAd() {
//        if (Vungle.canPlayAd(mPlacement)) {
//            if (mMediationInterstitialListener != null) {
//                mMediationInterstitialListener.onAdLoaded(this@VungleInterstitialAdapter)
//            }
//            return
//        }
//        Vungle.loadAd(mPlacement, object : LoadAdCallback() {
//            fun onAdLoad(placementID: String?) {
//                if (mMediationInterstitialListener != null) {
//                    mMediationInterstitialListener.onAdLoaded(this@VungleInterstitialAdapter)
//                }
//            }
//
//            fun onError(placementID: String?, exception: VungleException?) {
//                val error: AdError = VungleMediationAdapter.getAdError(exception)
//                Log.w(VungleMediationAdapter.Companion.TAG, error.getMessage())
//                if (mMediationInterstitialListener != null) {
//                    mMediationInterstitialListener.onAdFailedToLoad(
//                        this@VungleInterstitialAdapter,
//                        error
//                    )
//                }
//            }
//        })
//    }
//
//    override fun showInterstitial() {
//        Vungle.playAd(mPlacement, mAdConfig, object : PlayAdCallback() {
//            fun creativeId(creativeId: String?) {
//                // no-op
//            }
//
//            fun onAdStart(placementID: String?) {
//                if (mMediationInterstitialListener != null) {
//                    mMediationInterstitialListener.onAdOpened(this@VungleInterstitialAdapter)
//                }
//            }
//
//            fun onAdEnd(placementID: String?, completed: Boolean, isCTAClicked: Boolean) {
//                // Deprecated, no-op.
//            }
//
//            fun onAdEnd(placementID: String?) {
//                if (mMediationInterstitialListener != null) {
//                    mMediationInterstitialListener.onAdClosed(this@VungleInterstitialAdapter)
//                }
//            }
//
//            fun onAdClick(placementID: String?) {
//                if (mMediationInterstitialListener != null) {
//                    mMediationInterstitialListener.onAdClicked(this@VungleInterstitialAdapter)
//                }
//            }
//
//            fun onAdRewarded(placementID: String?) {
//                // No-op for interstitial ads.
//            }
//
//            fun onAdLeftApplication(placementID: String?) {
//                if (mMediationInterstitialListener != null) {
//                    mMediationInterstitialListener.onAdLeftApplication(this@VungleInterstitialAdapter)
//                }
//            }
//
//            fun onError(placementID: String?, exception: VungleException?) {
//                val error: AdError = VungleMediationAdapter.getAdError(exception)
//                Log.w(VungleMediationAdapter.Companion.TAG, error.getMessage())
//                if (mMediationInterstitialListener != null) {
//                    mMediationInterstitialListener.onAdClosed(this@VungleInterstitialAdapter)
//                }
//            }
//
//            fun onAdViewed(id: String?) {
//                // No-op.
//            }
//        })
//    }
//
//    override fun onDestroy() {
//        Log.d(VungleMediationAdapter.Companion.TAG, "onDestroy: " + hashCode())
//        if (vungleBannerAdapter != null) {
//            vungleBannerAdapter!!.destroy()
//            vungleBannerAdapter = null
//        }
//    }
//
//    // banner
//    override fun onPause() {
//        Log.d(VungleMediationAdapter.Companion.TAG, "onPause")
//        if (vungleBannerAdapter != null) {
//            vungleBannerAdapter!!.updateVisibility(false)
//        }
//    }
//
//    override fun onResume() {
//        Log.d(VungleMediationAdapter.Companion.TAG, "onResume")
//        if (vungleBannerAdapter != null) {
//            vungleBannerAdapter!!.updateVisibility(true)
//        }
//    }
//
//    override fun requestBannerAd(
//        context: Context,
//        mediationBannerListener: MediationBannerListener,
//        serverParameters: Bundle, adSize: AdSize,
//        mediationAdRequest: MediationAdRequest, mediationExtras: Bundle?
//    ) {
//        mMediationBannerListener = mediationBannerListener
//        val appID: String = serverParameters.getString(VungleMediationAdapter.Companion.KEY_APP_ID)
//        val config: AdapterParametersParser.Config
//        config = AdapterParametersParser.parse(appID, mediationExtras)
//        if (TextUtils.isEmpty(appID)) {
//            if (mediationBannerListener != null) {
//                val error = AdError(
//                    VungleMediationAdapter.Companion.ERROR_INVALID_SERVER_PARAMETERS,
//                    "Failed to load ad from Vungle. Missing or invalid app ID.",
//                    VungleMediationAdapter.Companion.ERROR_DOMAIN
//                )
//                Log.w(VungleMediationAdapter.Companion.TAG, error.getMessage())
//                mediationBannerListener.onAdFailedToLoad(this@VungleInterstitialAdapter, error)
//            }
//            return
//        }
//        VungleInitializer.getInstance()
//            .updateCoppaStatus(mediationAdRequest.taggedForChildDirectedTreatment())
//        mVungleManager = VungleManager.instance
//        val placement = mVungleManager!!.findPlacement(mediationExtras, serverParameters)
//        Log.d(
//            VungleMediationAdapter.Companion.TAG,
//            "requestBannerAd for Placement: $placement ### Adapter instance: " + this
//                .hashCode()
//        )
//        if (TextUtils.isEmpty(placement)) {
//            val error = AdError(
//                VungleMediationAdapter.Companion.ERROR_INVALID_SERVER_PARAMETERS,
//                "Failed to load ad from Vungle. Missing or Invalid placement ID.",
//                VungleMediationAdapter.Companion.ERROR_DOMAIN
//            )
//            Log.w(VungleMediationAdapter.Companion.TAG, error.getMessage())
//            mMediationBannerListener.onAdFailedToLoad(this@VungleInterstitialAdapter, error)
//            return
//        }
//        val adConfig: AdConfig =
//            VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, true)
//        if (!mVungleManager!!.hasBannerSizeAd(context, adSize, adConfig)) {
//            val error = AdError(
//                VungleMediationAdapter.Companion.ERROR_BANNER_SIZE_MISMATCH,
//                "Failed to load ad from Vungle. Invalid banner size.",
//                VungleMediationAdapter.Companion.ERROR_DOMAIN
//            )
//            Log.w(VungleMediationAdapter.Companion.TAG, error.getMessage())
//            mMediationBannerListener.onAdFailedToLoad(this@VungleInterstitialAdapter, error)
//            return
//        }
//
//        // Adapter does not support multiple Banner instances playing for same placement except for
//        // refresh.
//        val uniqueRequestId = config.requestUniqueId
//        if (!mVungleManager!!.canRequestBannerAd(placement!!, uniqueRequestId)) {
//            val error = AdError(
//                VungleMediationAdapter.Companion.ERROR_AD_ALREADY_LOADED,
//                "Vungle adapter does not support multiple banner instances for same placement.",
//                VungleMediationAdapter.Companion.ERROR_DOMAIN
//            )
//            Log.w(VungleMediationAdapter.Companion.TAG, error.getMessage())
//            mMediationBannerListener.onAdFailedToLoad(this@VungleInterstitialAdapter, error)
//            return
//        }
//        vungleBannerAdapter = VungleBannerAdapter(
//            placement, uniqueRequestId, adConfig,
//            this@VungleInterstitialAdapter
//        )
//        Log.d(
//            VungleMediationAdapter.Companion.TAG,
//            "New banner adapter: " + vungleBannerAdapter + "; size: " + adConfig.getAdSize()
//        )
//        val vungleBanner = VungleBannerAd(placement, vungleBannerAdapter)
//        mVungleManager!!.registerBannerAd(placement, vungleBanner)
//        Log.d(
//            VungleMediationAdapter.Companion.TAG,
//            "Requesting banner with ad size: " + adConfig.getAdSize()
//        )
//        vungleBannerAdapter!!
//            .requestBannerAd(context, config.appId, adSize, mMediationBannerListener)
//    }
//
//    val bannerView: View
//        get() {
//            Log.d(VungleMediationAdapter.Companion.TAG, "getBannerView # instance: " + hashCode())
//            return vungleBannerAdapter!!.adLayout!!
//        }
//}
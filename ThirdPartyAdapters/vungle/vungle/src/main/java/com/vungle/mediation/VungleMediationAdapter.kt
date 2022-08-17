package com.vungle.mediation

import android.content.Context
import android.text.TextUtils
import android.util.Log
import androidx.annotation.IntDef
import com.google.ads.mediation.vungle.VungleInitializer
import com.google.ads.mediation.vungle.rtb.VungleRtbInterstitialAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.*
import com.google.android.gms.ads.mediation.rtb.RtbAdapter
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.google.android.gms.ads.rewarded.RewardItem
import com.vungle.ads.*
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.ref.WeakReference

/**
 * Mediation network adapter for Vungle.
 */
open class VungleMediationAdapter : RtbAdapter(), MediationRewardedAd {
    private var rtbInterstitialAd: VungleRtbInterstitialAd? = null
//    private var rtbRewardedAd: VungleRtbRewardedAd? = null
//    private var rtbRewardedInterstitialAd: VungleRtbRewardedAd? = null
    private var mAdConfig: AdConfig? = null
    private var mUserID: String? = null
    private var mPlacement: String? = null
    private var mMediationAdLoadCallback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>? =
        null
    private var mMediationRewardedAdCallback: MediationRewardedAdCallback? = null

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = [ERROR_INVALID_SERVER_PARAMETERS, ERROR_BANNER_SIZE_MISMATCH, ERROR_REQUIRES_ACTIVITY_CONTEXT, ERROR_AD_ALREADY_LOADED, ERROR_VUNGLE_BANNER_NULL, ERROR_INITIALIZATION_FAILURE, ERROR_CANNOT_PLAY_AD])
    annotation class AdapterError

    override fun getVersionInfo(): VersionInfo {
        val versionString = BuildConfig.ADAPTER_VERSION
        val splits = versionString.split("\\.").toTypedArray()
        if (splits.size >= 4) {
            val major = splits[0].toInt()
            val minor = splits[1].toInt()
            val micro = splits[2].toInt() * 100 + splits[3].toInt()
            return VersionInfo(major, minor, micro)
        }
        val logMessage = String.format(
            "Unexpected adapter version format: %s. Returning 0.0.0 for adapter version.",
            versionString
        )
        Log.w(TAG, logMessage)
        return VersionInfo(0, 0, 0)
    }

    override fun getSDKVersionInfo(): VersionInfo {
        val versionString: String = com.vungle.ads.BuildConfig.VERSION_NAME
        val splits = versionString.split("\\.").toTypedArray()
        if (splits.size >= 3) {
            val major = splits[0].toInt()
            val minor = splits[1].toInt()
            val micro = splits[2].toInt()
            return VersionInfo(major, minor, micro)
        }
        val logMessage = String.format(
            "Unexpected SDK version format: %s. Returning 0.0.0 for SDK version.", versionString
        )
        Log.w(TAG, logMessage)
        return VersionInfo(0, 0, 0)
    }

    override fun collectSignals(
        rtbSignalData: RtbSignalData,
        signalCallbacks: SignalCallbacks
    ) {
        val token = VungleAds.getBidTokens()
        Log.d(TAG, "token=$token")
        signalCallbacks.onSuccess(token)
    }

    override fun initialize(
        context: Context,
        initializationCompleteCallback: InitializationCompleteCallback,
        mediationConfigurations: List<MediationConfiguration>
    ) {
        if (VungleAds.isInitialized()) {
            initializationCompleteCallback.onInitializationSucceeded()
            return
        }
        val appIDs = HashSet<String?>()
        for (configuration in mediationConfigurations) {
            val serverParameters = configuration.serverParameters
            val appIDFromServer = serverParameters.getString(KEY_APP_ID)
            if (!TextUtils.isEmpty(appIDFromServer)) {
                appIDs.add(appIDFromServer)
            }
        }
        val count = appIDs.size
        if (count <= 0) {
            val error = AdError(
                ERROR_INVALID_SERVER_PARAMETERS, "Missing or Invalid App ID.",
                ERROR_DOMAIN
            )
            Log.w(TAG, error.message)
            initializationCompleteCallback.onInitializationFailed(error.message)
            return
        }
        val appID = appIDs.iterator().next()
        if (count > 1) {
            val logMessage = String.format(
                "Multiple '%s' entries found: %s. Using '%s' to initialize the Vungle SDK.",
                KEY_APP_ID, appIDs, appID
            )
            Log.w(TAG, logMessage)
        }
        if (appID == null) {
            return
            // TODO: can this be null
        }
//        VungleAds.init(context.applicationContext, appID, object : InitializationListener {
//            override fun onError(vungleException: VungleException) {
//                Log.w(TAG, vungleException.message!!)
//                initializationCompleteCallback.onInitializationFailed(error.message)
//            }
//
//            override fun onSuccess() {
//                initializationCompleteCallback.onInitializationSucceeded()
//            }
//
//        }, VungleSettings())
    }

    override fun loadRewardedAd(
        mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
        mediationAdLoadCallback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
    ) {
//        mMediationAdLoadCallback = mediationAdLoadCallback
//        val mediationExtras = mediationRewardedAdConfiguration.mediationExtras
//        val serverParameters = mediationRewardedAdConfiguration.serverParameters
//        if (mediationExtras != null) {
//            mUserID = mediationExtras.getString(VungleExtrasBuilder.EXTRA_USER_ID)
//        }
//        mPlacement = VungleManager.getInstance().findPlacement(mediationExtras, serverParameters)
//        if (TextUtils.isEmpty(mPlacement)) {
//            val error = AdError(
//                ERROR_INVALID_SERVER_PARAMETERS,
//                "Failed to load ad from Vungle. Missing or invalid Placement ID.", ERROR_DOMAIN
//            )
//            Log.w(TAG, error.message)
//            mediationAdLoadCallback.onFailure(error)
//            return
//        }
//        if (mPlacementsInUse.containsKey(mPlacement)
//            && mPlacementsInUse[mPlacement]!!.get() != null
//        ) {
//            val error = AdError(
//                ERROR_AD_ALREADY_LOADED,
//                "Only a maximum of one ad can be loaded per placement.", ERROR_DOMAIN
//            )
//            Log.w(TAG, error.message)
//            mediationAdLoadCallback.onFailure(error)
//            return
//        }
//        val appID = serverParameters.getString(KEY_APP_ID)
//        if (TextUtils.isEmpty(appID)) {
//            val error = AdError(
//                ERROR_INVALID_SERVER_PARAMETERS,
//                "Failed to load ad from Vungle. Missing or Invalid App ID.", ERROR_DOMAIN
//            )
//            Log.w(TAG, error.message)
//            mediationAdLoadCallback.onFailure(error)
//            return
//        }
//
//        // Unmute full-screen ads by default.
//        mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras, false)
//        VungleInitializer.instance
//            .updateCoppaStatus(mediationRewardedAdConfiguration.taggedForChildDirectedTreatment())
//        VungleInitializer.instance
//            .initialize(
//                appID,
//                mediationRewardedAdConfiguration.context,
//                object : VungleInitializationListener() {
//                    fun onInitializeSuccess() {
//                        Vungle.setIncentivizedFields(mUserID, null, null, null, null)
//                        mPlacementsInUse[mPlacement] = WeakReference(this@VungleMediationAdapter)
//                        if (Vungle.canPlayAd(mPlacement)) {
//                            mMediationRewardedAdCallback =
//                                mMediationAdLoadCallback!!.onSuccess(this@VungleMediationAdapter)
//                            return
//                        }
//                        Vungle.loadAd(mPlacement, mAdConfig, this@VungleMediationAdapter)
//                    }
//
//                    fun onInitializeError(error: AdError) {
//                        Log.w(TAG, error.message)
//                        mMediationAdLoadCallback!!.onFailure(error)
//                        mPlacementsInUse.remove(mPlacement)
//                    }
//                })
    }

    override fun showAd(context: Context) {
//        Vungle.playAd(mPlacement, mAdConfig, this@VungleMediationAdapter)
    }

    /**
     * [LoadAdCallback] implementation from Vungle.
     */
    fun onAdLoad(placementId: String?) {
        if (mMediationAdLoadCallback != null) {
            mMediationRewardedAdCallback =
                mMediationAdLoadCallback!!.onSuccess(this@VungleMediationAdapter)
        }
        mPlacementsInUse[mPlacement] =
            WeakReference(this@VungleMediationAdapter)
    }

    fun creativeId(creativeId: String?) {
        // no-op
    }

    /**
     * [PlayAdCallback] implementation from Vungle
     */
    fun onAdStart(placementId: String?) {
        if (mMediationRewardedAdCallback != null) {
            mMediationRewardedAdCallback!!.onAdOpened()
        }
    }

    @Deprecated("")
    fun onAdEnd(
        placementId: String?, wasSuccessfulView: Boolean,
        wasCallToActionClicked: Boolean
    ) {
    }

    fun onAdEnd(placementId: String?) {
        if (mMediationRewardedAdCallback != null) {
            mMediationRewardedAdCallback!!.onAdClosed()
        }
        mPlacementsInUse.remove(placementId)
    }

    fun onAdClick(placementId: String?) {
        if (mMediationRewardedAdCallback != null) {
            mMediationRewardedAdCallback!!.reportAdClicked()
        }
    }

    fun onAdRewarded(placementId: String?) {
        if (mMediationRewardedAdCallback != null) {
            mMediationRewardedAdCallback!!.onVideoComplete()
            mMediationRewardedAdCallback!!.onUserEarnedReward(VungleReward("vungle", 1))
        }
    }

    fun onAdLeftApplication(placementId: String?) {
        // no op
    }

    // Vungle's LoadAdCallback and PlayAdCallback shares the same onError() call; when an
    // ad request to Vungle fails, and when an ad fails to play.
    fun onError(placementId: String?, throwable: VungleException) {
        val error = getAdError(throwable)
        Log.w(TAG, error.message)
        if (mMediationRewardedAdCallback != null) {
            mMediationRewardedAdCallback!!.onAdFailedToShow(error)
        } else if (mMediationAdLoadCallback != null) {
            mMediationAdLoadCallback!!.onFailure(error)
        }
        mPlacementsInUse.remove(placementId)
    }

    fun onAdViewed(placementId: String?) {
        mMediationRewardedAdCallback!!.onVideoStart()
        mMediationRewardedAdCallback!!.reportAdImpression()
    }

    /**
     * This class is used to map Vungle rewarded video ad rewards to Google Mobile Ads SDK rewards.
     */
    class VungleReward(private val mType: String, private val mAmount: Int) : RewardItem {
        override fun getAmount(): Int {
            return mAmount
        }

        override fun getType(): String {
            return mType
        }
    }

    override fun loadNativeAd(
        mediationNativeAdConfiguration: MediationNativeAdConfiguration,
        callback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
    ) {
        Log.d(TAG, "loadNativeAd()...")
        VungleInitializer.updateCoppaStatus(mediationNativeAdConfiguration.taggedForChildDirectedTreatment())
//        val nativeAdapter = VungleNativeAdapter(
//            mediationNativeAdConfiguration,
//            callback
//        )
//        nativeAdapter.render()
    }

    override fun loadRtbRewardedAd(
        mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
        mediationAdLoadCallback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
    ) {
        // TODO: replace:
//        Log.d(TAG, "loadRtbRewardedAd()...")
//        VungleInitializer.updateCoppaStatus(mediationRewardedAdConfiguration.taggedForChildDirectedTreatment())
//        rtbRewardedAd = VungleRtbRewardedAd(
//            mediationRewardedAdConfiguration, mediationAdLoadCallback
//        )
//        rtbRewardedAd!!.render()
    }

    override fun loadRtbInterstitialAd(
        mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
        mediationAdLoadCallback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
    ) {
        Log.d(TAG, "loadRtbInterstitialAd()...")
        VungleInitializer
            .updateCoppaStatus(mediationInterstitialAdConfiguration.taggedForChildDirectedTreatment())
        rtbInterstitialAd = VungleRtbInterstitialAd(
            mediationInterstitialAdConfiguration, mediationAdLoadCallback
        )
        rtbInterstitialAd!!.render()
    }

    override fun loadRtbNativeAd(
        adConfiguration: MediationNativeAdConfiguration,
        callback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
    ) {
        Log.d(TAG, "loadRtbNativeAd()...")
        VungleInitializer.updateCoppaStatus(adConfiguration.taggedForChildDirectedTreatment())
//        val nativeAdapter = VungleNativeAdapter(adConfiguration, callback)
//        nativeAdapter.render()
    }

    override fun loadRtbRewardedInterstitialAd(
        adConfiguration: MediationRewardedAdConfiguration,
        callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
    ) {
        Log.d(TAG, "loadRtbRewardedInterstitialAd()...")
        VungleInitializer.updateCoppaStatus(adConfiguration.taggedForChildDirectedTreatment())
//        rtbRewardedInterstitialAd = VungleRtbRewardedAd(adConfiguration, callback)
//        rtbRewardedInterstitialAd.render()
    }

    override fun loadRewardedInterstitialAd(
        mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
        callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
    ) {
        Log.d(TAG, "loadRewardedInterstitialAd()...")
        loadRewardedAd(mediationRewardedAdConfiguration, callback)
    }

    companion object {
        @JvmField
        val TAG = VungleMediationAdapter::class.java.simpleName
        const val KEY_APP_ID = "appid"
        private val mPlacementsInUse = HashMap<String?, WeakReference<VungleMediationAdapter?>>()

        /**
         * Vungle adapter error domain.
         */
        const val ERROR_DOMAIN = "com.google.ads.mediation.vungle"

        /**
         * Vungle SDK error domain.
         */
        const val VUNGLE_SDK_ERROR_DOMAIN = "com.vungle.warren"

        /**
         * Server parameters, such as app ID or placement ID, are invalid.
         */
        const val ERROR_INVALID_SERVER_PARAMETERS = 101

        /**
         * The requested ad size does not match a Vungle supported banner size.
         */
        const val ERROR_BANNER_SIZE_MISMATCH = 102

        /**
         * Vungle requires an [android.app.Activity] context to request ads.
         */
        const val ERROR_REQUIRES_ACTIVITY_CONTEXT = 103

        /**
         * Vungle SDK cannot load multiple ads for the same placement ID.
         */
        const val ERROR_AD_ALREADY_LOADED = 104

        /**
         * Vungle SDK failed to initialize.
         */
        const val ERROR_INITIALIZATION_FAILURE = 105

        /**
         * Vungle SDK returned a successful load callback, but Banners.getBanner() or Vungle.getNativeAd()
         * returned null.
         */
        const val ERROR_VUNGLE_BANNER_NULL = 106

        /**
         * Vungle SDK is not ready to play the ad.
         */
        const val ERROR_CANNOT_PLAY_AD = 107

        /**
         * Convert the given Vungle exception into the appropriate custom error code.
         */
        @JvmStatic
        fun getAdError(exception: VungleException): AdError {
            return AdError(
                exception.exceptionCode, exception.localizedMessage,
                VUNGLE_SDK_ERROR_DOMAIN
            )
        }
    }
}
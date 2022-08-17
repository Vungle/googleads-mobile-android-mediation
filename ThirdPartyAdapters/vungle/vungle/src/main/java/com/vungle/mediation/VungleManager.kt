package com.vungle.mediation

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.ads.mediation.vungle.VungleBannerAd
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MediationUtils
import com.vungle.ads.AdConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * A helper class to load and show Vungle ads and keep track of [VungleBannerAd] instances.
 */
class VungleManager private constructor() {
    private val mVungleBanners: ConcurrentHashMap<String, VungleBannerAd>
    private val mVungleNativeAds: ConcurrentHashMap<String, VungleNativeAd?>
    fun findPlacement(networkExtras: Bundle?, serverParameters: Bundle?): String? {
        var placement: String? = null
        if (networkExtras != null
            && networkExtras.containsKey(VungleExtrasBuilder.EXTRA_PLAY_PLACEMENT)
        ) {
            placement = networkExtras.getString(VungleExtrasBuilder.EXTRA_PLAY_PLACEMENT)
        }
        if (serverParameters != null && serverParameters.containsKey(PLAYING_PLACEMENT)) {
            if (placement != null) {
                Log.i(
                    TAG, "'placementID' had a value in both serverParameters and networkExtras. "
                            + "Used one from serverParameters"
                )
            }
            placement = serverParameters.getString(PLAYING_PLACEMENT)
        }
        if (placement == null) {
            Log.e(TAG, "placementID not provided from serverParameters.")
        }
        return placement
    }

    /**
     * Workaround to finish and clean [VungleBannerAdapter] if [ ][VungleInterstitialAdapter.onDestroy] is not called and adapter was garbage collected.
     */
    private fun cleanLeakedBannerAdapters() {
        for (placementId in HashSet(mVungleBanners.keys)) {
            val bannerAd: VungleBannerAd? = mVungleBanners[placementId]
            //            if (bannerAd != null && bannerAd.getAdapter() == null) {
//                removeActiveBannerAd(placementId, bannerAd);
//            }
        }
    }

    @Synchronized
    fun canRequestBannerAd(
        placementId: String,
        requestUniqueId: String?
    ): Boolean {
        cleanLeakedBannerAdapters()
        val bannerAd: VungleBannerAd = mVungleBanners[placementId] ?: return true

//        if (bannerAd.getAdapter() == null) {
//            mVungleBanners.remove(placementId);
//            return true;
//        }

//    VungleBannerAdapter adapter = bannerAd.getAdapter();
//    String activeUniqueRequestId = adapter.getUniqueRequestId();
//    Log.d(TAG,
//        "activeUniqueId: " + activeUniqueRequestId + " ###  RequestId: " + requestUniqueId);
//
//    if (activeUniqueRequestId == null) {
//      Log.w(TAG, "Ad already loaded for placement ID: " + placementId + ", and cannot "
//          + "determine if this is a refresh. Set Vungle extras when making an ad request to "
//          + "support refresh on Vungle banner ads.");
//      return false;
//    }
//
//    if (!activeUniqueRequestId.equals(requestUniqueId)) {
//      Log.w(TAG, "Ad already loaded for placement ID: " + placementId);
//      return false;
//    }
        return true
    }

    fun removeActiveBannerAd(
        placementId: String,
        activeBannerAd: VungleBannerAd?
    ) {
        Log.d(TAG, "try to removeActiveBannerAd: $placementId")
        val didRemove = mVungleBanners.remove(placementId, activeBannerAd)
        if (didRemove && activeBannerAd != null) {
            Log.d(TAG, "removeActiveBannerAd: " + activeBannerAd + "; size=" + mVungleBanners.size)
            //            activeBannerAd.detach();
//            activeBannerAd.destroyAd();
        }
    }

    fun registerBannerAd(placementId: String, instance: VungleBannerAd) {
        removeActiveBannerAd(placementId, mVungleBanners[placementId])
        if (!mVungleBanners.containsKey(placementId)) {
            mVungleBanners[placementId] = instance
            Log.d(TAG, "registerBannerAd: " + instance + "; size=" + mVungleBanners.size)
        }
    }

    fun getVungleBannerAd(placementId: String): VungleBannerAd? {
        return mVungleBanners[placementId]
    }

    fun removeActiveNativeAd(
        placementId: String,
        activeNativeAd: VungleNativeAd?
    ) {
        Log.d(TAG, "try to removeActiveNativeAd: $placementId")
        val didRemove = mVungleNativeAds.remove(placementId, activeNativeAd)
        if (didRemove && activeNativeAd != null) {
            Log.d(
                TAG,
                "removeActiveNativeAd: " + activeNativeAd + "; size=" + mVungleNativeAds.size
            )
            //      activeNativeAd.destroyAd();
        }
    }

    fun registerNativeAd(placementId: String, instance: VungleNativeAd) {
        removeActiveNativeAd(placementId, mVungleNativeAds[placementId])
        if (!mVungleNativeAds.containsKey(placementId)) {
            mVungleNativeAds[placementId] = instance
            Log.d(TAG, "registerNativeAd: " + instance + "; size=" + mVungleNativeAds.size)
        }
    }

    fun hasBannerSizeAd(context: Context?, adSize: AdSize, adConfig: AdConfig?): Boolean {
        val potentials = ArrayList<AdSize>()
        //    potentials.add(new AdSize(BANNER_SHORT.getWidth(), BANNER_SHORT.getHeight()));
//    potentials.add(new AdSize(BANNER.getWidth(), BANNER.getHeight()));
//    potentials.add(new AdSize(BANNER_LEADERBOARD.getWidth(), BANNER_LEADERBOARD.getHeight()));
//    potentials.add(new AdSize(VUNGLE_MREC.getWidth(), VUNGLE_MREC.getHeight()));
        val closestSize = MediationUtils.findClosestSize(context!!, adSize, potentials)
        if (closestSize == null) {
            Log.i(TAG, "Not found closest ad size: $adSize")
            return false
        }
        Log.i(TAG, "Found closest ad size: $closestSize for requested ad size: $adSize")

//    if (closestSize.getWidth() == BANNER_SHORT.getWidth()
//        && closestSize.getHeight() == BANNER_SHORT.getHeight()) {
//      adConfig.setAdSize(BANNER_SHORT);
//    } else if (closestSize.getWidth() == BANNER.getWidth()
//        && closestSize.getHeight() == BANNER.getHeight()) {
//      adConfig.setAdSize(BANNER);
//    } else if (closestSize.getWidth() == BANNER_LEADERBOARD.getWidth()
//        && closestSize.getHeight() == BANNER_LEADERBOARD.getHeight()) {
//      adConfig.setAdSize(BANNER_LEADERBOARD);
//    } else if (closestSize.getWidth() == VUNGLE_MREC.getWidth()
//        && closestSize.getHeight() == VUNGLE_MREC.getHeight()) {
//      adConfig.setAdSize(VUNGLE_MREC);
//    }
        return true
    }

    companion object {
        private const val PLAYING_PLACEMENT = "placementID"
        private val TAG = VungleMediationAdapter.TAG
        private var sInstance: VungleManager? = null

        @JvmStatic
        @get:Synchronized
        val instance: VungleManager
            get() {
                if (sInstance == null) {
                    sInstance = VungleManager()
                }
                return sInstance!!
            }
    }

    init {
        mVungleBanners = ConcurrentHashMap<String, VungleBannerAd>()
        mVungleNativeAds = ConcurrentHashMap()
    }
}
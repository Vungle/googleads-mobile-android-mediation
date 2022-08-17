package com.vungle.mediation

import android.content.Context

/**
 * This class is used to represent a Vungle Native ad.
 */
class VungleNativeAd(
    context: Context,
    private val placementId: String,
    isLifeCycleManagementDisabled: Boolean
) {
//    val nativeAdLayout: NativeAdLayout?
//    val mediaView: MediaView?
//
//    /**
//     * Vungle ad object for native ads.
//     */
//    private val nativeAd: NativeAd?
//    fun loadNativeAd(adConfig: AdConfig?, adMarkup: String?, listener: NativeAdListener?) {
//        nativeAd.loadAd(adConfig, adMarkup, listener)
//    }
//
//    fun getNativeAd(): NativeAd? {
//        return nativeAd
//    }
//
//    fun destroyAd() {
//        if (nativeAdLayout != null) {
//            nativeAdLayout.removeAllViews()
//            if (nativeAdLayout.parent != null) {
//                (nativeAdLayout.parent as ViewGroup).removeView(nativeAdLayout)
//            }
//        }
//        if (mediaView != null) {
//            mediaView.removeAllViews()
//            if (mediaView.parent != null) {
//                (mediaView.parent as ViewGroup).removeView(mediaView)
//            }
//        }
//        if (nativeAd != null) {
//            Log.d(
//                VungleMediationAdapter.TAG,
//                "Vungle native adapter cleanUp: destroyAd # " + nativeAd.hashCode()
//            )
//            nativeAd.unregisterView()
//            nativeAd.destroy()
//        }
//    }
//
//    override fun toString(): String {
//        return (" [placementId="
//                + placementId
//                + " # nativeAdLayout="
//                + nativeAdLayout
//                + " # mediaView="
//                + mediaView
//                + " # nativeAd="
//                + nativeAd
//                + " # hashcode="
//                + hashCode()
//                + "] ")
//    }
//
//    init {
//        nativeAd = NativeAd(context, placementId)
//        nativeAdLayout = NativeAdLayout(context)
//        nativeAdLayout.disableLifeCycleManagement(isLifeCycleManagementDisabled)
//        mediaView = MediaView(context)
//    }
}
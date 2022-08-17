package com.google.ads.mediation.vungle

import com.vungle.mediation.VungleBannerAdapter

/**
 * This class is used to represent a Vungle Banner ad.
 */
class VungleBannerAd(
    /**
     * Vungle banner placement ID.
     */
    private val placementId: String, adapter: VungleBannerAdapter
) {
    /**
     * Weak reference to the adapter owning this Vungle banner ad.
     */
//    private val adapter: WeakReference<VungleBannerAdapter>
//
//    /**
//     * Vungle ad object for banner ads.
//     */
//    private var vungleBanner: VungleBanner? = null
//    fun getAdapter(): VungleBannerAdapter? {
//        return adapter.get()
//    }
//
//    fun setVungleBanner(vungleBanner: VungleBanner) {
//        this.vungleBanner = vungleBanner
//    }
//
//    fun getVungleBanner(): VungleBanner? {
//        return vungleBanner
//    }
//
//    fun attach() {
//        val bannerAdapter: VungleBannerAdapter = adapter.get() ?: return
//        val layout: RelativeLayout = bannerAdapter.getAdLayout() ?: return
//        if (vungleBanner != null && vungleBanner.getParent() == null) {
//            layout.addView(vungleBanner)
//        }
//    }
//
//    fun detach() {
//        if (vungleBanner != null) {
//            if (vungleBanner.getParent() != null) {
//                (vungleBanner.getParent() as ViewGroup).removeView(vungleBanner)
//            }
//        }
//    }
//
//    fun destroyAd() {
//        if (vungleBanner != null) {
//            Log.d(TAG, "Vungle banner adapter cleanUp: destroyAd # " + vungleBanner.hashCode())
//            vungleBanner.destroyAd()
//            vungleBanner = null
//        }
//    }
//
//    init {
//        this.adapter = WeakReference<VungleBannerAdapter>(adapter)
//    }
}
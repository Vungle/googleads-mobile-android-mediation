package com.google.ads.mediation.vungle

import com.vungle.warren.PlayAdCallback
import java.lang.ref.WeakReference

/**
 * Vungle adapter implementation of [PlayAdCallback]. Since the Vungle SDK keeps a strong
 * mapping of ads with strong references to callbacks, this callback class must have no strong
 * references to an adapter object.
 */
class VunglePlayAdCallback(
    callback: PlayAdCallback,
    adapter: VungleBannerAdapter, vungleBannerAd: VungleBannerAd?
) : PlayAdCallback {
    private val adapterReference: WeakReference<VungleBannerAdapter>
    private val callbackReference: WeakReference<PlayAdCallback>
    private val vungleBannerAd: VungleBannerAd?
    fun creativeId(creativeId: String?) {
        // no-op
    }

    fun onAdStart(placementID: String?) {
        val callback: PlayAdCallback? = callbackReference.get()
        val adapter: VungleBannerAdapter? = adapterReference.get()
        if (callback != null && adapter != null && adapter.isRequestPending) {
            callback.onAdStart(placementID)
        }
    }

    @Deprecated("")
    fun onAdEnd(placementID: String?, completed: Boolean, isCTAClicked: Boolean) {
        // Deprecated, No-op.
    }

    fun onAdEnd(placementID: String?) {
        val callback: PlayAdCallback? = callbackReference.get()
        val adapter: VungleBannerAdapter? = adapterReference.get()
        if (callback != null && adapter != null && adapter.isRequestPending) {
            callback.onAdEnd(placementID)
        }
    }

    fun onAdClick(placementID: String?) {
        val callback: PlayAdCallback? = callbackReference.get()
        val adapter: VungleBannerAdapter? = adapterReference.get()
        if (callback != null && adapter != null && adapter.isRequestPending) {
            callback.onAdClick(placementID)
        }
    }

    fun onAdRewarded(placementID: String?) {
        val callback: PlayAdCallback? = callbackReference.get()
        val adapter: VungleBannerAdapter? = adapterReference.get()
        if (callback != null && adapter != null && adapter.isRequestPending) {
            callback.onAdRewarded(placementID)
        }
    }

    fun onAdLeftApplication(placementID: String?) {
        val callback: PlayAdCallback? = callbackReference.get()
        val adapter: VungleBannerAdapter? = adapterReference.get()
        if (callback != null && adapter != null && adapter.isRequestPending) {
            callback.onAdLeftApplication(placementID)
        }
    }

    fun onError(placementID: String?, exception: VungleException?) {
        VungleManager.getInstance().removeActiveBannerAd(placementID, vungleBannerAd)
        val callback: PlayAdCallback? = callbackReference.get()
        val adapter: VungleBannerAdapter? = adapterReference.get()
        if (callback != null && adapter != null && adapter.isRequestPending) {
            callback.onError(placementID, exception)
        }
    }

    fun onAdViewed(placementID: String?) {
        // No-op. To be mapped to respective adapter events in future release.
    }

    init {
        callbackReference = WeakReference<PlayAdCallback>(callback)
        adapterReference = WeakReference<VungleBannerAdapter>(adapter)
        this.vungleBannerAd = vungleBannerAd
    }
}
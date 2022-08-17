package com.vungle.mediation

import android.content.Context
import android.util.Log
import com.vungle.ads.AdConfig
import com.google.android.gms.ads.mediation.MediationBannerAdapter
import com.google.android.gms.ads.mediation.MediationBannerListener
import android.widget.RelativeLayout
import com.google.ads.mediation.vungle.VungleMediationAdapter
import com.google.android.gms.ads.AdSize

//
class VungleBannerAdapter internal constructor(
    placementId: String, uniqueRequestId: String,
    adConfig: AdConfig, mediationBannerAdapter: MediationBannerAdapter
) {
    /**
     * Vungle banner placement ID.
     */
    private val placementId: String

    /**
     * Vungle ad configuration settings.
     */
    private val mAdConfig: AdConfig

    /**
     * Unique Vungle banner request ID.
     */
    private val uniqueRequestId: String

    /**
     * Mediation Banner Adapter instance to receive callbacks.
     */
    private val mediationAdapter: MediationBannerAdapter

    /**
     * Vungle listener class to forward to the adapter.
     */
    private var mediationListener: MediationBannerListener? = null
    /**
     * Wrapper object for Vungle banner ads.
     */
    //  private VungleBannerAd vungleBannerAd;
    /**
     * Container for Vungle's banner ad view.
     */
    var adLayout: RelativeLayout? = null
        private set

    /**
     * Manager to handle Vungle banner ad requests.
     */
    private val mVungleManager: VungleManager

    /**
     * Indicates whether a Vungle banner ad request is in progress.
     */
    var isRequestPending = false
        private set

    /**
     * Indicates the Vungle banner ad's visibility.
     */
    private val mVisibility = true
    fun getUniqueRequestId(): String? {
        return uniqueRequestId
    }

    fun requestBannerAd(
        context: Context, appId: String, adSize: AdSize,
        mediationBannerListener: MediationBannerListener
    ) {
        mediationListener = mediationBannerListener
        requestBannerAd(context, appId, adSize)
    }

    private fun requestBannerAd(context: Context, appId: String, adSize: AdSize) {
        // Create the adLayout wrapper with the requested ad size, as Vungle's ad uses MATCH_PARENT for
        // its dimensions.
        adLayout = object : RelativeLayout(context) {
            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                attach()
            }

            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
                detach()
            }
        }
        var adLayoutHeight = adSize.getHeightInPixels(context)
        // If the height is 0 (e.g. for inline adaptive banner requests), use the closest supported size
        // as the height of the adLayout wrapper.
        if (adLayoutHeight <= 0) {
            val density = context.resources.displayMetrics.density
            adLayoutHeight = Math.round(mAdConfig.adSize.height * density)
        }
        val adViewLayoutParams =
            RelativeLayout.LayoutParams(adSize.getWidthInPixels(context), adLayoutHeight)
        adLayout.setLayoutParams(adViewLayoutParams)
        Log.d(TAG, "requestBannerAd: $this")
        isRequestPending = true
        //    VungleInitializer.getInstance()
//        .initialize(
//            appId,
//            context.getApplicationContext(),
//            new VungleInitializer.VungleInitializationListener() {
//              @Override
//              public void onInitializeSuccess() {
//                loadBanner();
//              }
//
//              @Override
//              public void onInitializeError(AdError error) {
//                mVungleManager.removeActiveBannerAd(placementId, vungleBannerAd);
//                if (mPendingRequestBanner && mediationAdapter != null
//                    && mediationListener != null) {
//                  Log.w(TAG, error.getMessage());
//                  mediationListener.onAdFailedToLoad(mediationAdapter, error);
//                }
//              }
//            });
    }

    fun destroy() {
//    Log.d(TAG, "Vungle banner adapter destroy:" + this);
//    mVisibility = false;
//    mVungleManager.removeActiveBannerAd(placementId, vungleBannerAd);
//    if (vungleBannerAd != null) {
//      vungleBannerAd.detach();
//      vungleBannerAd.destroyAd();
//    }
//    vungleBannerAd = null;
//    mPendingRequestBanner = false;
    }

    fun preCache() {
//    Banners.loadBanner(placementId, new BannerAdConfig(mAdConfig), null);
    }

    fun updateVisibility(visible: Boolean) {
//    if (vungleBannerAd == null) {
//      return;
//    }
//
//    this.mVisibility = visible;
//    if (vungleBannerAd.getVungleBanner() != null) {
//      vungleBannerAd.getVungleBanner().setAdVisibility(visible);
//    }
    }

    //  private final LoadAdCallback mAdLoadCallback =
    //      new LoadAdCallback() {
    //        @Override
    //        public void onAdLoad(String id) {
    //          createBanner();
    //        }
    //
    //        @Override
    //        public void onError(String id, VungleException exception) {
    //          mVungleManager.removeActiveBannerAd(placementId, vungleBannerAd);
    //
    //          if (!mPendingRequestBanner) {
    //            Log.w(TAG, "No banner request fired.");
    //            return;
    //          }
    //          if (mediationAdapter != null && mediationListener != null) {
    //            AdError error = VungleMediationAdapter.getAdError(exception);
    //            Log.w(TAG, error.getMessage());
    //            mediationListener.onAdFailedToLoad(mediationAdapter, error);
    //            return;
    //          }
    //        }
    //      };
    private fun loadBanner() {
//    Log.d(TAG, "loadBanner: " + this);
//    Banners.loadBanner(placementId, new BannerAdConfig(mAdConfig), mAdLoadCallback);
    }

    private fun createBanner() {
//    Log.d(TAG, "create banner: " + this);
//    if (!mPendingRequestBanner) {
//      return;
//    }
//
//    RelativeLayout.LayoutParams adParams = new RelativeLayout.LayoutParams(
//        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//    adParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
//    adParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
//    vungleBannerAd = mVungleManager.getVungleBannerAd(placementId);
//    VunglePlayAdCallback playAdCallback = new VunglePlayAdCallback(VungleBannerAdapter.this,
//        VungleBannerAdapter.this, vungleBannerAd);
//
//    if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
//      VungleBanner vungleBanner = Banners
//          .getBanner(placementId, new BannerAdConfig(mAdConfig), playAdCallback);
//      if (vungleBanner != null) {
//        Log.d(TAG, "display banner:" + vungleBanner.hashCode() + this);
//        if (vungleBannerAd != null) {
//          vungleBannerAd.setVungleBanner(vungleBanner);
//        }
//
//        updateVisibility(mVisibility);
//        vungleBanner.setLayoutParams(adParams);
//        // Don't add to parent here.
//        if (mediationAdapter != null && mediationListener != null) {
//          mediationListener.onAdLoaded(mediationAdapter);
//        }
//      } else {
//        AdError error = new AdError(ERROR_VUNGLE_BANNER_NULL,
//            "Vungle SDK returned a successful load callback, but Banners.getBanner() or "
//                + "Vungle.getNativeAd() returned null.",
//            ERROR_DOMAIN);
//        Log.d(TAG, error.getMessage());
//        if (mediationAdapter != null && mediationListener != null) {
//          mediationListener.onAdFailedToLoad(mediationAdapter, error);
//        }
//      }
//    } else {
//      AdError error = new AdError(ERROR_VUNGLE_BANNER_NULL,
//          "Vungle SDK returned a successful load callback, but Banners.getBanner() or "
//              + "Vungle.getNativeAd() returned null.",
//          ERROR_DOMAIN);
//      Log.d(TAG, error.getMessage());
//      if (mediationAdapter != null && mediationListener != null) {
//        mediationListener.onAdFailedToLoad(mediationAdapter, error);
//      }
//    }
    }

    override fun toString(): String {
        return (" [placementId="
                + placementId
                + " # uniqueRequestId="
                + uniqueRequestId
                + " # hashcode="
                + hashCode()
                + "] ")
    }

    fun attach() {
//    if (vungleBannerAd != null) {
//      vungleBannerAd.attach();
//    }
    }

    fun detach() {
//    if (vungleBannerAd != null) {
//      vungleBannerAd.detach();
//    }
    } //

    //  @Override
    //  public void creativeId(String creativeId) {
    //    // no-op
    //  }
    //
    //  /**
    ////   * Vungle SDK's {@link PlayAdCallback} implementation.
    //   */
    //  @Override
    //  public void onAdStart(String placementID) {
    //    // Let's load it again to mimic auto-cache, don't care about errors.
    //    preCache();
    //  }
    //
    //  @Override
    //  @Deprecated
    //  public void onAdEnd(String placementID, boolean completed, boolean isCTAClicked) {
    //    // Deprecated. No-op.
    //  }
    //
    //  @Override
    //  public void onAdEnd(String placementID) {
    //    // No-op for banner ads.
    //  }
    //
    //  @Override
    //  public void onAdClick(String placementID) {
    //    if (mediationAdapter != null && mediationListener != null) {
    //      mediationListener.onAdClicked(mediationAdapter);
    //      mediationListener.onAdOpened(mediationAdapter);
    //    }
    //  }
    //
    //  @Override
    //  public void onAdRewarded(String placementID) {
    //    // No-op for banner ads.
    //  }
    //
    //  @Override
    //  public void onAdLeftApplication(String placementID) {
    //    if (mediationAdapter != null && mediationListener != null) {
    //      mediationListener.onAdLeftApplication(mediationAdapter);
    //    }
    //  }
    //
    //  @Override
    //  public void onError(String placementID, VungleException exception) {
    //    AdError error = VungleMediationAdapter.getAdError(exception);
    //    Log.w(TAG, error.getMessage());
    //    if (mediationAdapter != null && mediationListener != null) {
    //      mediationListener.onAdFailedToLoad(mediationAdapter, error);
    //    }
    //  }
    //
    //  @Override
    //  public void onAdViewed(String placementID) {
    //    // No-op.
    //  }
    private val TAG = VungleMediationAdapter.TAG

    init {
        mVungleManager = VungleManager.instance
        this.placementId = placementId
        this.uniqueRequestId = uniqueRequestId
        mAdConfig = adConfig
        mediationAdapter = mediationBannerAdapter
    }
}
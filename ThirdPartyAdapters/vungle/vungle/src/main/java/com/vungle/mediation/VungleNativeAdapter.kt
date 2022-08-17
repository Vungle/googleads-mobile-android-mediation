package com.vungle.mediation

import android.text.TextUtils
import android.util.Log
import android.view.View
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.vungle.ads.AdConfig
import com.vungle.mediation.VungleManager.Companion.instance

class VungleNativeAdapter(
    private val adConfiguration: MediationNativeAdConfiguration,
    private val callback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
) : UnifiedNativeAdMapper() {
    private val nativeAdCallback: MediationNativeAdCallback? = null

    /**
     * Vungle native placement ID.
     */
    private var placementId: String? = null

    /**
     * Vungle ad configuration settings.
     */
    private var adConfig: AdConfig? = null
    private var adMarkup: String? = null

    /**
     * Wrapper object for Vungle native ads.
     */
    private var vungleNativeAd: VungleNativeAd? = null
    fun render() {
        val mediationExtras = adConfiguration.mediationExtras
        val serverParameters = adConfiguration.serverParameters
        val nativeAdOptions = adConfiguration.nativeAdOptions
        val context = adConfiguration.context
        val appID = serverParameters.getString(VungleMediationAdapter.KEY_APP_ID)
        if (TextUtils.isEmpty(appID)) {
            val error = AdError(
                VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
                "Failed to load ad from Vungle. Missing or invalid app ID.",
                VungleMediationAdapter.ERROR_DOMAIN
            )
            Log.d(VungleMediationAdapter.TAG, error.message)
            callback.onFailure(error)
            return
        }
        placementId = instance!!.findPlacement(mediationExtras, serverParameters)
        if (TextUtils.isEmpty(placementId)) {
            val error = AdError(
                VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS,
                "Failed to load ad from Vungle. Missing or Invalid placement ID.",
                VungleMediationAdapter.ERROR_DOMAIN
            )
            Log.d(VungleMediationAdapter.TAG, error.message)
            callback.onFailure(error)
            return
        }
        adMarkup = adConfiguration.bidResponse
        Log.d(VungleMediationAdapter.TAG, "Render native adMarkup=$adMarkup")
        adConfig = VungleExtrasBuilder
            .adConfigWithNetworkExtras(mediationExtras, nativeAdOptions, true)
        Log.d(VungleMediationAdapter.TAG, "start to render native ads...")
        vungleNativeAd = VungleNativeAd(
            context, placementId!!,
            mediationExtras.getBoolean(EXTRA_DISABLE_FEED_MANAGEMENT, false)
        )
        instance.registerNativeAd(placementId!!, vungleNativeAd!!)

//    VungleInitializer.
//        .initialize(
//            appID,
//            context.getApplicationContext(),
//            new VungleInitializer.VungleInitializationListener() {
//              @Override
//              public void onInitializeSuccess() {
//                vungleNativeAd.loadNativeAd(adConfig, adMarkup, new NativeListener());
//              }
//
//              @Override
//              public void onInitializeError(AdError error) {
//                VungleManager.getInstance().removeActiveNativeAd(placementId, vungleNativeAd);
//                Log.d(TAG, error.getMessage());
//                callback.onFailure(error);
//              }
//            });
    }

    //  private class NativeListener implements NativeAdListener {
    //
    //    @Override
    //    public void onNativeAdLoaded(NativeAd nativeAd) {
    //      mapNativeAd();
    //      nativeAdCallback = callback.onSuccess(VungleNativeAdapter.this);
    //    }
    //
    //    @Override
    //    public void onAdLoadError(String placementId, VungleException exception) {
    //      VungleManager.getInstance().removeActiveNativeAd(placementId, vungleNativeAd);
    //      AdError error = VungleMediationAdapter.getAdError(exception);
    //      Log.d(TAG, error.getMessage());
    //      callback.onFailure(error);
    //    }
    //
    //    @Override
    //    public void onAdPlayError(String placementId, VungleException exception) {
    //      VungleManager.getInstance().removeActiveNativeAd(placementId, vungleNativeAd);
    //
    //      AdError error = VungleMediationAdapter.getAdError(exception);
    //      Log.d(TAG, error.getMessage());
    //      callback.onFailure(error);
    //    }
    //
    //    @Override
    //    public void onAdClick(String placementId) {
    //      if (nativeAdCallback != null) {
    //        nativeAdCallback.reportAdClicked();
    //        nativeAdCallback.onAdOpened();
    //      }
    //    }
    //
    //    @Override
    //    public void onAdLeftApplication(String placementId) {
    //      if (nativeAdCallback != null) {
    //        nativeAdCallback.onAdLeftApplication();
    //      }
    //    }
    //
    //    @Override
    //    public void creativeId(String creativeId) {
    //      // no-op
    //    }
    //
    //    @Override
    //    public void onAdImpression(String placementId) {
    //      if (nativeAdCallback != null) {
    //        nativeAdCallback.reportAdImpression();
    //      }
    //    }
    //  }
    override fun trackViews(
        view: View, clickableAssetViews: Map<String, View>,
        nonClickableAssetViews: Map<String, View>
    ) {
        super.trackViews(view, clickableAssetViews, nonClickableAssetViews)
        //    Log.d(TAG, "trackViews()");
//    if (!(view instanceof ViewGroup)) {
//      return;
//    }
//
//    ViewGroup adView = (ViewGroup) view;
//
//    if (vungleNativeAd.getNativeAd() == null || !vungleNativeAd.getNativeAd().canPlayAd()) {
//      return;
//    }
//
//    View overlayView = adView.getChildAt(adView.getChildCount() - 1);
//
//    if (!(overlayView instanceof FrameLayout)) {
//      Log.d(TAG, "Vungle requires a FrameLayout to render the native ad.");
//      return;
//    }
//
//    // Since NativeAdView from GMA SDK will be used to render the ad options view,
//    // we need to pass it to the Vungle SDK.
//    vungleNativeAd.getNativeAd().setAdOptionsRootView((FrameLayout) overlayView);
//
//    View iconView = null;
//    ArrayList<View> assetViews = new ArrayList<>();
//    for (Map.Entry<String, View> clickableAssets : clickableAssetViews.entrySet()) {
//      assetViews.add(clickableAssets.getValue());
//
//      if (clickableAssets.getKey().equals(NativeAdAssetNames.ASSET_ICON)) {
//        iconView = clickableAssets.getValue();
//      }
//    }
//
//    ImageView iconImageView = null;
//    if (iconView instanceof ImageView) {
//      iconImageView = (ImageView) iconView;
//    } else {
//      Log.d(TAG, "The view to display a Vungle native icon image is not a type of ImageView, "
//          + "so it can't be registered for click events.");
//    }
//
//    vungleNativeAd.getNativeAd()
//        .registerViewForInteraction(vungleNativeAd.getNativeAdLayout(),
//            vungleNativeAd.getMediaView(), iconImageView, assetViews);
    }

    override fun untrackView(view: View) {
        super.untrackView(view)
        //    Log.d(TAG, "untrackView()");
//    if (vungleNativeAd.getNativeAd() == null) {
//      return;
//    }
//
//    vungleNativeAd.getNativeAd().unregisterView();
    }

    private fun mapNativeAd() {
//    NativeAd nativeAd = vungleNativeAd.getNativeAd();
//    String title = nativeAd.getAdTitle();
//    if (title != null) {
//      setHeadline(title);
//    }
//    String body = nativeAd.getAdBodyText();
//    if (body != null) {
//      setBody(body);
//    }
//    String cta = nativeAd.getAdCallToActionText();
//    if (cta != null) {
//      setCallToAction(cta);
//    }
//    Double starRating = nativeAd.getAdStarRating();
//    if (starRating != null) {
//      setStarRating(starRating);
//    }
//
//    String sponsored = nativeAd.getAdSponsoredText();
//    if (sponsored != null) {
//      setAdvertiser(sponsored);
//    }
//
//    // Since NativeAdView from GMA SDK (instead of Vungle SDK's NativeAdLayout) will be used as
//    // the root view to render Vungle native ad, below is the workaround to set the media view to
//    // ensure impression events will be fired.
//    NativeAdLayout nativeAdLayout = vungleNativeAd.getNativeAdLayout();
//    MediaView mediaView = vungleNativeAd.getMediaView();
//    nativeAdLayout.removeAllViews();
//    nativeAdLayout.addView(mediaView);
//    setMediaView(nativeAdLayout);
//
//    String iconUrl = nativeAd.getAppIcon();
//    if (iconUrl != null && iconUrl.startsWith("file://")) {
//      setIcon(new VungleNativeMappedImage(Uri.parse(iconUrl)));
//    }
//
//    setOverrideImpressionRecording(true);
//    setOverrideClickHandling(true);
    }

//    private class VungleNativeMappedImage(private val imageUri: Uri) : NativeAd.Image() {
//        override fun getDrawable(): Drawable {
//            return null
//        }
//
//        override fun getUri(): Uri {
//            return imageUri
//        }
//
//        override fun getScale(): Double {
//            return 1
//        }
//    }

    override fun toString(): String {
        return (" [placementId="
                + placementId
                + " # hashcode="
                + hashCode()
                + " # vungleNativeAd="
                + vungleNativeAd
                + "] ")
    }

    companion object {
        /**
         * Key to disable automatic management of native ad.
         * Required when displaying Vungle native ad in a RecyclerView.
         */
        const val EXTRA_DISABLE_FEED_MANAGEMENT = "disableFeedLifecycleManagement"
    }
}
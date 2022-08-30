package com.vungle.mediation;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BaseAdListener;
import com.vungle.ads.NativeAd;
import com.vungle.ads.NativeAdLayout;
import com.vungle.ads.internal.ui.view.MediaView;

/**
 * This class is used to represent a Vungle Native ad.
 */
public class VungleNativeAd {

    private final String placementId;

    private final NativeAdLayout nativeAdLayout;

    private final MediaView mediaView;
    private final NativeAd nativeAd;

    /**
     * Vungle ad object for native ads.
     */
    public VungleNativeAd(
            @NonNull Context context,
            @NonNull String placementId,
            @NonNull AdConfig adConfig,
            boolean isLifeCycleManagementDisabled,
            @NonNull BaseAdListener baseAdListener) {
        this.placementId = placementId;
        this.nativeAd = new NativeAd(placementId, adConfig);
        nativeAd.setAdListener(baseAdListener);
        this.nativeAdLayout = new NativeAdLayout(context);
        this.nativeAdLayout.disableLifeCycleManagement(isLifeCycleManagementDisabled);
        this.mediaView = new MediaView(context);
    }

    public void loadNativeAd(@Nullable String adMarkup) {
        nativeAd.load(adMarkup);
    }

    @Nullable
    public NativeAd getNativeAd() {
        return nativeAd;
    }

    public NativeAdLayout getNativeAdLayout() {
        return nativeAdLayout;
    }

    public MediaView getMediaView() {
        return mediaView;
    }

    // TODO: this method isn't being called
    public void destroyAd() {
        if (nativeAdLayout != null) {
            nativeAdLayout.removeAllViews();
            if (nativeAdLayout.getParent() != null) {
                ((ViewGroup) nativeAdLayout.getParent()).removeView(nativeAdLayout);
            }
        }

        if (mediaView != null) {
            mediaView.removeAllViews();
            if (mediaView.getParent() != null) {
                ((ViewGroup) mediaView.getParent()).removeView(mediaView);
            }
        }

        if (nativeAd != null) {
            Log.d(VungleMediationAdapter.TAG, "Vungle native adapter cleanUp: destroyAd # " + nativeAd.hashCode());
            nativeAd.unregisterView();
            nativeAd.destroy();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return " [placementId="
                + placementId
                + " # nativeAdLayout="
                + nativeAdLayout
                + " # mediaView="
                + mediaView
                + " # nativeAd="
                + nativeAd
                + " # hashcode="
                + hashCode()
                + "] ";
    }
}

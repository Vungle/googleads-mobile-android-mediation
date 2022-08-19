package com.google.ads.mediation.vungle;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vungle.ads.BannerAd;
import com.vungle.ads.BannerView;
import com.vungle.mediation.VungleBannerAdapter;

import java.lang.ref.WeakReference;

/**
 * This class is used to represent a Vungle Banner ad.
 */
public class VungleBannerAd {

    /**
     * Weak reference to the adapter owning this Vungle banner ad.
     */
    private final WeakReference<VungleBannerAdapter> adapter;

    /**
     * Vungle banner placement ID.
     */
    private final String placementId;

    /**
     * Vungle ad object for banner ads.
     */
    private BannerView vungleBanner;

    public VungleBannerAd(@NonNull String placementId, @NonNull VungleBannerAdapter adapter) {
        this.placementId = placementId;
        this.adapter = new WeakReference<>(adapter);
    }

    @Nullable
    public VungleBannerAdapter getAdapter() {
        return this.adapter.get();
    }

    public void setVungleBanner(@NonNull BannerView vungleBanner) {
        this.vungleBanner = vungleBanner;
    }

    @Nullable
    public BannerView getVungleBanner() {
        return vungleBanner;
    }

    public void attach() {
        VungleBannerAdapter bannerAdapter = adapter.get();
        if (bannerAdapter == null) {
            return;
        }

        FrameLayout layout = bannerAdapter.getAdLayout();
        if (layout == null) {
            return;
        }

        if (vungleBanner != null && vungleBanner.getParent() == null) {
            layout.addView(vungleBanner);
        }
    }

    public void detach() {
        if (vungleBanner != null) {
            if (vungleBanner.getParent() != null) {
                ((ViewGroup) vungleBanner.getParent()).removeView(vungleBanner);
            }
        }
    }

    public void destroyAd() {
        if (vungleBanner != null) {
//      Log.d(TAG, "Vungle banner adapter cleanUp: destroyAd # " + vungleBanner.hashCode());
            vungleBanner.finishAdInternal(false);
            vungleBanner = null;
        }
    }
}
package com.vungle.mediation;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_VUNGLE_BANNER_NULL;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.ads.mediation.vungle.VungleBannerAd;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.util.ErrorUtil;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BannerAd;
import com.vungle.ads.BannerView;
import com.vungle.ads.BaseAd;
import com.vungle.ads.BaseAdListener;
import com.vungle.ads.VungleException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VungleBannerAdapter implements BaseAdListener {

    /**
     * Vungle banner placement ID.
     */
    @NonNull
    private final String placementId;

    /**
     * Vungle ad configuration settings.
     */
    @NonNull
    private final AdConfig mAdConfig;

    /**
     * Unique Vungle banner request ID.
     */
    private final String uniqueRequestId;

    /**
     * Mediation Banner Adapter instance to receive callbacks.
     */
    private MediationBannerAdapter mediationAdapter;

    /**
     * Vungle listener class to forward to the adapter.
     */
    private MediationBannerListener mediationListener;

    /**
     * Wrapper object for Vungle banner ads.
     */
    private VungleBannerAd vungleBannerAd;

    /**
     * Container for Vungle's banner ad view.
     */
    private FrameLayout adLayout;

    /**
     * Manager to handle Vungle banner ad requests.
     */
    @NonNull
    private final VungleManager mVungleManager;

    /**
     * Indicates whether a Vungle banner ad request is in progress.
     */
    private boolean mPendingRequestBanner = false;

    /**
     * Indicates the Vungle banner ad's visibility.
     */
    private boolean mVisibility = true;

    VungleBannerAdapter(@NonNull String placementId, @NonNull String uniqueRequestId,
                        @NonNull AdConfig adConfig, @NonNull MediationBannerAdapter mediationBannerAdapter) {
        mVungleManager = VungleManager.getInstance();
        this.placementId = placementId;
        this.uniqueRequestId = uniqueRequestId;
        this.mAdConfig = adConfig;
        this.mediationAdapter = mediationBannerAdapter;
    }

    @Nullable
    public String getUniqueRequestId() {
        return uniqueRequestId;
    }

    public FrameLayout getAdLayout() {
        return adLayout;
    }

    public boolean isRequestPending() {
        return mPendingRequestBanner;
    }

    void requestBannerAd(@NonNull Context context, @NonNull String appId, @NonNull AdSize adSize,
                         @NonNull MediationBannerListener mediationBannerListener) {
        mediationListener = mediationBannerListener;

        requestBannerAd(context, appId, adSize);
    }

    private void requestBannerAd(Context context, String appId, AdSize adSize) {
        // Create the adLayout wrapper with the requested ad size, as Vungle's ad uses MATCH_PARENT for
        // its dimensions.
        adLayout = new FrameLayout(context) {
            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                attach();
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                detach();
            }
        };
        int adLayoutHeight = adSize.getHeightInPixels(context);
        // If the height is 0 (e.g. for inline adaptive banner requests), use the closest supported size
        // as the height of the adLayout wrapper.
        if (adLayoutHeight <= 0) {
            float density = context.getResources().getDisplayMetrics().density;
            adLayoutHeight = Math.round(mAdConfig.getAdSize().getHeight() * density);
        }
        RelativeLayout.LayoutParams adViewLayoutParams =
                new RelativeLayout.LayoutParams(adSize.getWidthInPixels(context), adLayoutHeight);
        adLayout.setLayoutParams(adViewLayoutParams);

        Log.d(TAG, "requestBannerAd: " + this);
        mPendingRequestBanner = true;
        VungleInitializer.getInstance()
                .initialize(
                        appId,
                        context.getApplicationContext(),
                        new VungleInitializer.VungleInitializationListener() {
                            @Override
                            public void onInitializeSuccess() {
                                loadBanner();
                            }

                            @Override
                            public void onInitializeError(AdError error) {
                                mVungleManager.removeActiveBannerAd(placementId, vungleBannerAd);
                                if (mPendingRequestBanner && mediationAdapter != null
                                        && mediationListener != null) {
                                    Log.w(TAG, error.getMessage());
                                    mediationListener.onAdFailedToLoad(mediationAdapter, error);
                                }
                            }
                        });
    }

    void destroy() {
        Log.d(TAG, "Vungle banner adapter destroy:" + this);
        mVisibility = false;
        mVungleManager.removeActiveBannerAd(placementId, vungleBannerAd);
        if (vungleBannerAd != null) {
            vungleBannerAd.detach();
            vungleBannerAd.destroyAd();
        }
        vungleBannerAd = null;
        mPendingRequestBanner = false;
    }

    void updateVisibility(boolean visible) {
        if (vungleBannerAd == null) {
            return;
        }

        this.mVisibility = visible;
        if (vungleBannerAd.getVungleBanner() != null) {
            int visibility;
            if (visible) {
                visibility = View.VISIBLE;
            } else {
                visibility = View.GONE;
            }
            vungleBannerAd.getVungleBanner().setVisibility(visibility);
        }
    }

//    private final LoadAdCallback mAdLoadCallback =
//            new LoadAdCallback() {
//                @Override
//                public void onAdLoad(String id) {
//                    createBanner();
//                }
//
//                @Override
//                public void onError(String id, VungleException exception) {
//                    mVungleManager.removeActiveBannerAd(placementId, vungleBannerAd);
//
//                    if (!mPendingRequestBanner) {
//                        Log.w(TAG, "No banner request fired.");
//                        return;
//                    }
//                    if (mediationAdapter != null && mediationListener != null) {
//                        AdError error = VungleMediationAdapter.getAdError(exception);
//                        Log.w(TAG, error.getMessage());
//                        mediationListener.onAdFailedToLoad(mediationAdapter, error);
//                        return;
//                    }
//                }
//            };

    private void loadBanner() {
        Log.d(TAG, "loadBanner: " + this);
        BannerAd bannerAd = new BannerAd(placementId, mAdConfig);
        bannerAd.setAdListener(this);
        bannerAd.load();
    }

    private void createBanner(BannerAd bannerAd) {
        Log.d(TAG, "create banner: " + this);
        if (!mPendingRequestBanner) {
            return;
        }

        FrameLayout.LayoutParams adParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);

        vungleBannerAd.setVungleBanner((BannerView) bannerAd.getBannerView());
        adLayout.addView(bannerAd.getBannerView(), adParams);
    }

    @NonNull
    @Override
    public String toString() {
        return " [placementId="
                + placementId
                + " # uniqueRequestId="
                + uniqueRequestId
                + " # hashcode="
                + hashCode()
                + "] ";
    }

    void attach() {
        if (vungleBannerAd != null) {
            vungleBannerAd.attach();
        }
    }

    void detach() {
        if (vungleBannerAd != null) {
            vungleBannerAd.detach();
        }
    }

    @Override
    public void adClick(@NonNull BaseAd baseAd) {
        if (mediationAdapter != null && mediationListener != null) {
            mediationListener.onAdClicked(mediationAdapter);
            mediationListener.onAdOpened(mediationAdapter);
        }
    }

    @Override
    public void adEnd(@NonNull BaseAd baseAd) {

    }

    @Override
    public void adImpression(@NonNull BaseAd baseAd) {

    }

    @Override
    public void adLoaded(@NonNull BaseAd baseAd) {
//        if (mediationAdapter != null && mediationListener != null) {
//            mediationListener.onAdLoaded(mediationAdapter);
//        }
        mediationListener.onAdLoaded(mediationAdapter);
        BannerAd bannerAd = (BannerAd) baseAd;
        createBanner(bannerAd);
    }

    @Override
    public void adStart(@NonNull BaseAd baseAd) {

    }

    @Override
    public void error(@NonNull BaseAd baseAd, @NonNull VungleException e) {
        AdError error = ErrorUtil.getAdError(e);
        Log.w(TAG, error.getMessage());
        if (mediationAdapter != null && mediationListener != null) {
            mediationListener.onAdFailedToLoad(mediationAdapter, error);
        }
    }

    @Override
    public void onAdLeftApplication(@NonNull BaseAd baseAd) {
        if (mediationAdapter != null && mediationListener != null) {
            mediationListener.onAdLeftApplication(mediationAdapter);
        }
    }


}

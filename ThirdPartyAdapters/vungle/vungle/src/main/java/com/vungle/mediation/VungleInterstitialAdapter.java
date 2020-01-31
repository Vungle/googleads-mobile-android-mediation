package com.vungle.mediation;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.vungle.warren.AdConfig;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.VungleNativeAd;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Keep;

import static com.vungle.warren.AdConfig.AdSize.BANNER;
import static com.vungle.warren.AdConfig.AdSize.BANNER_LEADERBOARD;
import static com.vungle.warren.AdConfig.AdSize.BANNER_SHORT;
import static com.vungle.warren.AdConfig.AdSize.VUNGLE_MREC;

/**
 * A {@link MediationInterstitialAdapter} used to load and show Vungle interstitial ads using
 * Google Mobile Ads SDK mediation.
 */
@Keep
public class VungleInterstitialAdapter implements MediationInterstitialAdapter,
        MediationBannerAdapter {

    private final String TAG = VungleInterstitialAdapter.class.getSimpleName();
    private MediationInterstitialListener mMediationInterstitialListener;
    private VungleManager mVungleManager;
    private AdConfig mAdConfig;
    private String mPlacementForPlay;

    //banner/MREC
    private volatile RelativeLayout adLayout;
    private VungleBanner vungleBannerAd;
    private VungleNativeAd vungleNativeAd;
    private AtomicBoolean pendingRequestBanner = new AtomicBoolean(false);
    private MediationBannerListener mMediationBannerListener;
    private boolean paused;

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener mediationInterstitialListener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {

        AdapterParametersParser.Config config;
        try {
            config = AdapterParametersParser.parse(mediationExtras, serverParameters);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Failed to load ad from Vungle", e);
            if (mediationInterstitialListener != null) {
                mediationInterstitialListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        mMediationInterstitialListener = mediationInterstitialListener;
        mVungleManager = VungleManager.getInstance();

        mPlacementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);
        if (TextUtils.isEmpty(mPlacementForPlay)) {
            Log.w(TAG, "Failed to load ad from Vungle: Missing or Invalid Placement ID");
            mMediationInterstitialListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);
        VungleInitializer.getInstance().initialize(config.getAppId(),
                context.getApplicationContext(),
                new VungleInitializer.VungleInitializationListener() {
                    @Override
                    public void onInitializeSuccess() {
                        loadAd();
                    }

                    @Override
                    public void onInitializeError(String errorMessage) {
                        Log.w(TAG, "Failed to load ad from Vungle: " + errorMessage);
                        if (mMediationInterstitialListener != null) {
                            mMediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                                    AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        }
                    }
                }
        );
    }

    private void loadAd() {
        if (mVungleManager.isAdPlayable(mPlacementForPlay)) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
            }
        } else if (mVungleManager.isValidPlacement(mPlacementForPlay)) {
            mVungleManager.loadAd(mPlacementForPlay, new VungleListener() {
                @Override
                void onAdAvailable() {
                    mMediationInterstitialListener.onAdLoaded(VungleInterstitialAdapter.this);
                }

                @Override
                void onAdFailedToLoad() {
                    mMediationInterstitialListener.onAdFailedToLoad(
                            VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                }
            });
        } else { // passed Placement Id is not what Vungle's SDK gets back after init/config
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
        }
    }


    @Override
    public void showInterstitial() {
        if (mVungleManager != null)
            mVungleManager.playAd(mPlacementForPlay, mAdConfig, new VungleListener() {
                @Override
                void onAdEnd(String placement, boolean wasSuccessfulView, boolean wasCallToActionClicked) {
                    if (mMediationInterstitialListener != null) {
                        if (wasCallToActionClicked) {
                            // Only the call to action button is clickable for Vungle ads. So the
                            // wasCallToActionClicked can be used for tracking clicks.
                            mMediationInterstitialListener.onAdClicked(VungleInterstitialAdapter.this);
                        }
                        mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
                    }
                }

                @Override
                void onAdStart(String placement) {
                    if (mMediationInterstitialListener != null) {
                        mMediationInterstitialListener.onAdOpened(VungleInterstitialAdapter.this);
                    }
                }

                @Override
                void onAdFail(String placement) {
                    if (mMediationInterstitialListener != null) {
                        mMediationInterstitialListener.onAdClosed(VungleInterstitialAdapter.this);
                    }
                }
            });
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        paused = true;
        pendingRequestBanner.set(false);
        if (vungleBannerAd != null) {
            vungleBannerAd.destroyAd();
            mVungleManager.cleanUpBanner(mPlacementForPlay, vungleBannerAd);
            vungleBannerAd = null;
        } else if (vungleNativeAd != null) {
            vungleNativeAd.finishDisplayingAd();
            mVungleManager.cleanUpBanner(mPlacementForPlay, vungleNativeAd);
            vungleNativeAd = null;
        }
        adLayout = null;
    }

    //banner
    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        paused = true;
        updateVisibility();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        paused = false;
        updateVisibility();
    }

    @Override
    public void requestBannerAd(Context context,
                                final MediationBannerListener mediationBannerListener,
                                Bundle serverParameters,
                                AdSize adSize,
                                MediationAdRequest mediationAdRequest,
                                Bundle mediationExtras) {
        Log.d(TAG, "requestBannerAd");
        pendingRequestBanner.set(true);
        mMediationBannerListener = mediationBannerListener;
        AdapterParametersParser.Config config;
        try {
            config = AdapterParametersParser.parse(mediationExtras, serverParameters);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Failed to load ad from Vungle", e);
            if (mediationBannerListener != null) {
                mediationBannerListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        mVungleManager = VungleManager.getInstance();

        mPlacementForPlay = mVungleManager.findPlacement(mediationExtras, serverParameters);

        if (TextUtils.isEmpty(mPlacementForPlay)) {
            String message = "Failed to load ad from Vungle: Missing or Invalid Placement ID.";
            Log.w(TAG, message);
            mMediationBannerListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);
        if (VungleExtrasBuilder.isStartMutedNotConfigured(mediationExtras)) {
            mAdConfig.setMuted(true); // start muted by default
        }
        if (!hasBannerSizeAd(adSize)) {
            String message = "Failed to load ad from Vungle: Invalid banner size.";
            Log.w(TAG, message);
            mMediationBannerListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        adLayout = new RelativeLayout(context);
        // Make adLayout wrapper match the requested ad size, as Vungle's ad uses MATCH_PARENT for
        // its dimensions.
        RelativeLayout.LayoutParams adViewLayoutParams = new RelativeLayout.LayoutParams(
                adSize.getWidthInPixels(context), adSize.getHeightInPixels(context));
        adLayout.setLayoutParams(adViewLayoutParams);
        VungleInitializer.getInstance().initialize(config.getAppId(),
                context.getApplicationContext(),
                new VungleInitializer.VungleInitializationListener() {
                    @Override
                    public void onInitializeSuccess() {
                        loadBanner();
                    }

                    @Override
                    public void onInitializeError(String errorMessage) {
                        Log.w(TAG, "Failed to load ad from Vungle: " + errorMessage);
                        if (mMediationBannerListener != null) {
                            mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                                    AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        }
                    }
                });
    }

    private void loadBanner() {
        if (mVungleManager.isAdPlayable(mPlacementForPlay, mAdConfig.getAdSize())) {
            createBanner();
        } else if (mVungleManager.isValidPlacement(mPlacementForPlay)) {
            mVungleManager.loadAd(mPlacementForPlay, mAdConfig.getAdSize(), new VungleListener() {
                @Override
                void onAdAvailable() {
                    createBanner();
                }

                @Override
                void onAdFailedToLoad() {
                    if (pendingRequestBanner.get() && mMediationBannerListener != null) {
                        mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_NO_FILL);
                    }
                }
            });
        } else { // passed Placement Id is not what Vungle's SDK gets back after init/config
            Log.w(TAG, "Invalid Placement: " + mPlacementForPlay);
            if (mMediationBannerListener != null) {
                mMediationBannerListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
        }
    }

    private VungleListener mVunglePlayListener = new VungleListener() {
        @Override
        void onAdEnd(String placement, boolean wasSuccessfulView, boolean wasCallToActionClicked) {
            if (mMediationBannerListener != null) {
                if (wasCallToActionClicked) {
                    // Only the call to action button is clickable for Vungle ads. So the
                    // wasCallToActionClicked can be used for tracking clicks.
                    mMediationBannerListener.onAdClicked(VungleInterstitialAdapter.this);
                    mMediationBannerListener.onAdOpened(VungleInterstitialAdapter.this);
                    mMediationBannerListener.onAdClosed(VungleInterstitialAdapter.this);
                }
            }
        }

        @Override
        void onAdStart(String placement) {
            //let's load it again to mimic auto-cache, don't care about errors
            mVungleManager.loadAd(placement, mAdConfig.getAdSize(), null);
        }

        @Override
        void onAdFail(String placement) {
            Log.w(TAG, "Ad playback error Placement: " + placement);
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                        AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        }
    };

    private void createBanner() {
        if (mVungleManager == null || !pendingRequestBanner.get())
            return;

        mVungleManager.cleanUpBanner(mPlacementForPlay);

        if (AdConfig.AdSize.isBannerAdSize(mAdConfig.getAdSize())) {
            vungleBannerAd = mVungleManager.getVungleBanner(mPlacementForPlay, mAdConfig.getAdSize(), mVunglePlayListener);
            if (vungleBannerAd != null) {
                updateVisibility();
                adLayout.addView(vungleBannerAd);
                if (mMediationBannerListener != null) {
                    mMediationBannerListener.onAdLoaded(VungleInterstitialAdapter.this);
                }
            } else {
                //missing resources
                if (mMediationBannerListener != null) {
                    mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        } else {
            vungleNativeAd = mVungleManager.getVungleNativeAd(mPlacementForPlay, mAdConfig, mVunglePlayListener);
            View adView = vungleNativeAd != null ? vungleNativeAd.renderNativeView() : null;
            if (adView != null) {
                updateVisibility();
                adLayout.addView(adView);
                if (mMediationBannerListener != null) {
                    mMediationBannerListener.onAdLoaded(VungleInterstitialAdapter.this);
                }
            } else {
                //missing resources
                if (mMediationBannerListener != null) {
                    mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        }
    }

    private void updateVisibility() {
        if (vungleBannerAd != null) {
            vungleBannerAd.setAdVisibility(!paused);
        } else if (vungleNativeAd != null) {
            vungleNativeAd.setAdVisibility(!paused);
        }
    }

    @Override
    public View getBannerView() {
        Log.d(TAG, "getBannerView");
        return adLayout;
    }

    private boolean hasBannerSizeAd(AdSize adSize) {
        AdConfig.AdSize adSizeType = null;

        if (VUNGLE_MREC.getWidth() == adSize.getWidth() && VUNGLE_MREC.getHeight() == adSize.getHeight()) {
            adSizeType = VUNGLE_MREC;
        } else if (BANNER_SHORT.getWidth() == adSize.getWidth() && BANNER_SHORT.getHeight() == adSize.getHeight()) {
            adSizeType = BANNER_SHORT;
        } else if (BANNER.getWidth() == adSize.getWidth() && BANNER.getHeight() == adSize.getHeight()) {
            adSizeType = BANNER;
        } else if (BANNER_LEADERBOARD.getWidth() == adSize.getWidth() && BANNER_LEADERBOARD.getHeight() == adSize.getHeight()) {
            adSizeType = BANNER_LEADERBOARD;
        }

        mAdConfig.setAdSize(adSizeType);

        return adSizeType != null;
    }

}

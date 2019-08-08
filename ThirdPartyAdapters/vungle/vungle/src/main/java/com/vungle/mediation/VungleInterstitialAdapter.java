package com.vungle.mediation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link MediationInterstitialAdapter} used to load and show Vungle interstitial ads using
 * Google Mobile Ads SDK mediation.
 */
@Keep
public class VungleInterstitialAdapter implements MediationInterstitialAdapter, MediationBannerAdapter {

    private final String TAG = VungleInterstitialAdapter.class.getSimpleName();
    private MediationInterstitialListener mMediationInterstitialListener;
    private VungleManager mVungleManager;
    private AdConfig mAdConfig;
    private static final String INTERSTITIAL = "interstitial";
    private static int sCounter = 0;
    private String mAdapterId;
    private String mPlacementForPlay;

    //banner
    private static final String BANNER = "banner";
    private volatile RelativeLayout adLayout;
    private VungleBanner vungleBannerAd;
    private AtomicBoolean pendingRequestBanner = new AtomicBoolean(false);
    private MediationBannerListener mMediationBannerListener;
    private AdConfig.AdSize mAdSize;
    private boolean paused;
    private boolean visible;

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
        mAdapterId = INTERSTITIAL + String.valueOf(sCounter);
        sCounter++;
        VungleInitializer.getInstance().initialize(config.getAppId(),
                context.getApplicationContext(), mAdapterId, new VungleInitializer.VungleInitializationListener() {
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
                    mMediationInterstitialListener.onAdFailedToLoad(VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
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
                            mMediationInterstitialListener.onAdLeftApplication(VungleInterstitialAdapter.this);
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
            mVungleManager.removeActiveBanner(mPlacementForPlay, mAdapterId);
        }
        vungleBannerAd = null;
        adLayout = null;
    }

    //banner
    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        paused = false;
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
        Context mContext = context;
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
        boolean isPlacementValid = !TextUtils.isEmpty(mPlacementForPlay);
        mAdSize = getVungleAdSize(adSize);
        boolean isBannerSizeValid = mAdSize != AdConfig.AdSize.VUNGLE_DEFAULT;
        if (isPlacementValid && isBannerSizeValid) {
            mAdConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras);
            mAdapterId = BANNER + String.valueOf(sCounter);
            sCounter++;
            //workaround for missing onPause/onResume/onDestroy
            adLayout = new RelativeLayout(mContext) {
                @Override
                protected void onWindowVisibilityChanged(int visibility) {
                    super.onWindowVisibilityChanged(visibility);
                    visible = (visibility == VISIBLE);
                    updateVisibility();
                }
            };
            VungleInitializer.getInstance().initialize(config.getAppId(),
                    context.getApplicationContext(), mAdapterId, new VungleInitializer.VungleInitializationListener() {
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
        } else {
            StringBuilder errMsg = new StringBuilder("Failed to load ad from Vungle: ");
            if(!isPlacementValid) {
                errMsg.append("Missing or Invalid Placement ID. ");
            }
            if (!isBannerSizeValid) {
                errMsg.append("Invalid banner size. ");
            }
            Log.w(TAG, errMsg.toString());
            mMediationBannerListener
                    .onAdFailedToLoad(VungleInterstitialAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
        }
    }

    private void loadBanner() {
        if (mVungleManager.isBannerAdPlayable(mPlacementForPlay, mAdSize)) {
            if (mMediationBannerListener != null) {
                createBanner();
                mMediationBannerListener.onAdLoaded(VungleInterstitialAdapter.this);
            }
        } else if (mVungleManager.isValidPlacement(mPlacementForPlay)) {
            mVungleManager.loadBannerAd(mPlacementForPlay, mAdSize, new VungleListener() {
                @Override
                void onAdAvailable() {
                    if (mMediationBannerListener != null) {
                        createBanner();
                        mMediationBannerListener.onAdLoaded(VungleInterstitialAdapter.this);
                    }
                }

                @Override
                void onAdFailedToLoad() {
                    if (pendingRequestBanner.get() && mMediationBannerListener != null) {
                        mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    }
                }
            });
        } else { // passed Placement Id is not what Vungle's SDK gets back after init/config
            if (mMediationBannerListener != null) {
                mMediationBannerListener
                        .onAdFailedToLoad(VungleInterstitialAdapter.this,
                                AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
        }
    }

    private void createBanner() {
        if (mVungleManager == null || !pendingRequestBanner.get())
            return;

        mVungleManager.cleanUpBanner(mPlacementForPlay);
        vungleBannerAd = mVungleManager.getVungleBanner(mAdapterId, mPlacementForPlay, mAdSize, new VungleListener() {
            @Override
            void onAdEnd(String placement, boolean wasSuccessfulView, boolean wasCallToActionClicked) {
                if (mMediationBannerListener != null) {
                    if (wasCallToActionClicked) {
                        // Only the call to action button is clickable for Vungle ads. So the
                        // wasCallToActionClicked can be used for tracking clicks.
                        mMediationBannerListener.onAdClicked(VungleInterstitialAdapter.this);
                        mMediationBannerListener.onAdOpened(VungleInterstitialAdapter.this);
                        mMediationBannerListener.onAdLeftApplication(VungleInterstitialAdapter.this);
                        mMediationBannerListener.onAdClosed(VungleInterstitialAdapter.this);
                    }
                }
            }

            @Override
            void onAdStart(String placement) {
                //let's load it again to mimic auto-cache, don't care about errors
                mVungleManager.loadBannerAd(placement, mAdSize,null);
            }

            @Override
            void onAdFail(String placement) {
                if (mMediationBannerListener != null) {
                    mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        });
        if (vungleBannerAd != null) {
            updateVisibility();
            adLayout.addView(vungleBannerAd);
        } else {
            //missing resources
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdFailedToLoad(VungleInterstitialAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        }
    }

    private void updateVisibility() {
        if (vungleBannerAd != null) {
            vungleBannerAd.setAdVisibility(!paused && visible);
        }
    }

    @Override
    public View getBannerView() {
        Log.d(TAG, "getBannerView");
        return adLayout;
    }

    private AdConfig.AdSize getVungleAdSize(AdSize adSize) {
        AdConfig.AdSize adSizeType = AdConfig.AdSize.VUNGLE_DEFAULT;

        if (300 == adSize.getWidth() && 250 == adSize.getHeight()) {
            mAdSize = AdConfig.AdSize.VUNGLE_MREC;
        } else if (300 == adSize.getWidth() && 50 == adSize.getHeight()) {
            mAdSize = AdConfig.AdSize.BANNER_SHORT;
        } else if (320 == adSize.getWidth() && 50 == adSize.getHeight()) {
            mAdSize = AdConfig.AdSize.BANNER;
        } else if (728 == adSize.getWidth() && 90 == adSize.getHeight()) {
            mAdSize = AdConfig.AdSize.BANNER_LEADERBOARD;
        }
        return adSizeType;
    }

}

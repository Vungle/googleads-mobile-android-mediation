package com.vungle.mediation;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Log;

import com.vungle.warren.AdConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.error.VungleException;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A helper class to load and show Vungle ads and keep track of multiple
 * {@link VungleInterstitialAdapter} instances.
 */
public class VungleManager {

    private static final String TAG = VungleManager.class.getSimpleName();
    private static final String PLAYING_PLACEMENT = "placementID";

    private static VungleManager sInstance;

    private ConcurrentHashMap<String, Pair<String, VungleBanner>> activeBannerAds;

    public static synchronized VungleManager getInstance() {
        if (sInstance == null) {
            sInstance = new VungleManager();
        }
        return sInstance;
    }

    private VungleManager() {
        activeBannerAds = new ConcurrentHashMap<>();
    }

    @Nullable
    public String findPlacement(Bundle networkExtras, Bundle serverParameters) {
        String placement = null;
        if (networkExtras != null
                && networkExtras.containsKey(VungleExtrasBuilder.EXTRA_PLAY_PLACEMENT)) {
            placement = networkExtras.getString(VungleExtrasBuilder.EXTRA_PLAY_PLACEMENT);
        }
        if (serverParameters != null && serverParameters.containsKey(PLAYING_PLACEMENT)) {
            if (placement != null) {
                Log.i(TAG, "'placementID' had a value in both serverParameters and networkExtras. "
                        + "Used one from serverParameters");
            }
            placement = serverParameters.getString(PLAYING_PLACEMENT);
        }
        if (placement == null) {
            Log.e(TAG, "placementID not provided from serverParameters. Please check your AdMob dashboard settings." +
                    "load and play functionality will not work");
        }
        return placement;
    }

    void loadAd(String adapterId, String placement, @Nullable AdConfig adConfig, @Nullable final VungleListener listener) {
        Vungle.loadAd(placement, adConfig, new LoadAdCallback() {
            @Override
            public void onAdLoad(String id) {
                if (listener != null) {
                    listener.onAdAvailable();
                }
            }

            @Override
            public void onError(String id, VungleException cause) {
                if (listener != null) {
                    listener.onAdFailedToLoad();
                }
            }
        });
    }

    void playAd(String adapterId, String placement, AdConfig cfg, @Nullable VungleListener listener) {
        Vungle.playAd(placement, cfg, playAdCallback(listener));
    }

    private PlayAdCallback playAdCallback(@Nullable final VungleListener listener) {
        return new PlayAdCallback() {
            @Override
            public void onAdStart(String id) {
                if (listener != null) {
                    listener.onAdStart(id);
                }
            }

            @Override
            public void onAdEnd(String id, boolean completed, boolean isCTAClicked) {
                if (listener != null) {
                    listener.onAdEnd(id, completed, isCTAClicked);
                }
            }

            @Override
            public void onError(String id, VungleException error) {
                if (listener != null) {
                    listener.onAdFail(id);
                }
            }
        };
    }

    void removeListeners(String adapterId) {
        //loadListeners.remove(adapterId);
    }

    boolean isAdPlayable(String placement) {
        return (placement != null && !placement.isEmpty()) &&
                Vungle.canPlayAd(placement);
    }

    /**
     * Checks and returns if the passed Placement ID is a valid placement for App ID
     *
     * @param placementId
     * @return
     */
    boolean isValidPlacement(String placementId) {
        return Vungle.isInitialized() &&
                Vungle.getValidPlacements().contains(placementId);
    }

    public VungleBanner getVungleBanner(String adapterId, String placement, @NonNull AdConfig adConfig, VungleListener vungleListener) {
        Log.d(TAG, "getVungleNativeAd");
        //Since we VungleInterstitialAdapter#onDestroy() does not called by AdMob SDK,
        // we have to take care of removal of listener
        cleanUpBanner(placement);
        //Fetch new ad

        VungleBanner bannerAd = Vungle.getBanner(placement, adConfig.getAdSize(), playAdCallback(vungleListener));
        if (bannerAd != null) {
            activeBannerAds.put(placement, new Pair<>(adapterId, bannerAd));
        }

        return bannerAd;
    }

    public void removeActiveBanner(String placementId, String adapterId) {
        if (placementId == null)
            return;
        Pair<String, VungleBanner> pair = activeBannerAds.get(placementId);
        if (pair != null && adapterId != null && adapterId.equals(pair.first)) {
            activeBannerAds.remove(placementId, pair);
        }
    }

    /**
     * called from adapters to clean, and remove
     *
     * @param placementId
     */
    public void cleanUpBanner(String placementId) {
        Log.d(TAG, "cleanUpBanner");
        Pair<String, VungleBanner> pair = activeBannerAds.get(placementId);
        if (pair != null) {
            String adapterId = pair.first; //TODO why not all elements, if somehow we end up with more than one
            removeListeners(adapterId);
            //Remove ad
            VungleBanner vungleNativeAd = pair.second;
            if (vungleNativeAd != null) {
                //We should do Report ad
                Log.d(TAG, "cleanUpBanner # finishDisplayingAd");
                vungleNativeAd.finishBanner();
                removeActiveBanner(placementId, adapterId);
            }
        }
    }
}

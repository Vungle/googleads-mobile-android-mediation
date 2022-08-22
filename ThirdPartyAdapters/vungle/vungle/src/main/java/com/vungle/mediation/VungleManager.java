package com.vungle.mediation;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.vungle.VungleBannerAd;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.vungle.ads.AdConfig;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A helper class to load and show Vungle ads and keep track of {@link VungleBannerAd} instances.
 */
public class VungleManager {

    private static final String PLAYING_PLACEMENT = "placementID";

    private static VungleManager sInstance;

    private final ConcurrentHashMap<String, VungleNativeAd> mVungleNativeAds;

    public static synchronized VungleManager getInstance() {
        if (sInstance == null) {
            sInstance = new VungleManager();
        }
        return sInstance;
    }

    private VungleManager() {
        mVungleNativeAds = new ConcurrentHashMap<>();
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
                Log.i(
                        TAG,
                        "'placementID' had a value in both serverParameters and networkExtras. "
                                + "Used one from serverParameters");
            }
            placement = serverParameters.getString(PLAYING_PLACEMENT);
        }
        if (placement == null) {
            Log.e(TAG, "placementID not provided from serverParameters.");
        }
        return placement;
    }


    public void removeActiveNativeAd(@NonNull String placementId,
                                     @Nullable VungleNativeAd activeNativeAd) {
        Log.d(TAG, "try to removeActiveNativeAd: " + placementId);

        boolean didRemove = mVungleNativeAds.remove(placementId, activeNativeAd);
        if (didRemove && activeNativeAd != null) {
            Log.d(TAG, "removeActiveNativeAd: " + activeNativeAd + "; size=" + mVungleNativeAds.size());
//      activeNativeAd.destroyAd();
        }
    }

    public void registerNativeAd(@NonNull String placementId, @NonNull VungleNativeAd instance) {
        removeActiveNativeAd(placementId, mVungleNativeAds.get(placementId));
        if (!mVungleNativeAds.containsKey(placementId)) {
            mVungleNativeAds.put(placementId, instance);
            Log.d(TAG, "registerNativeAd: " + instance + "; size=" + mVungleNativeAds.size());
        }
    }
}

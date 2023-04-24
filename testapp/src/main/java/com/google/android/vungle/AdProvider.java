package com.google.android.vungle;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.android.vungle.AdDialogFragment.AdDialogInteractionListener;
import com.google.android.vungle.data.DataSource;
import com.vungle.mediation.VungleAdapter;
import com.vungle.mediation.VungleInterstitialAdapter;

import java.util.HashMap;
import java.util.Locale;

public class AdProvider {

    private static final String TAG = AdProvider.class.getSimpleName();
    private HashMap<AdUnit, Object> admobHolder = new HashMap<>();

    private static AdProvider sInstance = null;
    private Context context;

    private AdProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized AdProvider getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AdProvider(context);
        }
        return sInstance;
    }

    public void onDestroy() {
        admobHolder.clear();
    }

    public void loadAndPlay(@NonNull AppCompatActivity activity, @Nullable UnitsAdapter adapter,
                            @NonNull AdUnit adUnit) {
        loadAd(activity, adapter, adUnit, true);
    }

    public void loadAd(@NonNull AppCompatActivity activity, @Nullable UnitsAdapter adapter,
                       @NonNull AdUnit unit) {
        loadAd(activity, adapter, unit, false);
    }

    private void loadAd(final @NonNull AppCompatActivity activity,
                        final @Nullable UnitsAdapter unitsAdapter,
                        final @NonNull AdUnit unit, final boolean autoPlay) {
        String unitName = unit.getType().name() + "(" + unit.getId() + ")";

        if (unitsAdapter != null) {
            unitsAdapter.setUnitStatus(unit, UnitsAdapter.STATE_LOADING);
            unitName = unitsAdapter.getUnitName(unit) + " (" + unit.getId() + ")";
        }
        final String adUnitName = unitName;
        String msg = String.format("Load ad pressed for %s", unitName);
        if (unitsAdapter != null) {
            unitsAdapter.log(msg);
        } else {
            Log.d(TAG, msg);
        }
        Bundle extras = DataSource.getInstance().getVungleExtras();
        switch (unit.getType()) {
            case RewardedAd: {
                AdRequest adRequest;
                if (unit.isOpenBidding()) {
                    adRequest = new AdRequest.Builder()
                            .addNetworkExtrasBundle(VungleMediationAdapter.class, extras)
                            .build();
                } else {
                    adRequest = new AdRequest.Builder()
                            .addNetworkExtrasBundle(VungleAdapter.class, extras)
                            .build();
                }

                RewardedAd.load(activity, unit.getId(),
                        adRequest, new RewardedAdLoadCallback() {
                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                Log.d(TAG, loadAdError.getMessage());
                                String msg = String
                                        .format(Locale.ENGLISH, "onRewardedAdFailedToLoad with code: %s for %s",
                                                loadAdError.getMessage(),
                                                adUnitName);
                                updateAdUnitStatus(unitsAdapter, unit, UnitsAdapter.STATE_IDLE, msg);
                            }

                            @Override
                            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                                admobHolder.put(unit, rewardedAd);

                                String msg = String.format("onRewardedAdLoaded for %s", adUnitName);
                                updateAdUnitStatus(unitsAdapter, unit, UnitsAdapter.STATE_LOADED, msg);
                                rewardedAd.setFullScreenContentCallback(getFullScreenContentCallback(
                                        unitsAdapter, unit, adUnitName
                                ));
                                if (autoPlay) {
                                    playAd(activity, unitsAdapter, unit);
                                }

                            }
                        });
            }
            break;
            case Interstitial: {
                AdRequest adRequest;
                if (unit.isOpenBidding()) {
                    adRequest = new AdRequest.Builder()
                            .addNetworkExtrasBundle(VungleMediationAdapter.class, extras)
                            .build();
                } else {
                    adRequest = new AdRequest.Builder()
                            .addNetworkExtrasBundle(VungleInterstitialAdapter.class, extras)
                            .build();
                }

                InterstitialAd.load(activity.getApplicationContext(), unit.getId(), adRequest,
                        new InterstitialAdLoadCallback() {
                            @Override
                            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                                // The mInterstitialAd reference will be null until
                                // an ad is loaded.
                                admobHolder.put(unit, interstitialAd);

                                String msg = String.format("onAdLoaded for %s", adUnitName);
                                updateAdUnitStatus(unitsAdapter, unit, UnitsAdapter.STATE_LOADED, msg);
                                if (autoPlay) {
                                    playAd(activity, unitsAdapter, unit);
                                }

                                interstitialAd.setFullScreenContentCallback(getFullScreenContentCallback(
                                        unitsAdapter, unit, adUnitName
                                ));
                            }

                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                String msg = String
                                        .format(Locale.ENGLISH, "onAdFailedToLoad witch code: %s for %s",
                                                loadAdError,
                                                adUnitName);
                                updateAdUnitStatus(unitsAdapter, unit, UnitsAdapter.STATE_IDLE, msg);
                            }
                        });
            }
            break;
            case RewardedInterstitial: {
                AdRequest adRequest = new AdRequest.Builder()
                        .addNetworkExtrasBundle(VungleMediationAdapter.class, extras)
                        .build();

                RewardedInterstitialAd.load(activity, unit.getId(),
                        adRequest, new RewardedInterstitialAdLoadCallback() {
                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                Log.d(TAG, loadAdError.getMessage());
                                String msg = String
                                        .format(Locale.ENGLISH,
                                                "onRewardedInterstitialAdFailedToLoad with code: %s for %s",
                                                loadAdError.getMessage(),
                                                adUnitName);
                                updateAdUnitStatus(unitsAdapter, unit, UnitsAdapter.STATE_IDLE, msg);
                            }

                            @Override
                            public void onAdLoaded(@NonNull RewardedInterstitialAd rewardedAd) {
                                admobHolder.put(unit, rewardedAd);
                                String msg = String.format("onRewardedInterstitialAdLoaded for %s", adUnitName);
                                updateAdUnitStatus(unitsAdapter, unit, UnitsAdapter.STATE_LOADED, msg);
                                rewardedAd.setFullScreenContentCallback(getFullScreenContentCallback(
                                        unitsAdapter, unit, adUnitName
                                ));
                                if (autoPlay) {
                                    playAd(activity, unitsAdapter, unit);
                                }

                            }
                        });
            }
        }
    }

    private void updateAdUnitStatus(UnitsAdapter unitsAdapter, AdUnit unit, int status, String msg) {
        if (unitsAdapter != null) {
            unitsAdapter.setUnitStatus(unit, status);
            unitsAdapter.log(msg);
        } else {
            Log.d(TAG, msg);
        }
    }

    private FullScreenContentCallback getFullScreenContentCallback(
            UnitsAdapter unitsAdapter, AdUnit unit, String adUnitName) {
        return new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when fullscreen content is dismissed.

                String msg = String.format("onAdDismissedFullScreenContent for %s", adUnitName);
                if (unitsAdapter != null) {
                    unitsAdapter.log(msg);
                } else {
                    Log.d(TAG, msg);
                }
                admobHolder.remove(unit);
                updateAdUnitStatus(unitsAdapter, unit, UnitsAdapter.STATE_IDLE, msg);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                // Called when fullscreen content failed to show.
                String msg = String
                        .format(Locale.ENGLISH, "onAdFailedToShowFullScreenContent with code: %s for %s",
                                adError.getMessage(),
                                adUnitName);
                admobHolder.remove(unit);
                updateAdUnitStatus(unitsAdapter, unit, UnitsAdapter.STATE_IDLE, msg);
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d("TAG", "The ad was shown.");
                admobHolder.remove(unit);
                String msg = String.format("onAdShowedFullScreenContent for %s", adUnitName);
                updateAdUnitStatus(unitsAdapter, unit, UnitsAdapter.STATE_PLAYING, msg);
            }
        };
    }

    public void playAd(@NonNull AppCompatActivity activity, @Nullable final UnitsAdapter adapter,
                       @NonNull AdUnit unit) {
        String unitName = unit.getId();
        if (adapter != null) {
            unitName = adapter.getUnitName(unit) + " (" + unit.getId() + ")";
            adapter.setUnitStatus(unit, UnitsAdapter.STATE_PLAYING);
        }
        final String unitNameLog = unitName;
        String msg = String.format("Play ad pressed for %s", unitName);
        if (adapter != null) {
            adapter.log(msg);
        } else {
            Log.d(TAG, msg);
        }
        if (admobHolder.get(unit) instanceof InterstitialAd) {
            ((InterstitialAd) admobHolder.get(unit)).show(activity);
        } else if (admobHolder.get(unit) instanceof RewardedAd) {
            ((RewardedAd) admobHolder.get(unit))
                    .show(activity, getUserEarnedRewardListener(adapter, unitName));
        } else if (admobHolder.get(unit) instanceof RewardedInterstitialAd) {
            final RewardedInterstitialAd rewardedInterstitialAd = (RewardedInterstitialAd) admobHolder
                    .get(unit);
            RewardItem rewardItem = rewardedInterstitialAd.getRewardItem();
            int rewardAmount = rewardItem.getAmount();
            String rewardType = rewardItem.getType();

            Log.d(TAG, "The rewarded interstitial ad is ready.");
            AdDialogFragment dialog = AdDialogFragment.newInstance(rewardAmount, rewardType);
            dialog.setAdDialogInteractionListener(
                    new AdDialogInteractionListener() {
                        @Override
                        public void onShowAd() {
                            Log.d(TAG, "The rewarded interstitial ad is starting.");
                            rewardedInterstitialAd.show(activity, getUserEarnedRewardListener(adapter, unitNameLog));
                        }

                        @Override
                        public void onCancelAd() {
                            Log.d(TAG, "The rewarded interstitial ad was skipped before it starts.");
                            if (adapter != null) {
                                adapter.setUnitStatus(unit, UnitsAdapter.STATE_LOADED);
                            }
                        }
                    });
            dialog.show(activity.getSupportFragmentManager(), "AdDialogFragment");
        }
    }

    private OnUserEarnedRewardListener getUserEarnedRewardListener(UnitsAdapter adapter,
                                                                   String adUnitName) {
        return rewardItem -> {
            // Handle the reward.
            String msg = String.format(Locale.ENGLISH,
                    "onUserEarnedReward with currency: %s, amount: %d for %s",
                    rewardItem.getType(), rewardItem.getAmount(), adUnitName);
            if (adapter != null) {
                adapter.log(msg);
            } else {
                Log.d(TAG, msg);
            }
        };
    }

}
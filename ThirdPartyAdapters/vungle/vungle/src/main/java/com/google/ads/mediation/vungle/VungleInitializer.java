package com.google.ads.mediation.vungle;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.ads.mediation.vungle.util.ErrorUtil;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.vungle.ads.InitializationListener;
import com.vungle.ads.Plugin;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleException;
import com.vungle.ads.VungleSettings;
import com.vungle.ads.internal.network.VungleApiClient;
import com.vungle.mediation.VungleNetworkSettings;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class VungleInitializer implements InitializationListener {

    private static final VungleInitializer instance = new VungleInitializer();
    private final AtomicBoolean isInitializing = new AtomicBoolean(false);
    private final ArrayList<VungleInitializationListener> initListener;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public static VungleInitializer getInstance() {
        return instance;
    }

    private VungleInitializer() {
        Plugin.addWrapperInfo(
                VungleApiClient.WrapperFramework.admob,
                com.vungle.mediation.BuildConfig.ADAPTER_VERSION.replace('.', '_')
        );
        initListener = new ArrayList<>();
    }

    public void initialize(
            final String appId, final Context context, VungleInitializationListener listener) {

        if (VungleAds.isInitialized()) {
            listener.onInitializeSuccess();
            return;
        }

        if (isInitializing.getAndSet(true)) {
            initListener.add(listener);
            return;
        }
        VungleNetworkSettings.setVungleSettingsChangedListener(vungleSettings -> {
            if (!VungleAds.isInitialized()) {
                return;
            }

            // Pass new settings to SDK.
            updateCoppaStatus(MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment());
            VungleAds.init(context.getApplicationContext(), appId, VungleInitializer.this, vungleSettings);
        });

        updateCoppaStatus(MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment());

        VungleSettings vungleSettings = VungleNetworkSettings.getVungleSettings();
        VungleAds.init(context, appId, VungleInitializer.this, vungleSettings);
        initListener.add(listener);
    }


    public void updateCoppaStatus(int configuration) {
        switch (configuration) {
            case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE:
                VungleAds.updateUserCoppaStatus(true);
                break;
            case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE:
                VungleAds.updateUserCoppaStatus(false);
                break;
            case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED:
            default:
                // Vungle's SDK only supports updating a user's COPPA status with true and false
                // values. If you haven't specified how you would like your content treated with
                // respect to COPPA in ad requests, you must indicate in the Vungle Publisher
                // Dashboard whether your app is directed toward children under age 13.
                break;
        }
    }

    @Override
    public void onError(@NonNull VungleException e) {
        final AdError error = ErrorUtil.getAdError(e);
        mHandler.post(() -> {
            for (VungleInitializationListener listener : initListener) {
                listener.onInitializeError(error);
            }
            initListener.clear();
        });
        isInitializing.set(false);
    }

    @Override
    public void onSuccess() {
        mHandler.post(() -> {
            for (VungleInitializationListener listener : initListener) {
                listener.onInitializeSuccess();
            }
            initListener.clear();
        });
        isInitializing.set(false);
    }

    public interface VungleInitializationListener {

        void onInitializeSuccess();

        void onInitializeError(AdError error);

    }
}
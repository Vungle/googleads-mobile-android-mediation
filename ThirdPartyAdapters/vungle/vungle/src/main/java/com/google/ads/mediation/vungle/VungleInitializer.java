package com.google.ads.mediation.vungle;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.ads.mediation.vungle.util.ErrorUtil;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.vungle.ads.internal.network.Plugin;
import com.google.android.gms.ads.RequestConfiguration;
import com.vungle.ads.InitializationListener;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleException;
import com.vungle.ads.VungleSettings;
import com.vungle.ads.internal.network.Plugin;
import com.vungle.ads.internal.network.VungleApiClient;
import com.vungle.mediation.VungleNetworkSettings;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class VungleInitializer implements InitializationListener {

    private static final VungleInitializer instance = new VungleInitializer();
    private final AtomicBoolean mIsInitializing = new AtomicBoolean(false);
    private final ArrayList<VungleInitializationListener> mInitListeners;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public static VungleInitializer getInstance() {
        return instance;
    }

    private VungleInitializer() {
        Plugin.addWrapperInfo(
                VungleApiClient.WrapperFramework.admob,
                com.vungle.mediation.BuildConfig.ADAPTER_VERSION.replace('.', '_')
        );
        mInitListeners = new ArrayList<>();

    }

    public void initialize(
            final String appId, final Context context, VungleInitializationListener listener) {

        if (VungleAds.isInitialized()) {
            listener.onInitializeSuccess();
            return;
        }

        if (mIsInitializing.getAndSet(true)) {
            mInitListeners.add(listener);
            return;
        }
//        VungleNetworkSettings.setVungleSettingsChangedListener(new VungleNetworkSettings.VungleSettingsChangedListener() {
//            @Override
//            public void onVungleSettingsChanged(@NonNull VungleSettings vungleSettings) {
//
//            }
//        });

        // Keep monitoring VungleSettings in case of any changes we need to re-init SDK to apply
        // updated settings.
//    VungleNetworkSettings.setVungleSettingsChangedListener(
//        new VungleNetworkSettings.VungleSettingsChangedListener() {
//          @Override
//          public void onVungleSettingsChanged(@NonNull VungleSettings settings) {
//            // Ignore if sdk is yet to initialize, it will get considered while init.
//            if (!Vungle.isInitialized()) {
//              return;
//            }
//
//            // Pass new settings to SDK.
//            updateCoppaStatus(
//                MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment());
//            Vungle.init(appId, context.getApplicationContext(), VungleInitializer.this, settings);
//          }
//        });

        updateCoppaStatus(MobileAds.getRequestConfiguration().getTagForChildDirectedTreatment());
//
        VungleSettings vungleSettings = VungleNetworkSettings.getVungleSettings();
        VungleAds.init(context, appId, VungleInitializer.this, vungleSettings);
        mInitListeners.add(listener);
    }


    public void updateCoppaStatus(int configuration) {
//    switch (configuration) {
//      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE:
//        Vungle.updateUserCoppaStatus(true);
//        break;
//      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE:
//        Vungle.updateUserCoppaStatus(false);
//        break;
//      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED:
//      default:
//        // Vungle's SDK only supports updating a user's COPPA status with true and false
//        // values. If you haven't specified how you would like your content treated with
//        // respect to COPPA in ad requests, you must indicate in the Vungle Publisher
//        // Dashboard whether your app is directed toward children under age 13.
//        break;
//    }
    }

    @Override
    public void onError(@NonNull VungleException e) {
        final AdError error = ErrorUtil.getAdError(e);
        mHandler.post(() -> {
            for (VungleInitializationListener listener : mInitListeners) {
                listener.onInitializeError(error);
            }
            mInitListeners.clear();
        });
        mIsInitializing.set(false);
    }

    @Override
    public void onSuccess() {
        mHandler.post(() -> {
            for (VungleInitializationListener listener : mInitListeners) {
                listener.onInitializeSuccess();
            }
            mInitListeners.clear();
        });
        mIsInitializing.set(false);
    }

    public interface VungleInitializationListener {

        void onInitializeSuccess();

        void onInitializeError(AdError error);

    }
}
package com.google.ads.mediation.vungle

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.ads.mediation.vungle.VungleMediationAdapter.Companion.getAdError
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.MobileAds
import com.vungle.ads.InitializationListener
import com.vungle.ads.VungleAds
import com.vungle.ads.VungleException
import com.vungle.ads.VungleSettings
import com.vungle.ads.internal.network.Plugin
import com.vungle.ads.internal.network.VungleApiClient
import com.vungle.mediation.BuildConfig
import com.vungle.mediation.VungleNetworkSettings.VungleSettingsChangedListener
import com.vungle.mediation.VungleNetworkSettings.getVungleSettings
import com.vungle.mediation.VungleNetworkSettings.setVungleSettingsChangedListener
import java.util.concurrent.atomic.AtomicBoolean

object VungleInitializer : InitializationListener {

    private val mIsInitializing = AtomicBoolean(false)

    private val mInitListeners: ArrayList<VungleInitializationListener> = ArrayList()

    private val mHandler = Handler(Looper.getMainLooper())

    fun initialize(appId: String, context: Context, listener: VungleInitializationListener) {
        if (VungleAds.isInitialized()) {
            listener.onInitializeSuccess()
            return
        }
        if (mIsInitializing.getAndSet(true)) {
            mInitListeners.add(listener)
            return
        }

        // Keep monitoring VungleSettings in case of any changes we need to re-init SDK to apply
        // updated settings.
        setVungleSettingsChangedListener(
            object : VungleSettingsChangedListener {
                override fun onVungleSettingsChanged(settings: VungleSettings) {
                    // Ignore if sdk is yet to initialize, it will get considered while init.
                    if (!VungleAds.isInitialized()) {
                        return
                    }

                    // Pass new settings to SDK.
                    updateCoppaStatus(
                        MobileAds.getRequestConfiguration().tagForChildDirectedTreatment
                    )
                    VungleAds.init(context, appId, this@VungleInitializer, settings)
                }
            })
        updateCoppaStatus(MobileAds.getRequestConfiguration().tagForChildDirectedTreatment)
        val vungleSettings: VungleSettings = getVungleSettings()
        VungleAds.init(context.applicationContext, appId, this@VungleInitializer, vungleSettings)
        mInitListeners.add(listener)
    }

    override fun onSuccess() {
        mHandler.post {
            for (listener in mInitListeners) {
                listener.onInitializeSuccess()
            }
            mInitListeners.clear()
        }
        mIsInitializing.set(false)
    }

    override fun onError(vungleException: VungleException) {
        val error = getAdError(vungleException!!)
        mHandler.post {
            for (listener in mInitListeners) {
                listener.onInitializeError(error)
            }
            mInitListeners.clear()
        }
        mIsInitializing.set(false)
    }

    fun onAutoCacheAdAvailable(placementId: String?) {
        // Unused
    }

    fun updateCoppaStatus(configuration: Int) {
        // TODO: Update coppa status
//        when (configuration) {
//            RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE -> Vungle.updateUserCoppaStatus(
//                true
//            )
//            RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE -> Vungle.updateUserCoppaStatus(
//                false
//            )
//            RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED -> {}
//            else -> {}
//        }
    }

    interface VungleInitializationListener {

        fun onInitializeSuccess()

        fun onInitializeError(error: AdError?)
    }

    init {
        Plugin.addWrapperInfo(
            VungleApiClient.WrapperFramework.admob,
            BuildConfig.ADAPTER_VERSION.replace('.', '_')
        )
    }
}
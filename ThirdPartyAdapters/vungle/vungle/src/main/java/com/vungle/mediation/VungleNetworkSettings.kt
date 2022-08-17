package com.vungle.mediation

import com.vungle.ads.VungleSettings

/**
 * To apply the Vungle network settings during initialization.
 */
object VungleNetworkSettings {
    private const val MEGABYTE = (1024 * 1024).toLong()
    private var minimumSpaceForInit = 50 * MEGABYTE
    private var minimumSpaceForAd = 51 * MEGABYTE
    private var androidIdOptedOut = false
    private var vungleSettings: VungleSettings = VungleSettings().apply {
        bannerRefreshDisabled = true
    }
    private var vungleSettingsChangedListener: VungleSettingsChangedListener? = null

    @JvmStatic
    fun setMinSpaceForInit(spaceForInit: Long) {
        minimumSpaceForInit = spaceForInit
        applySettings()
    }

    @JvmStatic
    fun setMinSpaceForAdLoad(spaceForAd: Long) {
        minimumSpaceForAd = spaceForAd
        applySettings()
    }

    @JvmStatic
    fun setAndroidIdOptOut(isOptedOut: Boolean) {
        androidIdOptedOut = isOptedOut
        applySettings()
    }

    /**
     * To pass Vungle network setting to SDK. this method must be called before first loadAd. if
     * called after first loading an ad, settings will not be applied.
     */
    private fun applySettings() {
        vungleSettings = VungleSettings().also {
            it.minimumSpaceForAd = minimumSpaceForAd
            it.bannerRefreshDisabled = true
            // TODO: opt out of android ID
        }

        vungleSettingsChangedListener?.onVungleSettingsChanged(vungleSettings)
    }

    fun getVungleSettings(): VungleSettings {
        return vungleSettings
    }

    fun setVungleSettingsChangedListener(
        settingsChangedListener: VungleSettingsChangedListener?
    ) {
        vungleSettingsChangedListener = settingsChangedListener
    }

    interface VungleSettingsChangedListener {
        fun onVungleSettingsChanged(vungleSettings: VungleSettings)
    }
}
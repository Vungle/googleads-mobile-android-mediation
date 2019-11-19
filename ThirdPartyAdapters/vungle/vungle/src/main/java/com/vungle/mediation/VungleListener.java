package com.vungle.mediation;

/**
 * A listener class used to send Vungle events from {@link VungleManager} to
 * {@link VungleInterstitialAdapter} and {@link VungleAdapter}.
 */
abstract class VungleListener {

    void onAdStart(String placement) {}

    void onAdClick(String placement) {}

    void onAdEnd(String placement) {}

    void onAdRewarded(String placement) {}

    void onAdLeftApplication(String placement) {}

    void onAdFail(String placement) {}

    void onAdAvailable() {}

    void onAdFailedToLoad() {}
}

package com.vungle.mediation

import com.vungle.ads.internal.model.RtbTokens


/**
 * A public static class used to set Vungle Consent Status.
 */
object VungleConsent {
    /**
     * Update GDPR consent status and corresponding version number.
     */
    @JvmStatic
    fun updateConsentStatus(
        consentStatus: RtbTokens.Consent?, consentMessageVersion: String?
    ) {
//        Vungle.updateConsentStatus(consentStatus, consentMessageVersion)
    }

//    @JvmStatic
//    val currentVungleConsent: Consent
//        get() = Vungle.getConsentStatus()
//    val currentVungleConsentMessageVersion: String
//        get() = Vungle.getConsentMessageVersion()
}
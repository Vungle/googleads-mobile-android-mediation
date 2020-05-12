package com.vungle.mediation;

import com.vungle.warren.Vungle;

/**
 * A public static class used to set Vungle Consent Status.
 */

public class VungleConsent {

  private static Vungle.Consent sCurrentVungleConsent = null;
  private static String sCurrentVungleConsentMessageVersion = "";

  /**
   * Update GDPR consent status and corresponding version number
   */
  public static void updateConsentStatus(Vungle.Consent consentStatus,
      String consentMessageVersion) {
    sCurrentVungleConsent = consentStatus;
    sCurrentVungleConsentMessageVersion = consentMessageVersion;

    if (Vungle.isInitialized() && sCurrentVungleConsent != null
        && sCurrentVungleConsentMessageVersion != null) {
      Vungle.updateConsentStatus(sCurrentVungleConsent, sCurrentVungleConsentMessageVersion);
    }
  }

  public static Vungle.Consent getCurrentVungleConsent() {
    return sCurrentVungleConsent;
  }

  public static String getCurrentVungleConsentMessageVersion() {
    return sCurrentVungleConsentMessageVersion;
  }

  /**
   * Update CCPA consent status.
   * @param status See {@link Vungle.Consent}.
   *               If true, the user has consented to us gathering data about their device.
   */
  public static void setCCPAStatus(Vungle.Consent status) {
    Vungle.updateCCPAStatus(status);
  }

  /**
   * Whether a user has Accepted CCPA Consent.
   * @return null if user has not called earlier with {@link VungleConsent#setCCPAStatus(Vungle.Consent)}.
   */
  public static Vungle.Consent getCCPAStatus() {
    return Vungle.getCCPAStatus();
  }
}

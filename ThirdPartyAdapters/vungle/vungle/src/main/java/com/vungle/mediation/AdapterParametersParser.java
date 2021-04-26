package com.vungle.mediation;

import android.os.Bundle;
import android.util.Log;

/**
 * The {@link AdapterParametersParser} class helps in creating a Vungle network-specific
 * parameters.
 */
public class AdapterParametersParser {

  private static final String TAG = VungleManager.class.getSimpleName();

  public static class Config {

    private String appId;
    private String requestUniqueId;
    private String userId;

    public String getAppId() {
      return appId;
    }

    public String getRequestUniqueId() {
      return requestUniqueId;
    }

    public String getUserId() {
      return userId;
    }
  }

  public static Config parse(Bundle networkExtras, Bundle serverParameters)
      throws IllegalArgumentException {
    String appId = serverParameters.getString("appid");
    if (appId == null || appId.isEmpty()) {
      String message = "Vungle app ID should be specified!";
      Log.e(TAG, message);
      throw new IllegalArgumentException(message);
    }

    String uuid = null;
    if (networkExtras != null && networkExtras.containsKey(VungleExtrasBuilder.UUID_KEY)) {
      uuid = networkExtras.getString(VungleExtrasBuilder.UUID_KEY);
    }

    String userId = null;
    if (networkExtras != null && networkExtras.containsKey(VungleExtrasBuilder.EXTRA_USER_ID)) {
      userId = networkExtras.getString(VungleExtrasBuilder.EXTRA_USER_ID);
    }

    Config ret = new Config();
    ret.appId = appId;
    ret.requestUniqueId = uuid;
    ret.userId = userId;

    return ret;
  }
}

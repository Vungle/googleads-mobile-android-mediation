package com.google.android.vungle.data;

import static com.google.android.vungle.SettingsActivity.KEY_ANDROID_ID_OPTED_OUT;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.vungle.AdUnit;
import com.google.android.vungle.AdUnit.AdType;
import com.google.android.vungle.SettingsActivity;
import com.google.android.vungle.Util;
import com.vungle.mediation.VungleConsent;
import java.util.ArrayList;

public class DataSource {

  private static final String API_HOST_KEY = "vungle_api_host";
  private static final String AD_UNITS_KEY = "DefaultActivity_adapter_2";

  private static final DataSource sInstance = new DataSource();

  private ArrayList<AdUnit> units = null;
  private SharedPreferences mPreferences;
  private Boolean coppaStatus;

  private DataSource() {
  }

  public void init(Context context) {
    if (mPreferences == null) {
      mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
  }

  public static synchronized DataSource getInstance() {
    return sInstance;
  }

  public void saveApiHost(String hostUrl) {
    mPreferences.edit().putString(API_HOST_KEY, hostUrl).apply();
  }

  public String getApiHost() {
    return mPreferences.getString(API_HOST_KEY, null);
  }

  public ArrayList<AdUnit> getBannerAdUnits() {
    if (units == null) {
      load();
    }
    ArrayList<AdUnit> unitList = new ArrayList<>();
    for (AdUnit unit : units) {
      if (unit.getType() == AdUnit.AdType.MREC || unit.getType() == AdUnit.AdType.Banner) {
        unitList.add(unit);
      }
    }
    return unitList;
  }

  public ArrayList<AdUnit> getDefaultSizeAdUnits() {
    if (units == null) {
      load();
    }
    ArrayList<AdUnit> unitList = new ArrayList<>();
    for (AdUnit unit : units) {
      if (unit.getType() == AdUnit.AdType.Interstitial
          || unit.getType() == AdType.RewardedAd) {
        unitList.add(unit);
      }
    }
    return unitList;
  }

  public void reset() {
    mPreferences.edit().putString(AD_UNITS_KEY, null).apply();
    load();
  }

  public ArrayList<AdUnit> getAllAdUnits() {
    if (units == null) {
      load();
    }
    return units;
  }

  public void save(ArrayList<AdUnit> units) {
    mPreferences.edit().putString(AD_UNITS_KEY, Util.adUnitsToJsonString(units)).apply();
  }

  public void add(AdUnit unit) {
    if (units == null) {
      load();
    }
    units.add(unit);
  }

  private void load() {
    units = Util.jsonStringToAdUnits(mPreferences.getString(AD_UNITS_KEY, null));
    if (units == null || units.isEmpty()) {
      units = new ArrayList<>();
      units.add(new AdUnit(BANNER_AD_UNIT_ID,
          AdUnit.AdType.Banner, false, "Waterfall"));
      units.add(new AdUnit("ca-app-pub-6614121354918432/7678151874",
          AdUnit.AdType.Banner, true, "Realtime"));
      units.add(new AdUnit("ca-app-pub-6614121354918432/1171179857",
          AdUnit.AdType.Banner, true, "Bidding"));

      units.add(new AdUnit("ca-app-pub-6614121354918432/9049669873",
          AdUnit.AdType.MREC, false, "Waterfall"));
      units.add(new AdUnit("ca-app-pub-6614121354918432/8496409527",
          AdUnit.AdType.MREC, true, "Realtime"));
      units.add(new AdUnit("ca-app-pub-6614121354918432/5364548068",
          AdUnit.AdType.MREC, true, "Bidding"));

      units.add(new AdUnit("ca-app-pub-6614121354918432/1616874748",
          AdUnit.AdType.Interstitial, false, "Waterfall"));
      units.add(new AdUnit("ca-app-pub-6614121354918432/5713145469",
          AdUnit.AdType.Interstitial, true, "Realtime"));
      units.add(new AdUnit("ca-app-pub-6614121354918432/7858849467",
          AdUnit.AdType.Interstitial, true, "Bidding"));

      units.add(new AdUnit("ca-app-pub-6614121354918432/9303793073",
          AdUnit.AdType.RewardedAd, false, "Waterfall"));
      units.add(new AdUnit("ca-app-pub-6614121354918432/4137489977",
          AdType.RewardedAd, true, "Realtime"));
      units.add(new AdUnit("ca-app-pub-6614121354918432/2484261522",
          AdType.RewardedAd, true, "Bidding"));

      units.add(new AdUnit("ca-app-pub-6614121354918432/8272155345",
          AdType.RewardedInterstitial, false, "Waterfall"));
      units.add(new AdUnit("ca-app-pub-6614121354918432/1838629698",
          AdType.RewardedInterstitial, true, "Realtime"));
      units.add(new AdUnit("ca-app-pub-6614121354918432/8155339923",
          AdType.RewardedInterstitial, true, "Bidding"));

      units.add(new AdUnit(NATIVE_AD_UNIT_ID,
          AdType.Native, false, "Waterfall"));
      units.add(new AdUnit("ca-app-pub-6614121354918432/5714469253",
          AdType.Native, true, "Realtime"));
      units.add(new AdUnit("ca-app-pub-6614121354918432/2888806382",
          AdType.Native, true, "Bidding"));
    }
  }

  public static final String NATIVE_AD_UNIT_ID = "ca-app-pub-6614121354918432/9854460325";
  public static final String BANNER_AD_UNIT_ID = "ca-app-pub-6614121354918432/8182283094";

  public Bundle getVungleExtras() {
    Bundle bundle = new Bundle();
    bundle.putString(VungleMediationAdapter.KEY_USER_ID,
        mPreferences.getString(SettingsActivity.KEY_PREF_USER_ID, null));
    bundle.putInt(VungleMediationAdapter.KEY_ORIENTATION,
        Util.parseInt(mPreferences.getString(SettingsActivity.KEY_PREF_AD_ORIENTATION, "2"), 2));

    return bundle;
  }

  public void setupVungleNetworkSettings() {
    boolean isAndroidIdOptedOut = mPreferences.getBoolean(KEY_ANDROID_ID_OPTED_OUT, false);
    VungleConsent.publishAndroidId(!isAndroidIdOptedOut);
  }

  public void setCoppaStatus(Boolean status) {
    this.coppaStatus = status;
  }

  public Boolean getCoppaStatus() {
    return coppaStatus;
  }
}

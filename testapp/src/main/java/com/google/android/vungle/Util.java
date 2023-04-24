package com.google.android.vungle;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vungle.mediation.BuildConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.ceil;

/**
 * Created by andrey on 6/15/17.
 */

public class Util {
    private static String TAG = Util.class.getSimpleName();

    static public String adUnitsToJsonString(List<AdUnit> object) {
        return new Gson().toJson(object);
    }

    @SuppressWarnings("unchecked")
    public static <T> T jsonStringToAdUnits(String string) {
        if (string != null) {
            try {
                Type founderListType = new TypeToken<ArrayList<AdUnit>>(){}.getType();
                return new Gson().fromJson(string, founderListType);
            } catch (Throwable t) {
                Log.e(TAG, "Error on parsing ", t);
            }
        }
        return null;
    }

    public static void showSoftKeyboard(View focusable) {
        focusable.requestFocus();
        InputMethodManager imm = (InputMethodManager) focusable.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

//    public static void hideSoftKeyboard(View focusable) {
//        focusable.clearFocus();
//        InputMethodManager imm = (InputMethodManager) focusable.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.hideSoftInputFromWindow(focusable.getWindowToken(), 0);
//    }

    public static String getGooglePlayServicesVersion(Context ctx) {
        try {
            return ctx.getPackageManager()
                    .getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0)
                    .versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(ctx.getApplicationContext().getPackageName(), "getGooglePlayServicesVersion failed", e);
            return "";
        }
    }

    public static String getVersion(Context ctx) {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(ctx.getApplicationContext().getPackageName(), "getVersion failed", e);
            return "";
        }
    }

    public static String getAdapterVersion() {
        return BuildConfig.ADAPTER_VERSION;
    }

    public static String getSdkVersion() {
      try {
        Class<?> cls = Class.forName("com.vungle.ads.BuildConfig");
        Field field = cls.getDeclaredField("VERSION_NAME");
        field.setAccessible(true);
        return (String) field.get(null);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return "";
    }

    public static int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getDeviceID(Context ctx) {
        String android_id = Settings.Secure.getString(ctx.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        return md5(android_id).toUpperCase();
    }

    private static final String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException ignored) {
        }
        return "";
    }

    public static String getVersionInfo(Context context) {
        final String versions = String.format(context.getString(R.string.format_about),
                Util.getVersion(context), Util.getAdapterVersion(),
                Util.getSdkVersion(),
                Util.getGooglePlayServicesVersion(context));
        return versions;
    }

    public static int px2dip(Context context, float pxValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) ceil((pxValue / scale));
    }
}

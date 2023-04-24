package com.google.android.vungle;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import androidx.appcompat.app.AppCompatActivity;
import com.vungle.mediation.VungleConsent;


public class SettingsActivity extends AppCompatActivity {

    public static final String KEY_PREF_USER_ID = "pref_key_user_id";
    public static final String KEY_PREF_AD_ORIENTATION = "pref_key_ad_orientation";
    public static final String KEY_ANDROID_ID_OPTED_OUT = "pref_key_key_android_id_opted_out";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (KEY_ANDROID_ID_OPTED_OUT.equalsIgnoreCase(key)) {
                boolean isAndroidIdOptedOut = sharedPreferences.getBoolean(KEY_ANDROID_ID_OPTED_OUT, false);
                VungleConsent.publishAndroidId(!isAndroidIdOptedOut);
            }

        }
    }

}

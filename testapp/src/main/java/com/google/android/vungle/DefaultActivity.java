package com.google.android.vungle;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.ads.mediationtestsuite.MediationTestSuite;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.vungle.banner.BannerAdFeedActivity;
import com.google.android.vungle.data.DataSource;
import com.google.android.vungle.nativeads.NativeAdFeedActivity;
import com.google.android.vungle.nativeads.advanced.NativeAdFeedAdvancedActivity;
import com.vungle.mediation.VungleConsent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DefaultActivity extends AppCompatActivity {

    private UnitsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);

        DataSource.getInstance().init(getApplicationContext());

        List<String> testDeviceIds = Arrays.asList(Util.getDeviceID(DefaultActivity.this));
        RequestConfiguration configuration =
                new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds)
                        .setTagForChildDirectedTreatment(
                                DataSource.getInstance().getCoppaStatus() == null ? RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED :
                                        DataSource.getInstance().getCoppaStatus() ? RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE :
                                                RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
                        )
                        .build();

        MobileAds.setRequestConfiguration(configuration);

        MobileAds.initialize(this, initializationStatus -> {
            Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
            for (String adapterClass : statusMap.keySet()) {
                AdapterStatus status = statusMap.get(adapterClass);
                if (adapter != null) {
                    adapter.log(String.format(
                            "Adapter name: %s, Description: %s, Latency: %d",
                            adapterClass, status.getDescription(), status.getLatency()));
                }
            }
        });

        RecyclerView list = findViewById(R.id.list);
        adapter = new UnitsAdapter(new ArrayList<>());
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        loadAdUnits();

        adapter.setUnitChangeListener(new UnitsAdapter.UnitChangeListener() {
            @Override
            public void createRequest() {
                final AddUnitDialog dlg = AddUnitDialog.newInstance();
                dlg.setListener(unit -> {
                    for (AdUnit el : adapter.getUnits()) {
                        if (el.getId().equals(unit.getId()) && el.getType().equals(unit.getType())) {
                            return "Ad unit already exists";
                        }
                    }
                    dlg.dismiss();
                    adapter.getUnits().add(unit);
                    adapter.notifyItemInserted(adapter.getUnits().size());
                    DataSource.getInstance().add(unit);
                    return null;
                });
                dlg.show(getSupportFragmentManager(), null);
            }

            @Override
            public void loadAdRequest(final AdUnit unit) {
                AdProvider.getInstance(DefaultActivity.this).loadAd(DefaultActivity.this, adapter, unit);
            }

            @Override
            public void playAdRequest(final AdUnit unit) {
                AdProvider.getInstance(DefaultActivity.this).playAd(DefaultActivity.this, adapter, unit);
            }

            @Override
            public void launchBanner(AdUnit unit) {
                Intent intent = new Intent(DefaultActivity.this, BannerActivity.class);
                Bundle arguments = new Bundle();
                arguments.putParcelable(BannerActivity.AD_UNIT, unit);
                intent.putExtras(arguments);
                DefaultActivity.this.startActivity(intent);
            }

            @Override
            public void launchNative(AdUnit unit) {
                Intent intent = new Intent(DefaultActivity.this, NativeAdActivity.class);
                Bundle arguments = new Bundle();
                arguments.putParcelable(NativeAdActivity.AD_UNIT, unit);
                intent.putExtras(arguments);
                DefaultActivity.this.startActivity(intent);
            }

            @Override
            public void reset() {
                DataSource.getInstance().reset();
                loadAdUnits();
            }
        });

        DataSource.getInstance().setupVungleNetworkSettings();

        adapter.log(String
                .format(getString(R.string.format_about), Util.getVersion(this), Util.getAdapterVersion(),
                        Util.getSdkVersion(), Util.getGooglePlayServicesVersion(this)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_about) {
            onAbout();
            return true;
        } else if (id == R.id.action_settings) {
            onSettings();
            return true;
        } else if (id == R.id.action_test) {
            launchTestSuite();
            return true;
        } else if (id == R.id.action_update_ccpa) {
            updateCCPA();
            return true;
        } else if (id == R.id.action_update_gdpr) {
            updateGDPR();
            return true;
        } else if (id == R.id.action_multiple_native_ad) {
            startActivity(new Intent(this, NativeAdFeedActivity.class));
            return true;
        } else if (id == R.id.action_multiple_native_ad_advanced) {
            startActivity(new Intent(this, NativeAdFeedAdvancedActivity.class));
            return true;
        } else if (id == R.id.action_multiple_banner_ad) {
            startActivity(new Intent(this, BannerAdFeedActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateGDPR() {
    String gdprStatus = VungleConsent.getGDPRStatus();
    new AlertDialog.Builder(this)
        .setTitle(getString(R.string.update_consent))
        .setMessage(
                "This simulates publisher-provided consent for gathering privacy related data. " +
                    "Do you allow Vungle to collect information about it?\n" +
                    "Current GDPR status: " + gdprStatus)
        .setPositiveButton(R.string.yes, (dialogInterface, i) ->
                VungleConsent.setGDPRStatus(true, ""))
        .setNegativeButton(R.string.no, (dialogInterface, i) ->
            VungleConsent.setGDPRStatus(false, ""))
        .show();
    }

    private void updateCCPA() {
    String ccpaStatus = VungleConsent.getCcpaStatus();
    new AlertDialog.Builder(this)
        .setTitle(getString(R.string.action_update_ccpa))
        .setMessage(
                "This simulates publisher-provided consent for gathering privacy related data. " +
                    "Do you allow Vungle to collect information about it?\n" +
                    "Current CCPA status: " + ccpaStatus)
        .setPositiveButton(R.string.yes, (dialogInterface, i) ->
            VungleConsent.setCCPAStatus(true))
        .setNegativeButton(R.string.no, (dialogInterface, i) ->
            VungleConsent.setCCPAStatus(false))
        .show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App?")
                .setMessage("Do you want to exit the AdMob Adapter Testing app?")
                .setPositiveButton("Exit", (dialog, i) -> super.onBackPressed())
                .setNegativeButton(R.string.cancel, (dialog, i) -> dialog.dismiss())
                .show();
    }

    private void onAbout() {
        final String message = String.format(getString(R.string.format_about),
                Util.getVersion(this), Util.getAdapterVersion(),
                Util.getSdkVersion(),
                Util.getGooglePlayServicesVersion(this));
        new AlertDialog.Builder(this)
                .setTitle(R.string.label_about)
                .setMessage(message)
                .setNegativeButton(R.string.label_ok, null)
                .show();
    }

    private void onSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void launchTestSuite() {
        MediationTestSuite.launch(this);
    }

    @Override
    protected void onDestroy() {
        AdProvider.getInstance(this).onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        DataSource.getInstance().save(adapter.getUnits());
        super.onPause();
    }

    private void loadAdUnits() {
        ArrayList<AdUnit> units = DataSource.getInstance().getAllAdUnits();
        adapter.getUnits().clear();
        adapter.getUnits().addAll(units);
        adapter.notifyDataSetChanged();
    }

}

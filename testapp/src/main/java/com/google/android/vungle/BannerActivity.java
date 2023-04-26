package com.google.android.vungle;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.vungle.data.DataSource;
import com.vungle.mediation.VungleInterstitialAdapter;
import java.util.Calendar;

public class BannerActivity extends AppCompatActivity implements OnClickListener {

  final static String AD_UNIT = "adUnitId";

  private TextView unitIdTV;
  private TextView adSizeTV;
  private ViewGroup adViewContainer;
  private ProgressBar progressBar;
  private AdView adView;
  private TextView adLogs;

  private boolean isManualLand = false;

  private boolean refreshFlag;
  private AdUnit adUnit;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_banner);

    Intent intent = getIntent();
    if ((intent == null || null == (adUnit = intent.getParcelableExtra(AD_UNIT)))) {
      finish();
      return;
    }

    unitIdTV = findViewById(R.id.unitIdTV);
    adSizeTV = findViewById(R.id.adSizeTV);
    Button playAdBtn = findViewById(R.id.btn_play_ad);
    Button finishAdBtn = findViewById(R.id.btn_finish_banner_ad);
    adViewContainer = findViewById(R.id.rl_container);
    progressBar = findViewById(R.id.pb_load);
    adLogs = findViewById(R.id.text_log);
    adLogs.setMovementMethod(new ScrollingMovementMethod());
    playAdBtn.setOnClickListener(this);
    finishAdBtn.setOnClickListener(this);

    if (landscapeScreen(adUnit)) {
      isManualLand = true;
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
      return;
    }

    requestBanner(adUnit);
  }

  @Override
  public void onConfigurationChanged(@NonNull Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (isManualLand && adUnit != null) {
      isManualLand = false;
      requestBanner(adUnit);
    }
  }

  private void requestBanner(AdUnit adUnit) {
    if (adView != null) {
      adView.destroy();
      adViewContainer.removeView(adView);
    }
    Context context = getApplicationContext();
    adView = new AdView(context);
    adView.setAdListener(new CustomAdListener());
    adViewContainer.addView(adView);
    unitIdTV.setText(
        getString(R.string.ad_unitId_title, adUnit.getId(), "" + adUnit.isOpenBidding()));
    adLogs.setText("AdLogs:\n");
    adView.setAdUnitId(adUnit.getId());
    AdSize adSize = getBannerAdSize(adUnit);
    adView.setAdSize(adSize);
    int width = Util.px2dip(context, adSize.getWidthInPixels(context));
    int height = Util.px2dip(context, adSize.getHeightInPixels(context));
    adSizeTV.setText("AdSize:" + adView.getAdSize() + " (" + width + "x" + height + ")");

    playAd();
  }

  @Override
  protected void onPause() {
    if (adView != null) {
      adView.pause();
    }
    super.onPause();
  }

  @Override
  protected void onResume() {
    if (adView != null) {
      adView.resume();
    }
    super.onResume();
  }

  @Override
  protected void onDestroy() {
    if (adView != null) {
      adView.destroy();
    }
    super.onDestroy();
  }

  //get banner ad size
  private AdSize getBannerAdSize(@NonNull AdUnit adUnit) {
    AdSize adSize;
    UnitsAdapter.AdSizeSpinnerWrapper adSizeWrapper = adUnit.getAdSizeWrapper();
    if (adSizeWrapper == null) {
      switch (adUnit.getType()) {
        case MREC:
          return AdSize.MEDIUM_RECTANGLE;
        case Banner:
          return AdSize.BANNER;
        default:
          return AdSize.INVALID;
      }
    }

    if (adSizeWrapper.isAdaptiveBanner()) {
      String adaptiveBannerSize = adSizeWrapper.getOtherSize();
      Display display = getWindowManager().getDefaultDisplay();
      DisplayMetrics outMetrics = new DisplayMetrics();
      display.getMetrics(outMetrics);
      float widthPixels = outMetrics.widthPixels;
      float density = outMetrics.density;
      int adWidth = (int) (widthPixels / density);
      if (adaptiveBannerSize.endsWith("PORTRAIT")) {
        adSize = AdSize.getPortraitAnchoredAdaptiveBannerAdSize(this, adWidth);
      } else if (adaptiveBannerSize.endsWith("LANDSCAPE")) {
        adSize = AdSize.getLandscapeAnchoredAdaptiveBannerAdSize(this, adWidth);
      } else {
        adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
      }
    } else if (adSizeWrapper.isArbitraryBanner()) {
      int width = 310;
      int height = 60;
      adSize = new AdSize(width, height);
    } else {
      adSize = adSizeWrapper.getSize();
    }
    return adSize;
  }

  // Whether need to landscape screen for current banner ad
  private boolean landscapeScreen(@NonNull AdUnit adUnit) {
    UnitsAdapter.AdSizeSpinnerWrapper adSizeWrapper = adUnit.getAdSizeWrapper();
    if (adSizeWrapper == null) {
      return false;
    }
    Configuration mConfiguration = getResources().getConfiguration();
    return (AdSize.LEADERBOARD.equals(adSizeWrapper.getSize()) || adSizeWrapper
        .isAdaptiveBannerLandscape()) &&
        !getResources().getBoolean(R.bool.isTablet) &&
        mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT;
  }

  private void playAd() {
    refreshFlag = false;
    if (adView.getVisibility() != View.VISIBLE) {
      adView.setVisibility(View.VISIBLE);
    }
    progressBar.setVisibility(View.VISIBLE);
    Bundle vungleExtras = DataSource.getInstance().getVungleExtras();
    AdRequest adRequest;
    if (adUnit.isOpenBidding()) {
      adRequest = new AdRequest.Builder()
          .addNetworkExtrasBundle(VungleMediationAdapter.class, vungleExtras)
          .build();
    } else {
      adRequest = new AdRequest.Builder()
          .addNetworkExtrasBundle(VungleInterstitialAdapter.class, vungleExtras)
          .build();
    }
    adView.loadAd(adRequest);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btn_play_ad:
        requestBanner(adUnit);
        break;
      case R.id.btn_finish_banner_ad:
        progressBar.setVisibility(View.GONE);
        if (adView != null) {
          adView.setVisibility(View.GONE);
          adView.destroy();
          adViewContainer.removeView(adView);
          adView = null;
        }
        break;
    }
  }

  private class CustomAdListener extends AdListener {

    @Override
    public void onAdLoaded() {
      progressBar.setVisibility(View.GONE);
      if (!refreshFlag) {
        adLogs.append(getCurrentTime() + " " + getString(R.string.started) + " - "
            + getString(R.string.new_banner_ad) + "\n");
      } else {
        adLogs.append(getCurrentTime() + " " + getString(R.string.started) + " - "
            + getString(R.string.refresh_new_banner_ad) + "\n");
      }
      refreshFlag = true;
    }

    @Override
    public void onAdFailedToLoad(LoadAdError error) {
      progressBar.setVisibility(View.GONE);
      adLogs.append(getCurrentTime() + " " + getString(R.string.error) + ": " + error + "\n");
    }

    @Override
    public void onAdOpened() {
      adLogs.append(getCurrentTime() + " AdOpened" + "\n");
    }

    @Override
    public void onAdClosed() {
      adLogs.append(getCurrentTime() + " AdClosed" + "\n");
    }

    @Override
    public void onAdClicked() {
      adLogs.append(getCurrentTime() + " onAdClicked" + "\n");
    }

    @Override
    public void onAdImpression() {
      adLogs.append(getCurrentTime() + " onAdImpression" + "\n");
    }
  }

  private String getCurrentTime() {
    return String.format("%02d", Calendar.getInstance().get(Calendar.HOUR)) + ":" +
        String.format("%02d", Calendar.getInstance().get(Calendar.MINUTE)) + ":" +
        String.format("%02d", Calendar.getInstance().get(Calendar.SECOND));
  }
}

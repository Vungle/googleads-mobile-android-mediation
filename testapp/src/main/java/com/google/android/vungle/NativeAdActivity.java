package com.google.android.vungle;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import java.util.Calendar;

public class NativeAdActivity extends AppCompatActivity {

  final static String AD_UNIT = "adUnitId";

  private TextView adLogsTV;

  private String adUnitId;
  private NativeAd nativeAd;
  private Button refreshAdBtn, destroyAdBtn;

  private SharedPreferences mPreferences;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_native);

    Intent intent = getIntent();

    AdUnit adUnit;
    if ((intent == null || null == (adUnit = intent.getParcelableExtra(AD_UNIT)))) {
      finish();
      return;
    }

    mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    adLogsTV = findViewById(R.id.ad_logs);
    refreshAdBtn = findViewById(R.id.refreshAdBtn);
    destroyAdBtn = findViewById(R.id.destroyAdBtn);
    destroyAdBtn.setVisibility(View.INVISIBLE);

    adUnitId = adUnit.getId();

    int adChoicesPlacement = Util
        .parseInt(mPreferences.getString(getString(R.string.pref_key_options_position),
            "" + NativeAdOptions.ADCHOICES_TOP_RIGHT), NativeAdOptions.ADCHOICES_TOP_RIGHT);

    final AdLoader adLoader = new AdLoader.Builder(this, adUnitId)
        .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
          @Override
          public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
            refreshAdBtn.setVisibility(View.VISIBLE);
//            destroyAdBtn.setVisibility(View.VISIBLE);

            NativeAdActivity.this.nativeAd = nativeAd;
            FrameLayout nativeContainer = findViewById(R.id.native_container);
            NativeAdView adView = (NativeAdView) getLayoutInflater()
                .inflate(R.layout.admob_native_ad, null);
            populateNativeAdView(nativeAd, adView);
            nativeContainer.removeAllViews();
            nativeContainer.addView(adView);
          }
        })
        .withAdListener(new AdMobAdListener())
        .withNativeAdOptions(new NativeAdOptions.Builder()
            .setAdChoicesPlacement(adChoicesPlacement)
            .build())
        .build();

    refreshAdBtn.setOnClickListener(view -> {
      adLogsTV.append(getCurrentTime() + " Loading...\n");
      refreshAdBtn.setVisibility(View.INVISIBLE);
//      destroyAdBtn.setVisibility(View.INVISIBLE);
      adLoader.loadAd(new AdRequest.Builder().build());
    });

    destroyAdBtn.setOnClickListener(view -> {
      if (nativeAd != null) {
        nativeAd.destroy();
      }
    });

    refreshAdBtn.performClick();
  }

  private class AdMobAdListener extends AdListener {

    @Override
    public void onAdClosed() {
      adLogsTV.append(getCurrentTime() + " " + getString(R.string.ad_closed) + "\n");
    }

    @Override
    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
      adLogsTV.append(
          getCurrentTime() + " " + getString(R.string.load_error) + ": " + loadAdError + "\n");
    }

    @Override
    public void onAdOpened() {
      adLogsTV.append(getCurrentTime() + " " + getString(R.string.ad_opened) + "\n");
    }

    @Override
    public void onAdLoaded() {
      adLogsTV.append(getCurrentTime() + " " + getString(R.string.ad_loaded) + "\n");
    }

    @Override
    public void onAdClicked() {
      adLogsTV.append(getCurrentTime() + " " + getString(R.string.ad_clicked) + "\n");
    }

    @Override
    public void onAdImpression() {
      adLogsTV.append(getCurrentTime() + " " + getString(R.string.ad_impression) + "\n");
    }
  }

  private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
    // Set the media view. Media content will be automatically populated in the media view once
    // adView.setNativeAd() is called.
    MediaView mediaView = adView.findViewById(R.id.ad_media);
    adView.setMediaView(mediaView);

    // Set other ad assets.
    adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
    adView.setBodyView(adView.findViewById(R.id.ad_body));
    adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
    adView.setIconView(adView.findViewById(R.id.ad_app_icon));
    adView.setPriceView(adView.findViewById(R.id.ad_price));
    adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
    adView.setStoreView(adView.findViewById(R.id.ad_store));
    adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

    // The headline is guaranteed to be in every NativeAd.
    ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());

    // These assets aren't guaranteed to be in every NativeAd, so it's important to
    // check before trying to display them.
    if (nativeAd.getBody() == null) {
      adView.getBodyView().setVisibility(View.INVISIBLE);
    } else {
      adView.getBodyView().setVisibility(View.VISIBLE);
      ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
    }

    if (nativeAd.getCallToAction() == null) {
      adView.getCallToActionView().setVisibility(View.INVISIBLE);
    } else {
      adView.getCallToActionView().setVisibility(View.VISIBLE);
      ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
    }

    if (nativeAd.getIcon() == null) {
      adView.getIconView().setVisibility(View.GONE);
    } else {
      ((ImageView) adView.getIconView()).setImageURI(
          nativeAd.getIcon().getUri());
      adView.getIconView().setVisibility(View.VISIBLE);
    }

    if (nativeAd.getPrice() == null) {
      adView.getPriceView().setVisibility(View.INVISIBLE);
    } else {
      adView.getPriceView().setVisibility(View.VISIBLE);
      ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
    }

    if (nativeAd.getStore() == null) {
      adView.getStoreView().setVisibility(View.INVISIBLE);
    } else {
      adView.getStoreView().setVisibility(View.VISIBLE);
      ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
    }

    if (nativeAd.getStarRating() == null || nativeAd.getStarRating() < 3) {
      adView.getStarRatingView().setVisibility(View.INVISIBLE);
    } else {
      ((RatingBar) adView.getStarRatingView())
          .setRating(nativeAd.getStarRating().floatValue());
      adView.getStarRatingView().setVisibility(View.VISIBLE);
    }

    if (nativeAd.getAdvertiser() == null) {
      adView.getAdvertiserView().setVisibility(View.INVISIBLE);
    } else {
      ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
      adView.getAdvertiserView().setVisibility(View.VISIBLE);
    }

    // This method tells the Google Mobile Ads SDK that you have finished populating your
    // native ad view with this native ad. The SDK will populate the adView's MediaView
    // with the media content from this native ad.
    adView.setNativeAd(nativeAd);
  }

  private String getCurrentTime() {
    return String.format("%02d", Calendar.getInstance().get(Calendar.HOUR)) + ":" +
        String.format("%02d", Calendar.getInstance().get(Calendar.MINUTE)) + ":" +
        String.format("%02d", Calendar.getInstance().get(Calendar.SECOND));
  }

  @Override
  protected void onDestroy() {
    if (nativeAd != null) {
      nativeAd.destroy();
    }
    super.onDestroy();
  }
}

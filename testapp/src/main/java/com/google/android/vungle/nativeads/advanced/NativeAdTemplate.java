package com.google.android.vungle.nativeads.advanced;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAd.OnNativeAdLoadedListener;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.vungle.R;
import java.util.concurrent.atomic.AtomicBoolean;

public class NativeAdTemplate {

  private Context context;
  private String unitId;

  private NativeAd adMobNativeAd;
  private NativeAdView adView;

  // Optional for debug
  private String tag;

  private AtomicBoolean called = new AtomicBoolean(false);

  public NativeAdTemplate(Context context, String nativeAdUnitId) {
    this.context = context;
    this.unitId = nativeAdUnitId;

    adView = (NativeAdView) LayoutInflater.from(context)
        .inflate(R.layout.native_ad_view_advanced, null);
    adView.setMediaView(adView.findViewById(R.id.ad_media));
    adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
    adView.setBodyView(adView.findViewById(R.id.ad_body));
    adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
    adView.setIconView(adView.findViewById(R.id.ad_icon));
    adView.setPriceView(adView.findViewById(R.id.ad_price));
    adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
    adView.setStoreView(adView.findViewById(R.id.ad_store));
    adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
  }

  public void destroy() {
    if (adMobNativeAd != null) {
      adMobNativeAd.destroy();
      adMobNativeAd = null;
    }
  }

  public void loadNativeAd(LoadCallback callback) {
    AdLoader adLoader = new AdLoader.Builder(context, unitId)
        .forNativeAd(new OnNativeAdLoadedListener() {
          @Override
          public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
            adMobNativeAd = nativeAd;
            callback.onSuccess();
          }
        })
        .withAdListener(new AdListener() {
          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            super.onAdFailedToLoad(loadAdError);
            callback.onFailure(loadAdError);
          }
        })
        .withNativeAdOptions(new NativeAdOptions.Builder().build())
        .build();
    adLoader.loadAd(new AdRequest.Builder().build());
  }

  public void populateNativeAdView(ViewGroup rootView) {
    if (adMobNativeAd == null) {
      return;
    }

    if (adView.getParent() != null) {
      ((ViewGroup) adView.getParent()).removeView(adView);
    }
    rootView.addView(adView);

    // Some assets are guaranteed to be in every UnifiedNativeAd.
    ((TextView) adView.getHeadlineView()).setText(adMobNativeAd.getHeadline());
    ((TextView) adView.getBodyView()).setText(adMobNativeAd.getBody());
    ((Button) adView.getCallToActionView()).setText(adMobNativeAd.getCallToAction());

    // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
    // check before trying to display them.
    NativeAd.Image icon = adMobNativeAd.getIcon();

    if (icon == null) {
      adView.getIconView().setVisibility(View.INVISIBLE);
    } else {
      ((ImageView) adView.getIconView()).setImageURI(icon.getUri());
      adView.getIconView().setVisibility(View.VISIBLE);
    }

    if (adMobNativeAd.getPrice() == null) {
      adView.getPriceView().setVisibility(View.INVISIBLE);
    } else {
      adView.getPriceView().setVisibility(View.VISIBLE);
      ((TextView) adView.getPriceView()).setText(adMobNativeAd.getPrice());
    }

    if (adMobNativeAd.getStore() == null) {
      adView.getStoreView().setVisibility(View.INVISIBLE);
    } else {
      adView.getStoreView().setVisibility(View.VISIBLE);
      ((TextView) adView.getStoreView()).setText(adMobNativeAd.getStore());
    }

    if (adMobNativeAd.getStarRating() == null) {
      adView.getStarRatingView().setVisibility(View.INVISIBLE);
    } else {
      ((RatingBar) adView.getStarRatingView())
          .setRating(adMobNativeAd.getStarRating().floatValue());
      adView.getStarRatingView().setVisibility(View.VISIBLE);
    }

    if (adMobNativeAd.getAdvertiser() == null) {
      adView.getAdvertiserView().setVisibility(View.INVISIBLE);
    } else {
      ((TextView) adView.getAdvertiserView()).setText(adMobNativeAd.getAdvertiser());
      adView.getAdvertiserView().setVisibility(View.VISIBLE);
    }

    // Assign native ad object to the native view.
    // For Vungle native ad in feed case we must call it only once, because if we call it more then once,
    // AdMob SDK will invoke adapter#untrackView() method, then vungle SDK will destroy native ad.
    if(!called.getAndSet(true)) {
      adView.setNativeAd(adMobNativeAd);
    }
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  interface LoadCallback {

    void onSuccess();

    void onFailure(LoadAdError error);
  }
}

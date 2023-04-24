package com.google.android.vungle.nativeads;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.vungle.R;
import java.util.ArrayList;
import java.util.List;

public class AdmobNativeAdAdapter extends RecyclerViewAdapterWrapper {

  private static final String TAG = AdmobNativeAdAdapter.class.getSimpleName();

  private static final int TYPE_FB_NATIVE_ADS = 1;
  private static final int DEFAULT_AD_ITEM_INTERVAL = 16;

  private final Param mParam;

  private final List<NativeAd> nativeAdList = new ArrayList<>();

  private AdmobNativeAdAdapter(Param param) {
    super(param.adapter);
    this.mParam = param;
  }

  private int convertAdPosition2OrgPosition(int position) {
    return position - (position + 1) / (mParam.adItemInterval + 1);
  }

  public void destroy() {
    for (NativeAd nativeAd : nativeAdList) {
      nativeAd.destroy();
    }
  }

  @Override
  public int getItemCount() {
    int realCount = super.getItemCount();
    return realCount + realCount / mParam.adItemInterval;
  }

  @Override
  public int getItemViewType(int position) {
    if (isAdPosition(position)) {
      return TYPE_FB_NATIVE_ADS;
    }
    return super.getItemViewType(convertAdPosition2OrgPosition(position));
  }

  private boolean isAdPosition(int position) {
    return (position + 1) % (mParam.adItemInterval + 1) == 0;
  }

  private void onBindAdViewHolder(final RecyclerView.ViewHolder holder) {
    final AdViewHolder adHolder = (AdViewHolder) holder;

    if (mParam.forceReloadAdOnBind || !adHolder.loaded) {
      adHolder.adView.setVisibility(View.INVISIBLE);
      adHolder.adLoadingView.setVisibility(View.VISIBLE);
      AdLoader adLoader = new AdLoader.Builder(adHolder.getContext(), mParam.admobNativeId)
          .forNativeAd(nativeAd -> {
            Log.e(TAG, "loaded");
            nativeAdList.add(nativeAd);

            adHolder.adView.setVisibility(View.VISIBLE);
            adHolder.adLoadingView.setVisibility(View.GONE);

            populateNativeAdView(nativeAd, ((AdViewHolder) holder).getAdView());

            adHolder.loaded = true;

          })
          .withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
              super.onAdFailedToLoad(loadAdError);
              Log.e(TAG, "error:" + loadAdError.getMessage());
              adHolder.adContainer.setVisibility(View.GONE);
            }

          })
          .withNativeAdOptions(new NativeAdOptions.Builder()
              .build())
          .build();

      adLoader.loadAd(
          new AdRequest.Builder().build());
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (getItemViewType(position) == TYPE_FB_NATIVE_ADS) {
      onBindAdViewHolder(holder);
    } else {
      super.onBindViewHolder(holder, convertAdPosition2OrgPosition(position));
    }
  }

  private RecyclerView.ViewHolder onCreateAdViewHolder(ViewGroup parent) {
    View nativeLayoutView = LayoutInflater.from(
        parent.getContext()).inflate(R.layout.native_ad_view, parent, false);
    return new AdViewHolder(nativeLayoutView);
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == TYPE_FB_NATIVE_ADS) {
      return onCreateAdViewHolder(parent);
    }
    return super.onCreateViewHolder(parent, viewType);
  }

  private static class Param {

    String admobNativeId;
    RecyclerView.Adapter<RecyclerView.ViewHolder> adapter;
    int adItemInterval;
    boolean forceReloadAdOnBind;
  }

  public static class Builder {

    private final Param mParam;

    private Builder(Param param) {
      mParam = param;
    }

    public static Builder with(String placementId,
        RecyclerView.Adapter<RecyclerView.ViewHolder> wrapped) {
      Param param = new Param();
      param.admobNativeId = placementId;
      param.adapter = wrapped;

      //default value
      param.adItemInterval = DEFAULT_AD_ITEM_INTERVAL;
      param.forceReloadAdOnBind = true;
      return new Builder(param);
    }

    public AdmobNativeAdAdapter build() {
      return new AdmobNativeAdAdapter(mParam);
    }

    public Builder forceReloadAdOnBind(boolean forced) {
      mParam.forceReloadAdOnBind = forced;
      return this;
    }
  }

  private static class AdViewHolder extends RecyclerView.ViewHolder {

    boolean loaded;
    private NativeAdView adView;
    private View adContainer;
    private View adLoadingView;

    public NativeAdView getAdView() {
      return adView;
    }

    public Context getContext() {
      return adView.getContext();
    }

    AdViewHolder(View view) {
      super(view);
      adContainer = view;

      adView = view.findViewById(R.id.ad_view);
      adLoadingView = view.findViewById(R.id.adLoadingView);

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
  }

  private void populateNativeAdView(NativeAd nativeAd,
      NativeAdView adView) {
    // Some assets are guaranteed to be in every UnifiedNativeAd.
    ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
    ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
    ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());

    // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
    // check before trying to display them.
    NativeAd.Image icon = nativeAd.getIcon();

    if (icon == null) {
      adView.getIconView().setVisibility(View.INVISIBLE);
    } else {
      ((ImageView) adView.getIconView()).setImageURI(icon.getUri());
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

    if (nativeAd.getStarRating() == null) {
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

    // Assign native ad object to the native view.
    adView.setNativeAd(nativeAd);
  }
}

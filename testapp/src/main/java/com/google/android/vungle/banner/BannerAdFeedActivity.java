package com.google.android.vungle.banner;

import static com.google.android.vungle.data.DataSource.BANNER_AD_UNIT_ID;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.vungle.R;
import com.google.android.vungle.nativeads.MenuItem;
import java.util.ArrayList;
import java.util.List;

// https://github.com/googleads/googleads-mobile-android-examples/tree/main/java/advanced/BannerRecyclerViewExample
public class BannerAdFeedActivity extends AppCompatActivity {

  // A banner ad is placed in every 16th position in the RecyclerView.
  public static final int ITEMS_PER_AD = 16;

  // The RecyclerView that holds and displays banner ads and menu items.
  private RecyclerView recyclerView;

  // List of banner ads and MenuItems that populate the RecyclerView.
  private List<Object> recyclerViewItems = new ArrayList<>();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_banner_feed);

    recyclerView = findViewById(R.id.recycler_view);

    // Use this setting to improve performance if you know that changes
    // in content do not change the layout size of the RecyclerView.
    recyclerView.setHasFixedSize(true);

    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
    recyclerView.setLayoutManager(layoutManager);

    // Update the RecyclerView item's list with menu items and banner ads.
    ArrayList<MenuItem> demoDatas = MenuItem.createDemoDataList(this);
    recyclerViewItems.addAll(demoDatas);
    addBannerAds();
    loadBannerAds();

    // Specify an adapter.
    RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new AdMobBannerAdAdapter(this,
        recyclerViewItems);
    recyclerView.setAdapter(adapter);
  }

  @Override
  protected void onResume() {
    for (Object item : recyclerViewItems) {
      if (item instanceof AdView) {
        AdView adView = (AdView) item;
        adView.resume();
      }
    }
    super.onResume();
  }

  @Override
  protected void onPause() {
    for (Object item : recyclerViewItems) {
      if (item instanceof AdView) {
        AdView adView = (AdView) item;
        adView.pause();
      }
    }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    for (Object item : recyclerViewItems) {
      if (item instanceof AdView) {
        AdView adView = (AdView) item;
        adView.destroy();
      }
    }
    recyclerViewItems.clear();
    super.onDestroy();
  }

  /**
   * Adds banner ads to the items list.
   */
  private void addBannerAds() {
    // Loop through the items array and place a new banner ad in every ith position in
    // the items List.
    for (int i = 0, j = 0; i <= recyclerViewItems.size(); i += ITEMS_PER_AD, j++) {
      final AdView adView = new AdView(BannerAdFeedActivity.this);
      adView.setAdSize(AdSize.BANNER);
      adView.setTag("BannerAdIndex:" + j);
      adView.setAdUnitId(BANNER_AD_UNIT_ID);
      recyclerViewItems.add(i, adView);
    }
  }

  /**
   * Sets up and loads the banner ads.
   */
  private void loadBannerAds() {
    // Load the first banner ad in the items list (subsequent ads will be loaded automatically
    // in sequence).
    loadBannerAd(0);
  }

  /**
   * Loads the banner ads in the items list.
   */
  private void loadBannerAd(final int index) {

    if (index >= recyclerViewItems.size()) {
      return;
    }

    Object item = recyclerViewItems.get(index);
    if (!(item instanceof AdView)) {
      throw new ClassCastException("Expected item at index " + index + " to be a banner ad"
          + " ad.");
    }

    final AdView adView = (AdView) item;

    // Set an AdListener on the AdView to wait for the previous banner ad
    // to finish loading before loading the next ad in the items list.
    adView.setAdListener(
        new AdListener() {
          @Override
          public void onAdLoaded() {
            super.onAdLoaded();
            // The previous banner ad loaded successfully, call this method again to
            // load the next ad in the items list.
            loadBannerAd(index + ITEMS_PER_AD);
          }

          @Override
          public void onAdFailedToLoad(LoadAdError loadAdError) {
            // The previous banner ad failed to load. Call this method again to load
            // the next ad in the items list.
            String error =
                String.format(
                    "domain: %s, code: %d, message: %s",
                    loadAdError.getDomain(), loadAdError.getCode(), loadAdError.getMessage());
            Log.e(
                "BannerAdFeedActivity",
                "The previous banner ad failed to load with error: "
                    + error
                    + ". Attempting to"
                    + " load the next banner ad in the items list.");
            loadBannerAd(index + ITEMS_PER_AD);
          }
        });

    // Load the banner ad.
    adView.loadAd(new AdRequest.Builder().build());
  }

}

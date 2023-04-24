package com.google.android.vungle.nativeads.advanced;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.vungle.R;
import com.google.android.vungle.data.DataSource;
import com.google.android.vungle.nativeads.MenuItem;
import com.google.android.vungle.nativeads.advanced.NativeAdTemplate.LoadCallback;
import java.util.ArrayList;
import java.util.List;

public class NativeAdFeedAdvancedActivity extends AppCompatActivity {

  // A native ad is placed in every 16th position in the RecyclerView.
  public static final int ITEMS_PER_AD = 16;

  // The RecyclerView that holds and displays native ads and menu items.
  private RecyclerView recyclerView;

  // List of native ads and MenuItems that populate the RecyclerView.
  private List<Object> recyclerViewItems = new ArrayList<>();

  private RecyclerView.Adapter<RecyclerView.ViewHolder> adapter;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_native_feed);

    recyclerView = findViewById(R.id.recycler_view);

    // Use this setting to improve performance if you know that changes
    // in content do not change the layout size of the RecyclerView.
    recyclerView.setHasFixedSize(true);

    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
    recyclerView.setLayoutManager(layoutManager);

    // Update the RecyclerView item's list with menu items and native ads.
    ArrayList<MenuItem> demoDatas = MenuItem.createDemoDataList(this);
    recyclerViewItems.addAll(demoDatas);
    addNativeAds();
    loadNativeAds();

    // Specify an adapter.
    adapter = new NativeAdAdvancedAdapter(this, recyclerViewItems);
    recyclerView.setAdapter(adapter);
  }

  @Override
  protected void onDestroy() {
    for (Object item : recyclerViewItems) {
      if (item instanceof NativeAdTemplate) {
        NativeAdTemplate nativeAdTemplate = (NativeAdTemplate) item;
        nativeAdTemplate.destroy();
      }
    }
    recyclerViewItems.clear();
    super.onDestroy();
  }

  private void addNativeAds() {
    // Loop through the items array and place a new native ad in every ith position in
    // the items List.
    for (int i = 0, j = 0; i <= recyclerViewItems.size(); i += ITEMS_PER_AD, j++) {
      final NativeAdTemplate adView = new NativeAdTemplate(NativeAdFeedAdvancedActivity.this,
          DataSource.NATIVE_AD_UNIT_ID);
      adView.setTag("NativeAdIndex:" + j);
      recyclerViewItems.add(i, adView);
    }
  }

  private void loadNativeAds() {
    loadNativeAds(0);
  }

  private void loadNativeAds(final int index) {
    if (index >= recyclerViewItems.size()) {
      adapter.notifyDataSetChanged();
      return;
    }

    Object item = recyclerViewItems.get(index);
    if (!(item instanceof NativeAdTemplate)) {
      throw new ClassCastException("Expected item at index " + index + " to be a native ad");
    }

    final NativeAdTemplate template = (NativeAdTemplate) item;
    template.loadNativeAd(new LoadCallback() {
      @Override
      public void onSuccess() {
        loadNativeAds(index + ITEMS_PER_AD);
      }

      @Override
      public void onFailure(LoadAdError loadAdError) {
        String error =
            String.format(
                "domain: %s, code: %d, message: %s",
                loadAdError.getDomain(), loadAdError.getCode(), loadAdError.getMessage());
        Log.e(
            "NativeAdFeedAdvanced",
            "The previous native ad failed to load with error: "
                + error
                + ". Attempting to"
                + " load the next native ad in the items list.");

        loadNativeAds(index + ITEMS_PER_AD);
      }
    });

  }

}

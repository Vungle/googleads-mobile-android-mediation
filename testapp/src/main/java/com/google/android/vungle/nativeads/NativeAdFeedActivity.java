package com.google.android.vungle.nativeads;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.vungle.R;
import com.google.android.vungle.data.DataSource;
import java.util.ArrayList;

// https://github.com/vimalcvs/RecyclerView-Native-ads-Example/tree/main/nativetemplates/src/main/java/com/google/rvadapter
public class NativeAdFeedActivity extends AppCompatActivity {

  private AdmobNativeAdAdapter admobNativeAdAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_native_feed);

    RecyclerView recyclerView = findViewById(R.id.recycler_view);
    recyclerView.setHasFixedSize(true);

    ArrayList<MenuItem> demoDatas = MenuItem.createDemoDataList(this);

    DemoDataAdapter adapter = new DemoDataAdapter(demoDatas);

    admobNativeAdAdapter = AdmobNativeAdAdapter.Builder.with(
        DataSource.NATIVE_AD_UNIT_ID,
        adapter
    ).forceReloadAdOnBind(true).build();

    recyclerView.setAdapter(admobNativeAdAdapter);

    recyclerView.setLayoutManager(new LinearLayoutManager(this));
  }

  @Override
  protected void onDestroy() {
    admobNativeAdAdapter.destroy();
    super.onDestroy();
  }
}

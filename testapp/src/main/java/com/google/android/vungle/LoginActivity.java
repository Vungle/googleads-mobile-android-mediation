package com.google.android.vungle;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.vungle.data.DataSource;
import com.google.android.vungle.ui.settings.PrivacySettingsFragment;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;

public class LoginActivity extends AppCompatActivity {

  private static final String DEFAULT_API_ENDPOINT = "https://config.ads.vungle.com/api/v5/";

  private AutoCompleteTextView main_api_host_field;
  private CheckBox checkboxNativeAdRTBHeader;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    DataSource.getInstance().init(getApplicationContext());
    setContentView(R.layout.activity_login);
    main_api_host_field = findViewById(R.id.main_api_host_field);
    TextView versionInfo = findViewById(R.id.versionInfo);
    findViewById(R.id.main_init_button).setOnClickListener(view -> launchMainPage());
    String[] apiHostValues = getResources().getStringArray(R.array.list_api_endpoints);
    ArrayAdapter<String> endpointAdapter = new ArrayAdapter<>(this,
        android.R.layout.simple_dropdown_item_1line, apiHostValues);
    main_api_host_field.setAdapter(endpointAdapter);
    String hostUrl = DataSource.getInstance().getApiHost();
    if (hostUrl == null) {
      main_api_host_field.setText(DEFAULT_API_ENDPOINT);
    } else {
      main_api_host_field.setText(hostUrl);
    }
    versionInfo.setText(Util.getVersionInfo(this));
    findViewById(R.id.privacySettings).setOnClickListener(view -> {
      new PrivacySettingsFragment().show(getSupportFragmentManager(), PrivacySettingsFragment.TAG);
    });

    checkboxNativeAdRTBHeader = findViewById(R.id.native_ads_rtb_header);
  }

  private void launchMainPage() {
    final String host = String.valueOf(main_api_host_field.getText());
    if (TextUtils.isEmpty(host) || HttpUrl.parse(host) == null) {
      Toast
          .makeText(this, "Invalid Configuration. Please enter a valid API URL", Toast.LENGTH_SHORT)
          .show();
      return;
    }
    new AlertDialog.Builder(this)
        .setMessage("Launch using the selected API Host?")
        .setPositiveButton("Start", (dialog, i) -> {

          if (checkboxNativeAdRTBHeader.isChecked()) {
            injectRtbHeadersInDebug("5fd219d7c80cb9051249a6ab"); // For Native ads
          }

          DataSource.getInstance().saveApiHost(host);
          try {
            Class<?> apiClientClz = Class.forName(
                "com.vungle.ads.internal.network.VungleApiClient");
            Field field = apiClientClz.getDeclaredField("BASE_URL");
            field.setAccessible(true);
            field.set(null, !host.endsWith("/") ? host.concat("/") : host);
          } catch (Exception e) {
            e.printStackTrace();
            dialog.dismiss();
            Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
          }
          dialog.dismiss();
          startActivity(new Intent(this, DefaultActivity.class));
          finish();
        })
        .setNegativeButton("Cancel", (dialog, i) -> {
          dialog.dismiss();
        })
        .show();
  }

  private void injectRtbHeadersInDebug(@NonNull String rtbId) {
    List<Pair<String, String>> extraHeaders = Arrays.asList(
        new Pair<>("X-Vungle-RTB-ID", rtbId),
        new Pair<>("Vungle-explain", "jaeger")
    );
    try {
      Class<?> apiClientClz = Class.forName("com.vungle.ads.internal.network.VungleApiClient");
      Field f = apiClientClz.getDeclaredField("networkInterceptors");
      f.setAccessible(true);
      Set<Interceptor> interceptors = (Set<Interceptor>) f.get(null);
      interceptors.add(chain -> {
        Request request = chain.request();
        List<String> segments = request.url().pathSegments();
        if ("ads".equals(segments.get(segments.size() - 1))) {
          Request.Builder newBuilder = request.newBuilder();
          for (Pair<String, String> header : extraHeaders) {
            newBuilder.addHeader(header.first, header.second);
          }
          return chain.proceed(newBuilder.build());
        }
        return chain.proceed(request);
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

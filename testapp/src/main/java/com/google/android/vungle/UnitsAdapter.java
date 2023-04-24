package com.google.android.vungle;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.ads.AdSize;
import com.google.android.vungle.AdUnit.AdType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;


public class UnitsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private static final int ITEM_TYPE_UNIT = 0;
  private static final int ITEM_TYPE_UNIT_BANNER = 1;
  private static final int ITEM_TYPE_BOTTOM = 2;

  public static final int STATE_IDLE = 1;
  public static final int STATE_LOADING = 2;
  public static final int STATE_LOADED = 3;
  public static final int STATE_PLAYING = 4;

  private ArrayList<AdUnit> units = new ArrayList<>();
  private StringBuilder log = new StringBuilder();
  private HashMap<AdUnit, Integer> unitStateMap = new HashMap<>();
  private UnitChangeListener unitChangeListener = null;

  UnitsAdapter(ArrayList<AdUnit> units) {
    if (units != null) {
      this.units = units;
    }
  }

  void log(String message) {
    if (message != null) {
      if (log.length() > 0) {
        log.append("\n");
      }
      log.append(message);
    }
    notifyItemChanged(units.size());
  }

  ArrayList<AdUnit> getUnits() {
    return units;
  }

  void setUnitChangeListener(UnitChangeListener listener) {
    unitChangeListener = listener;
  }

  void setUnitStatus(AdUnit unit, Integer state) {
    if (unit == null || !units.contains(unit)) {
      return;
    }
    unitStateMap.put(unit, state);
    notifyItemChanged(units.indexOf(unit));
  }

  @Override
  @NonNull
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == ITEM_TYPE_UNIT) {
      return new AdUnitHolder(
          LayoutInflater.from(parent.getContext()).inflate(R.layout.item_adunit, parent, false));
    } else if (viewType == ITEM_TYPE_UNIT_BANNER) {
      return new BannerAdHolder(LayoutInflater.from(parent.getContext())
          .inflate(R.layout.item_adunit_banner, parent, false));
    } else {
      return new BottomViewHolder(
          LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false));
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (holder instanceof AdUnitHolder) {
      AdUnit unit = units.get(position);
      Integer status = unitStateMap.get(unit);
      if (status == null) {
        status = STATE_IDLE;
      }
      ((AdUnitHolder) holder).update(unit, status);
    } else if (holder instanceof BottomViewHolder) {
      ((BottomViewHolder) holder).update();
    } else if (holder instanceof BannerAdHolder) {
      AdUnit unit = units.get(position);

      ((BannerAdHolder) holder).update(unit);
    }
  }

  @Override
  public int getItemCount() {
    return units.size() + 1;
  }

  @Override
  public int getItemViewType(int position) {
    if (position == units.size()) {
      return ITEM_TYPE_BOTTOM;
    }
    if ((units.get(position)).getType() == AdUnit.AdType.Banner ||
        (units.get(position)).getType() == AdUnit.AdType.MREC ||
        (units.get(position)).getType() == AdUnit.AdType.Native) {
      return ITEM_TYPE_UNIT_BANNER;
    }
    return ITEM_TYPE_UNIT;
  }

  private View.OnClickListener addUnitClick = v -> {
    if (unitChangeListener != null) {
      unitChangeListener.createRequest();
    }
  };

  private View.OnClickListener removeUnitClick = v -> {
    if (v.getTag() != null && v.getTag() instanceof AdUnit) {
      AdUnit unit = (AdUnit) v.getTag();
      int index = units.indexOf(unit);
      units.remove(unit);
      notifyItemRemoved(index);
      notifyItemRangeChanged(index, getItemCount());
    }
  };

  private View.OnClickListener loadUnitClick = v -> {
    if (v.getTag() != null && v.getTag() instanceof AdUnit && unitChangeListener != null) {
      unitChangeListener.loadAdRequest((AdUnit) v.getTag());
    }
  };

  private View.OnClickListener playUnitClick = v -> {
    if (v.getTag() != null && v.getTag() instanceof AdUnit && unitChangeListener != null) {
      notifyDataSetChanged();
      unitChangeListener.playAdRequest((AdUnit) v.getTag());
    }
  };

  private View.OnClickListener resetUnitsClick = v -> {
    if (unitChangeListener != null) {
      unitChangeListener.reset();
    }
  };

  String getUnitName(AdUnit unit) {
    int itemIndex = 0;
    for (AdUnit el : units) {
      if (el == unit) {
        break;
      }
      if (el.getType() == unit.getType()) {
        itemIndex++;
      }
    }
    return String.format(Locale.ENGLISH, "%s AdUnit %d %s", unit.getType().name(), itemIndex + 1,
        unit.getDescription());
  }

  private class BottomViewHolder extends RecyclerView.ViewHolder {

    private final TextView logView;
    private final ScrollView logScroll;

    @SuppressLint("ClickableViewAccessibility")
    BottomViewHolder(View itemView) {
      super(itemView);
      logView = itemView.findViewById(R.id.logContent);
      Button addUnitButton = itemView.findViewById(R.id.addUnit);
      View resetUnitsButton = itemView.findViewById(R.id.resetUnits);
      logScroll = itemView.findViewById(R.id.logScroll);
      addUnitButton.setOnClickListener(addUnitClick);
      resetUnitsButton.setOnClickListener(resetUnitsClick);

      logScroll.setOnTouchListener((v, event) -> {
        BottomViewHolder.this.itemView.getParent().requestDisallowInterceptTouchEvent(true);
        return false;
      });
    }

    void update() {
      logView.setText(log.toString());
      logScroll.postDelayed(() -> logScroll.fullScroll(View.FOCUS_DOWN), 150);
    }
  }

  private class AdUnitHolder extends RecyclerView.ViewHolder {

    private TextView itemNameLabel;
    private EditText itemIdEdit;
    private Button loadAdButton;
    private Button playAdButton;
    private Button removeButton;
    private AdUnit unit;
    private View root;

    AdUnitHolder(View itemView) {
      super(itemView);
      itemNameLabel = itemView.findViewById(R.id.itemName);
      itemIdEdit = itemView.findViewById(R.id.itemId);
      loadAdButton = itemView.findViewById(R.id.loadAd);
      playAdButton = itemView.findViewById(R.id.playAd);
      removeButton = itemView.findViewById(R.id.removeUnit);
      root = itemView.findViewById(R.id.itemRoot);
      loadAdButton.setOnClickListener(loadUnitClick);
      playAdButton.setOnClickListener(playUnitClick);
      removeButton.setOnClickListener(removeUnitClick);
    }

    void update(AdUnit unit, int state) {
      this.unit = unit;
      loadAdButton.setTag(unit);
      playAdButton.setTag(unit);
      removeButton.setTag(unit);
      itemIdEdit.setTag(unit);
      itemIdEdit.setText(unit.getId());
      itemNameLabel.setText(getUnitName(unit));
      switch (state) {
        case STATE_IDLE:
          loadAdButton.setEnabled(true);
          playAdButton.setEnabled(false);
          removeButton.setEnabled(true);
          break;
        case STATE_LOADING:
        case STATE_PLAYING:
          loadAdButton.setEnabled(false);
          playAdButton.setEnabled(false);
          removeButton.setEnabled(false);
          break;
        case STATE_LOADED:
          loadAdButton.setEnabled(false);
          playAdButton.setEnabled(true);
          removeButton.setEnabled(false);
          break;
      }
      int bgColor;
      switch (unit.getType()) {
        case RewardedAd:
          bgColor = R.color.rewarded_background;
          break;
        case RewardedInterstitial:
          bgColor = R.color.rewarded_interstitial_background;
          break;
        case Interstitial:
        default:
          bgColor = R.color.interstitial_background;
          break;
      }
      root.setBackgroundResource(bgColor);
    }
  }

  private class BannerAdHolder extends RecyclerView.ViewHolder {

    private TextView itemNameLabel;
    private EditText itemIdEdit;
    private Button removeButton;
    private Spinner adSizeSpinner;
    private AdUnit unit;
    private View root;

    BannerAdHolder(@NonNull View itemView) {
      super(itemView);
      root = itemView.findViewById(R.id.itemRoot);
      itemNameLabel = itemView.findViewById(R.id.itemName);
      itemIdEdit = itemView.findViewById(R.id.itemId);
      Button launchButton = itemView.findViewById(R.id.launchBannerScreen);
      removeButton = itemView.findViewById(R.id.removeUnit);
      adSizeSpinner = itemView.findViewById(R.id.adSizeSpinner);
      launchButton.setOnClickListener(v -> {
        if (AdUnit.AdType.Native == unit.getType()) {
          unitChangeListener.launchNative(unit);
        } else {
          unitChangeListener.launchBanner(unit);
        }
      });
      adSizeSpinner.setAdapter(new ArrayAdapter<>(itemView.getContext(),
          android.R.layout.simple_spinner_item, defaultSizes));
      adSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
          updateBannerAdSize(unit);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
      });
      removeButton.setOnClickListener(removeUnitClick);
    }

    private void updateBannerAdSize(AdUnit unit) {
      if (adSizeSpinner.getVisibility() == View.GONE) {
        unit.setAdSizeWrapper(defaultSizes.get(2));
      } else {
        int position = adSizeSpinner.getSelectedItemPosition();
        if (position >= 0 && position < defaultSizes.size()) {
          unit.setAdSizeWrapper(defaultSizes.get(position));
        }
      }
    }

    void update(AdUnit unit) {
      this.unit = unit;
      if (unit.getType() == AdUnit.AdType.MREC) {
        adSizeSpinner.setVisibility(View.GONE);
        root.setBackgroundResource(R.color.mrec_background);
      } else if (unit.getType() == AdType.Banner) {
        adSizeSpinner.setVisibility(View.VISIBLE);
        adSizeSpinner.setSelection(getSelectionPos(unit));
        root.setBackgroundResource(R.color.banner_background);
      } else if (unit.getType() == AdUnit.AdType.Native) {
        adSizeSpinner.setVisibility(View.GONE);
        root.setBackgroundResource(R.color.native_background);
      }

      updateBannerAdSize(unit);
      itemIdEdit.setText(unit.getId());
      itemNameLabel.setText(getUnitName(unit));
      removeButton.setTag(unit);
      removeButton.setEnabled(true);
    }

    private int getSelectionPos(AdUnit unit) {
      if (unit == null || unit.getAdSizeWrapper() == null) {
        return 0;
      }

      return defaultSizes.indexOf(unit.getAdSizeWrapper());
    }

  }

  interface UnitChangeListener {

    void createRequest();

    void loadAdRequest(AdUnit unit);

    void playAdRequest(AdUnit unit);

    void launchBanner(AdUnit unit);

    void launchNative(AdUnit unit);

    void reset();
  }

  public static class AdSizeSpinnerWrapper implements Parcelable {

    private final AdSize size;
    private final String otherSize;
    private final String name;

    private AdSizeSpinnerWrapper(@NonNull AdSize size, @NonNull String name) {
      this.size = size;
      this.otherSize = null;
      this.name = name;
    }

    private AdSizeSpinnerWrapper(@NonNull String size, @NonNull String name) {
      this.size = null;
      this.otherSize = size;
      this.name = name;
    }

    AdSizeSpinnerWrapper(Parcel in) {
      byte hasAdSize = in.readByte();
      if (hasAdSize == 1) {
        size = new AdSize(in.readInt(), in.readInt());
      } else {
        size = null;
      }
      byte hasOtherSize = in.readByte();
      if (hasOtherSize == 1) {
        otherSize = in.readString();
      } else {
        otherSize = null;
      }
      name = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeByte((byte) (size == null ? 0 : 1));
      if (size != null) {
        dest.writeInt(size.getWidth());
        dest.writeInt(size.getHeight());
      }
      dest.writeByte((byte) (otherSize == null ? 0 : 1));
      if (otherSize != null) {
        dest.writeString(otherSize);
      }
      dest.writeString(name);
    }

    public static final Creator<AdSizeSpinnerWrapper> CREATOR = new Creator<AdSizeSpinnerWrapper>() {
      @Override
      public AdSizeSpinnerWrapper createFromParcel(Parcel in) {
        return new AdSizeSpinnerWrapper(in);
      }

      @Override
      public AdSizeSpinnerWrapper[] newArray(int size) {
        return new AdSizeSpinnerWrapper[size];
      }
    };

    public AdSize getSize() {
      return size;
    }

    String getOtherSize() {
      return otherSize;
    }

    @NonNull
    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      AdSizeSpinnerWrapper that = (AdSizeSpinnerWrapper) o;
      if (size != null && that.size == null || size == null && that.size != null) {
        return false;
      }
      if (otherSize != null && that.otherSize == null
          || otherSize == null && that.otherSize != null) {
        return false;
      }
      if (size != null) {
        return size.getWidth() == that.size.getWidth() && size.getHeight() == that.size.getHeight();
      } else if (otherSize != null) {
        return otherSize.equals(that.otherSize);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (size != null) {
        return size.hashCode();
      } else if (otherSize != null) {
        return otherSize.hashCode();
      }
      return -1;
    }

    @Override
    public int describeContents() {
      return 0;
    }

    boolean isAdaptiveBanner() {
      return otherSize != null && otherSize.startsWith("ADAPTIVE_BANNER");
    }

    public boolean isAdaptiveBannerLandscape() {
      return otherSize != null && otherSize.equals("ADAPTIVE_BANNER_LANDSCAPE");
    }

    boolean isArbitraryBanner() {
      return otherSize != null && otherSize.equals("Arbitrary");
    }
  }

  public static ArrayList<AdSizeSpinnerWrapper> defaultSizes = new ArrayList<AdSizeSpinnerWrapper>() {{
    add(new AdSizeSpinnerWrapper(AdSize.BANNER, "BANNER(320x50)"));
    add(new AdSizeSpinnerWrapper(AdSize.LARGE_BANNER, "LARGE_BANNER(320x100)"));
    add(new AdSizeSpinnerWrapper(AdSize.MEDIUM_RECTANGLE, "MEDIUM_RECTANGLE(300x250)"));
    add(new AdSizeSpinnerWrapper(AdSize.LEADERBOARD, "LEADERBOARD(468x60)"));
    add(new AdSizeSpinnerWrapper("ADAPTIVE_BANNER_AUTO", "ADAPTIVE_BANNER(AUTO)"));
    add(new AdSizeSpinnerWrapper("ADAPTIVE_BANNER_PORTRAIT", "ADAPTIVE_BANNER(PORTRAIT)"));
    add(new AdSizeSpinnerWrapper("ADAPTIVE_BANNER_LANDSCAPE", "ADAPTIVE_BANNER(LANDSCAPE)"));
    add(new AdSizeSpinnerWrapper("Arbitrary", "Arbitrary(310X60)"));
  }};

}

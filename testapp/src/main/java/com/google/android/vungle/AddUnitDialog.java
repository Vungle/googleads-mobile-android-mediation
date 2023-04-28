package com.google.android.vungle;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;


public class AddUnitDialog extends DialogFragment {

  public static AddUnitDialog newInstance() {
    AddUnitDialog f = new AddUnitDialog();
    f.setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogTheme);
    return f;
  }

  private AdUnit.AdType adType = AdUnit.AdType.Interstitial;
  private OnAddClickListener listener;

  public void setListener(OnAddClickListener listener) {
    this.listener = listener;
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.dialog_add_adunit, container);
    final EditText unitId = v.findViewById(R.id.itemId);
    final CheckBox isOpenBiddingUnitView = v.findViewById(R.id.is_openbidding_check);
    v.findViewById(R.id.cancelButton).setOnClickListener(v1 -> dismiss());
    unitId.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {

      }

      @Override
      public void afterTextChanged(Editable s) {
        unitId.setError(null);
      }
    });
    RadioGroup adTypeGroup = v.findViewById(R.id.adTypeRadioGroup);
    adTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
      if (checkedId == R.id.interstitialButton) {
        adType = AdUnit.AdType.Interstitial;
      } else if (checkedId == R.id.rewardedButton) {
        adType = AdUnit.AdType.RewardedAd;
      } else if (checkedId == R.id.rewardedInterstitialButton) {
        adType = AdUnit.AdType.RewardedInterstitial;
      } else if (checkedId == R.id.bannerButton) {
        adType = AdUnit.AdType.Banner;
      } else if (checkedId == R.id.mrecButton) {
        adType = AdUnit.AdType.MREC;
      } else if (checkedId == R.id.nativeButton) {
        adType = AdUnit.AdType.Native;
      }
    });
    v.findViewById(R.id.addButton).setOnClickListener(v12 -> {
      if (unitId.getText() == null || unitId.getText().length() == 0) {
        unitId.setError("Unit id can not be empty");
        return;
      }
      if (listener != null) {
        boolean isOpenBidingUnit = isOpenBiddingUnitView.isChecked();
        String result = listener.addUnit(
            new AdUnit(unitId.getText().toString(), adType, isOpenBidingUnit,
                isOpenBidingUnit ? "HB" : "NON-HB"));
        unitId.setError(result);
      }
    });

    return v;
  }

  @Override
  public void onPause() {
    super.onPause();
    this.dismissAllowingStateLoss();
  }

  public interface OnAddClickListener {

    String addUnit(AdUnit unit);
  }
}

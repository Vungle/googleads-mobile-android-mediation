package com.google.android.vungle.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.vungle.R;
import com.google.android.vungle.data.DataSource;
import com.vungle.mediation.VungleConsent;

public class PrivacySettingsFragment extends DialogFragment {
    public static final String TAG = PrivacySettingsFragment.class.getSimpleName();

    public enum COPPA_STATUS {
        NOT_SET,
        USER_COPPA,
        USER_NOT_COPPA
    }

    @BindView(R.id.ccpa_spinner)
    Spinner ccpaConsentOptions;

    @BindView(R.id.coppa_spinner_admob)
    Spinner coppaConsentOptionsAdmob;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.privacy_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
        ccpaConsentOptions.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Not-set", "opted_in", "opted_out"}));

        coppaConsentOptionsAdmob.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item,
                new String[]{COPPA_STATUS.NOT_SET.name(), COPPA_STATUS.USER_COPPA.name(), COPPA_STATUS.USER_NOT_COPPA.name()}));

        String ccpaStatus = VungleConsent.getCcpaStatus();
        int ccpaSelection = 0;
        if (ccpaStatus.equals("opted_in")) {
            ccpaSelection = 1;
        } else if (ccpaStatus.equals("opted_out")) {
            ccpaSelection = 2;
        }
        ccpaConsentOptions.setSelection(ccpaSelection);
    }


    @OnClick(R.id.cancel)
    public void onCancelClicked() {
        dismiss();
    }

    @OnClick(R.id.save)
    public void onSaveClicked() {
        String ccpaConsent = (String) ccpaConsentOptions.getSelectedItem();
        if ("opted_in".equals(ccpaConsent)) {
            VungleConsent.setCCPAStatus(true);
        } else if ("opted_out".equals(ccpaConsent)) {
            VungleConsent.setCCPAStatus(false);
        }

        String coppaConsentAdmob = (String) coppaConsentOptionsAdmob.getSelectedItem();
        if (COPPA_STATUS.USER_COPPA.name().equals(coppaConsentAdmob)) {
            DataSource.getInstance().setCoppaStatus(true);
        } else if (COPPA_STATUS.USER_NOT_COPPA.name().equals(coppaConsentAdmob)) {
            DataSource.getInstance().setCoppaStatus(false);
        }

        dismiss();
    }
}

package com.abhishek.batsignal.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;

import com.abhishek.batsignal.R;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.abhishek.batsignal.BatSignalApplication;
import com.abhishek.batsignal.R;
import com.abhishek.batsignal.data.SettingsRepository;

/**
 * Fragment for configuring alert style and behavior.
 */
public class SettingsFragment extends Fragment {

    private SettingsRepository settings;

    private Switch masterSwitch;
    private Switch soundSwitch;
    private Switch volumeSwitch;
    private Switch fullScreenSwitch;
    private EditText delayInput;
    private Spinner vibeSpinner;
    private EditText customVibeInput;
    private LinearLayout customVibeContainer;
    private Spinner flashSpinner;
    private EditText customFlashOnInput;
    private EditText customFlashOffInput;
    private LinearLayout customFlashContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settings = ((BatSignalApplication) requireActivity().getApplication())
                .getSettingsRepository();

        bindViews(view);
        setupSpinners();
        loadSettings();
        setupListeners();
    }

    private void bindViews(@NonNull View view) {
        masterSwitch = view.findViewById(R.id.switch_master);
        soundSwitch = view.findViewById(R.id.switch_sound);
        volumeSwitch = view.findViewById(R.id.switch_volume_override);
        fullScreenSwitch = view.findViewById(R.id.switch_fullscreen);
        delayInput = view.findViewById(R.id.input_delay);
        vibeSpinner = view.findViewById(R.id.spinner_vibration);
        customVibeInput = view.findViewById(R.id.input_custom_vibration);
        customVibeContainer = view.findViewById(R.id.container_custom_vibration);
        flashSpinner = view.findViewById(R.id.spinner_flash);
        customFlashOnInput = view.findViewById(R.id.input_custom_flash_on);
        customFlashOffInput = view.findViewById(R.id.input_custom_flash_off);
        customFlashContainer = view.findViewById(R.id.container_custom_flash);
    }

    private void setupSpinners() {
        String[] vibePresets = getResources().getStringArray(R.array.vibration_presets);
        vibeSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, vibePresets));

        String[] flashPresets = getResources().getStringArray(R.array.flash_presets);
        flashSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, flashPresets));
    }

    private void loadSettings() {
        masterSwitch.setChecked(settings.isEnabled());
        soundSwitch.setChecked(settings.isSoundEnabled());
        volumeSwitch.setChecked(settings.isVolumeOverrideEnabled());
        fullScreenSwitch.setChecked(settings.isFullScreenAlertEnabled());
        delayInput.setText(String.valueOf(settings.getDelaySeconds()));

        setSpinnerSelection(vibeSpinner, settings.getVibrationPreset());
        setSpinnerSelection(flashSpinner, settings.getFlashPreset());

        customVibeInput.setText(joinLongArray(settings.getCustomVibrationPattern()));
        customFlashOnInput.setText(joinIntArray(settings.getCustomFlashOnPattern()));
        customFlashOffInput.setText(joinIntArray(settings.getCustomFlashOffPattern()));

        updateCustomVisibility();
    }

    private void setupListeners() {
        masterSwitch.setOnCheckedChangeListener((btn, checked) -> settings.setEnabled(checked));
        soundSwitch.setOnCheckedChangeListener((btn, checked) -> settings.setSoundEnabled(checked));
        volumeSwitch.setOnCheckedChangeListener((btn, checked) -> settings.setVolumeOverrideEnabled(checked));
        fullScreenSwitch.setOnCheckedChangeListener((btn, checked) -> settings.setFullScreenAlertEnabled(checked));

        vibeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String preset = parent.getItemAtPosition(position).toString();
                settings.setVibrationPreset(preset);
                updateCustomVisibility();
                if ("Custom".equals(preset)) {
                    saveCustomVibration();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        flashSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String preset = parent.getItemAtPosition(position).toString();
                settings.setFlashPreset(preset);
                updateCustomVisibility();
                if ("Custom".equals(preset)) {
                    saveCustomFlash();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        saveNumericSettings();
    }

    private void saveNumericSettings() {
        try {
            int delay = Integer.parseInt(delayInput.getText().toString().trim());
            settings.setDelaySeconds(delay);
        } catch (NumberFormatException ignored) {
        }

        saveCustomVibration();
        saveCustomFlash();
    }

    private void saveCustomVibration() {
        long[] pattern = parseLongArray(customVibeInput.getText().toString());
        if (pattern.length > 0) {
            settings.setCustomVibrationPattern(pattern);
        }
    }

    private void saveCustomFlash() {
        int[] onPattern = parseIntArray(customFlashOnInput.getText().toString());
        int[] offPattern = parseIntArray(customFlashOffInput.getText().toString());
        if (onPattern.length > 0) {
            settings.setCustomFlashPattern(onPattern, offPattern);
        }
    }

    private void updateCustomVisibility() {
        boolean vibeCustom = "Custom".equals(vibeSpinner.getSelectedItem());
        customVibeContainer.setVisibility(vibeCustom ? View.VISIBLE : View.GONE);

        boolean flashCustom = "Custom".equals(flashSpinner.getSelectedItem());
        customFlashContainer.setVisibility(flashCustom ? View.VISIBLE : View.GONE);
    }

    private void setSpinnerSelection(@NonNull Spinner spinner, @NonNull String value) {
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (value.equals(adapter.getItem(i).toString())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    @NonNull
    private static String joinLongArray(long[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        return sb.toString();
    }

    @NonNull
    private static String joinIntArray(int[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        return sb.toString();
    }

    @NonNull
    private static long[] parseLongArray(@NonNull String text) {
        if (TextUtils.isEmpty(text)) return new long[0];
        try {
            String[] parts = text.split(",");
            long[] result = new long[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Long.parseLong(parts[i].trim());
            }
            return result;
        } catch (NumberFormatException e) {
            return new long[0];
        }
    }

    @NonNull
    private static int[] parseIntArray(@NonNull String text) {
        if (TextUtils.isEmpty(text)) return new int[0];
        try {
            String[] parts = text.split(",");
            int[] result = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.parseInt(parts[i].trim());
            }
            return result;
        } catch (NumberFormatException e) {
            return new int[0];
        }
    }
}

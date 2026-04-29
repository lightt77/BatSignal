package com.abhishek.batsignal.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Arrays;

/**
 * Repository for user-configurable alert settings.
 */
public class SettingsRepository {

    private static final String PREFS_NAME = "batsignal_settings";

    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_DELAY_SECONDS = "delay_seconds";
    private static final String KEY_VIBRATION_PRESET = "vibration_preset";
    private static final String KEY_CUSTOM_VIBRATION = "custom_vibration";
    private static final String KEY_FLASH_PRESET = "flash_preset";
    private static final String KEY_CUSTOM_FLASH_ON = "custom_flash_on";
    private static final String KEY_CUSTOM_FLASH_OFF = "custom_flash_off";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_VOLUME_OVERRIDE = "volume_override";
    private static final String KEY_FULL_SCREEN_ALERT = "full_screen_alert";

    private static final String DEFAULT_VIBRATION_PRESET = "SOS";
    private static final String DEFAULT_FLASH_PRESET = "Strobe";

    private final SharedPreferences prefs;

    public SettingsRepository(@NonNull Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, true);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public int getDelaySeconds() {
        return prefs.getInt(KEY_DELAY_SECONDS, 5);
    }

    public void setDelaySeconds(int seconds) {
        prefs.edit().putInt(KEY_DELAY_SECONDS, Math.max(1, Math.min(60, seconds))).apply();
    }

    @NonNull
    public String getVibrationPreset() {
        return prefs.getString(KEY_VIBRATION_PRESET, DEFAULT_VIBRATION_PRESET);
    }

    public void setVibrationPreset(@NonNull String preset) {
        prefs.edit().putString(KEY_VIBRATION_PRESET, preset).apply();
    }

    @NonNull
    public long[] getCustomVibrationPattern() {
        String csv = prefs.getString(KEY_CUSTOM_VIBRATION, "0,200,100,200");
        return parseLongArray(csv);
    }

    public void setCustomVibrationPattern(@NonNull long[] pattern) {
        prefs.edit().putString(KEY_CUSTOM_VIBRATION, joinArray(pattern)).apply();
    }

    @NonNull
    public String getFlashPreset() {
        return prefs.getString(KEY_FLASH_PRESET, DEFAULT_FLASH_PRESET);
    }

    public void setFlashPreset(@NonNull String preset) {
        prefs.edit().putString(KEY_FLASH_PRESET, preset).apply();
    }

    @NonNull
    public int[] getCustomFlashOnPattern() {
        String csv = prefs.getString(KEY_CUSTOM_FLASH_ON, "50,50,50,50,50");
        return parseIntArray(csv);
    }

    @NonNull
    public int[] getCustomFlashOffPattern() {
        String csv = prefs.getString(KEY_CUSTOM_FLASH_OFF, "50,50,50,50,0");
        return parseIntArray(csv);
    }

    public void setCustomFlashPattern(@NonNull int[] onPattern, @NonNull int[] offPattern) {
        prefs.edit()
                .putString(KEY_CUSTOM_FLASH_ON, joinArray(onPattern))
                .putString(KEY_CUSTOM_FLASH_OFF, joinArray(offPattern))
                .apply();
    }

    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true);
    }

    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    public boolean isVolumeOverrideEnabled() {
        return prefs.getBoolean(KEY_VOLUME_OVERRIDE, true);
    }

    public void setVolumeOverrideEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOLUME_OVERRIDE, enabled).apply();
    }

    public boolean isFullScreenAlertEnabled() {
        return prefs.getBoolean(KEY_FULL_SCREEN_ALERT, true);
    }

    public void setFullScreenAlertEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_FULL_SCREEN_ALERT, enabled).apply();
    }

    @NonNull
    private static long[] parseLongArray(@NonNull String csv) {
        try {
            return Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .mapToLong(Long::parseLong)
                    .toArray();
        } catch (NumberFormatException e) {
            return new long[]{0, 250};
        }
    }

    @NonNull
    private static int[] parseIntArray(@NonNull String csv) {
        try {
            return Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .mapToInt(Integer::parseInt)
                    .toArray();
        } catch (NumberFormatException e) {
            return new int[]{500};
        }
    }

    @NonNull
    private static String joinArray(@NonNull long[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        return sb.toString();
    }

    @NonNull
    private static String joinArray(@NonNull int[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        return sb.toString();
    }
}

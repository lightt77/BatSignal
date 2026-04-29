package com.abhishek.batsignal.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Utility class for Samsung-specific battery and background optimization guidance.
 */
public final class SamsungUtils {

    private static final String TAG = "SamsungUtils";
    private static final String PREFS_NAME = "batsignal_samsung_prefs";
    private static final String KEY_NEVER_SLEEP_SHOWN = "never_sleep_shown";

    private SamsungUtils() {
        // utility class
    }

    /**
     * Returns true if the device is likely a Samsung device.
     */
    public static boolean isSamsungDevice() {
        String manufacturer = Build.MANUFACTURER;
        return manufacturer != null && manufacturer.toLowerCase().contains("samsung");
    }

    /**
     * Opens the system battery optimization settings for this app.
     */
    public static void openBatteryOptimizationSettings(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Battery optimization settings not available");
            fallbackToAppSettings(context);
        }
    }

    /**
     * Opens the app's system settings page.
     */
    public static void fallbackToAppSettings(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Cannot open app settings");
        }
    }

    /**
     * Checks whether battery optimization is already disabled for this app.
     */
    public static boolean isBatteryOptimizationDisabled(@NonNull Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            return false;
        }
        return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    /**
     * Opens Do Not Disturb access settings so the user can whitelist this app.
     */
    public static void openDndSettings(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "DND settings not available");
        }
    }

    /**
     * Opens Samsung's "Never sleeping apps" settings if possible.
     * Falls back to general device care settings.
     */
    public static void openSamsungNeverSleepSettings(@NonNull Context context) {
        // Try deep-linking into Samsung's device care battery settings
        Intent intent = new Intent();
        intent.setAction("android.settings.APP_OPEN_BY_DEFAULT_SETTINGS");
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        try {
            context.startActivity(intent);
            return;
        } catch (Exception e) {
            Log.w(TAG, "App open by default settings not available, trying device care");
        }

        // Fallback to device care
        intent = new Intent("android.settings.MANAGE_APP_PERMISSIONS");
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        try {
            context.startActivity(intent);
        } catch (Exception e2) {
            Log.w(TAG, "Samsung specific settings not available, falling back to app settings");
            fallbackToAppSettings(context);
        }
    }

    public static void markNeverSleepShown(@NonNull Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NEVER_SLEEP_SHOWN, true)
                .apply();
    }

    public static boolean hasNeverSleepBeenShown(@NonNull Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NEVER_SLEEP_SHOWN, false);
    }
}

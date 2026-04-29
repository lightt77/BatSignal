package com.abhishek.batsignal.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
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
}

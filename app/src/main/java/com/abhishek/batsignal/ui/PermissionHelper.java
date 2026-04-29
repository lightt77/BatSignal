package com.abhishek.batsignal.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.abhishek.batsignal.R;
import com.abhishek.batsignal.util.SamsungUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the permission and system-settings onboarding chain.
 * Guides the user through each required grant step-by-step.
 */
public class PermissionHelper {

    public interface Callback {
        void onComplete();
    }

    private static final int REQ_RUNTIME_PERMISSIONS = 100;

    private final Activity activity;
    private final Callback callback;

    public PermissionHelper(@NonNull Activity activity, @Nullable Callback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    /**
     * Starts the permission wizard if any grants are missing.
     * Returns true if the wizard was started, false if everything is already granted.
     */
    public boolean startIfNeeded() {
        if (hasAllRuntimePermissions() && hasBatteryOptimizationDisabled() && hasDndAccess()) {
            if (callback != null) callback.onComplete();
            return false;
        }
        showNextDialog();
        return true;
    }

    public void onRequestPermissionsResult(int requestCode) {
        if (requestCode == REQ_RUNTIME_PERMISSIONS) {
            showNextDialog();
        }
    }

    public void onActivityResult() {
        showNextDialog();
    }

    private void showNextDialog() {
        if (!hasAllRuntimePermissions()) {
            showRuntimePermissionDialog();
        } else if (!hasBatteryOptimizationDisabled()) {
            showBatteryOptimizationDialog();
        } else if (!hasDndAccess()) {
            showDndDialog();
        } else {
            if (callback != null) callback.onComplete();
        }
    }

    private void showRuntimePermissionDialog() {
        List<String> missing = getMissingRuntimePermissions();
        new AlertDialog.Builder(activity)
                .setTitle(R.string.perm_dialog_title)
                .setMessage(buildPermissionRationale(missing))
                .setCancelable(false)
                .setPositiveButton(R.string.perm_grant, (d, w) -> {
                    ActivityCompat.requestPermissions(
                            activity,
                            missing.toArray(new String[0]),
                            REQ_RUNTIME_PERMISSIONS
                    );
                })
                .setNegativeButton(R.string.perm_exit, (d, w) -> activity.finish())
                .show();
    }

    private void showBatteryOptimizationDialog() {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.battery_dialog_title)
                .setMessage(SamsungUtils.isSamsungDevice()
                        ? R.string.battery_dialog_msg_samsung
                        : R.string.battery_dialog_msg)
                .setCancelable(false)
                .setPositiveButton(R.string.perm_open_settings, (d, w) ->
                        SamsungUtils.openBatteryOptimizationSettings(activity))
                .setNegativeButton(R.string.perm_skip, (d, w) -> showNextDialog())
                .show();
    }

    private void showDndDialog() {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.dnd_dialog_title)
                .setMessage(R.string.dnd_dialog_msg)
                .setCancelable(false)
                .setPositiveButton(R.string.perm_open_settings, (d, w) ->
                        SamsungUtils.openDndSettings(activity))
                .setNegativeButton(R.string.perm_skip, (d, w) -> showNextDialog())
                .show();
    }

    @NonNull
    private List<String> getMissingRuntimePermissions() {
        List<String> perms = new ArrayList<>();
        addIfMissing(perms, Manifest.permission.READ_PHONE_STATE);
        addIfMissing(perms, Manifest.permission.READ_CALL_LOG);
        addIfMissing(perms, Manifest.permission.READ_CONTACTS);
        addIfMissing(perms, Manifest.permission.CAMERA);
        addIfMissing(perms, Manifest.permission.VIBRATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addIfMissing(perms, Manifest.permission.POST_NOTIFICATIONS);
        }
        return perms;
    }

    private void addIfMissing(@NonNull List<String> list, @NonNull String permission) {
        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            list.add(permission);
        }
    }

    private boolean hasAllRuntimePermissions() {
        return getMissingRuntimePermissions().isEmpty();
    }

    private boolean hasBatteryOptimizationDisabled() {
        return SamsungUtils.isBatteryOptimizationDisabled(activity);
    }

    private boolean hasDndAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.isNotificationPolicyAccessGranted();
    }

    @NonNull
    private String buildPermissionRationale(@NonNull List<String> missing) {
        StringBuilder sb = new StringBuilder();
        sb.append(activity.getString(R.string.perm_dialog_msg));
        sb.append("\n\n");
        for (String perm : missing) {
            sb.append("• ").append(formatPermissionName(perm)).append("\n");
        }
        return sb.toString();
    }

    @NonNull
    private String formatPermissionName(@NonNull String permission) {
        switch (permission) {
            case Manifest.permission.READ_PHONE_STATE:
                return activity.getString(R.string.perm_phone_state);
            case Manifest.permission.READ_CALL_LOG:
                return activity.getString(R.string.perm_call_log);
            case Manifest.permission.READ_CONTACTS:
                return activity.getString(R.string.perm_contacts);
            case Manifest.permission.CAMERA:
                return activity.getString(R.string.perm_camera);
            case Manifest.permission.VIBRATE:
                return activity.getString(R.string.perm_vibrate);
            case Manifest.permission.POST_NOTIFICATIONS:
                return activity.getString(R.string.perm_notifications);
            default:
                return permission;
        }
    }
}

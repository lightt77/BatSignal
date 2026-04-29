package com.abhishek.batsignal.hardware;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Concrete implementation of {@link VibrationController} using system Vibrator service.
 */
public class VibrationControllerImpl implements VibrationController {

    private final Vibrator vibrator;

    public VibrationControllerImpl(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            this.vibrator = manager != null ? manager.getDefaultVibrator() : null;
        } else {
            this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    @Override
    public void startPattern(@Nullable long[] pattern) {
        if (vibrator == null || pattern == null || pattern.length == 0) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    @Override
    public void stop() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
}

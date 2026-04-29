package com.abhishek.batsignal.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.abhishek.batsignal.BatSignalApplication;
import com.abhishek.batsignal.data.AlertPatternProvider;
import com.abhishek.batsignal.data.SettingsRepository;
import com.abhishek.batsignal.hardware.FlashController;
import com.abhishek.batsignal.hardware.FlashControllerImpl;
import com.abhishek.batsignal.hardware.SoundController;
import com.abhishek.batsignal.hardware.SoundControllerImpl;
import com.abhishek.batsignal.hardware.VibrationController;
import com.abhishek.batsignal.hardware.VibrationControllerImpl;
import com.abhishek.batsignal.notification.NotificationHelper;

/**
 * Foreground service that executes the full BatSignal alert pattern.
 * Handles vibration, flash, sound, and full-screen notification.
 */
public class BatSignalService extends Service {

    private static final String TAG = "BatSignalService";
    public static final String ACTION_START = "com.abhishek.batsignal.ACTION_START";
    public static final String ACTION_STOP = "com.abhishek.batsignal.ACTION_STOP";
    public static final String EXTRA_CALLER_NAME = "caller_name";

    private static final long AUTO_STOP_MILLIS = 60_000;

    private VibrationController vibrationController;
    private FlashController flashController;
    private SoundController soundController;
    private NotificationHelper notificationHelper;
    private Handler autoStopHandler;
    private PowerManager.WakeLock wakeLock;
    private String callerName = "Unknown";

    @Override
    public void onCreate() {
        super.onCreate();
        Context appContext = getApplicationContext();
        this.vibrationController = new VibrationControllerImpl(appContext);
        this.flashController = new FlashControllerImpl(appContext);
        this.soundController = new SoundControllerImpl(appContext);
        this.notificationHelper = new NotificationHelper(appContext);
        this.autoStopHandler = new Handler(getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopAlert();
            return START_NOT_STICKY;
        }

        if (!ACTION_START.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        this.callerName = intent.getStringExtra(EXTRA_CALLER_NAME);
        if (callerName == null || callerName.isEmpty()) {
            callerName = "Unknown";
        }

        startForeground(1, notificationHelper.buildForegroundNotification(callerName));
        startAlert();
        return START_NOT_STICKY;
    }

    private void startAlert() {
        Log.d(TAG, "Starting alert for " + callerName);
        acquireWakeLock();

        SettingsRepository settings = ((BatSignalApplication) getApplication()).getSettingsRepository();

        // Vibration
        long[] vibePattern = AlertPatternProvider.getVibrationPattern(
                settings.getVibrationPreset(),
                settings.getCustomVibrationPattern()
        );
        vibrationController.startPattern(vibePattern);

        // Flash
        AlertPatternProvider.FlashPattern flashPattern = AlertPatternProvider.getFlashPattern(
                settings.getFlashPreset(),
                settings.getCustomFlashOnPattern(),
                settings.getCustomFlashOffPattern()
        );
        if (flashPattern != null) {
            flashController.startPattern(flashPattern.getOnDurations(), flashPattern.getOffDurations());
        }

        // Sound
        if (settings.isSoundEnabled()) {
            soundController.start(settings.isVolumeOverrideEnabled());
        }

        // Full-screen alert notification
        notificationHelper.showAlertNotification(callerName);

        // Auto-stop safety net
        autoStopHandler.postDelayed(this::stopAlert, AUTO_STOP_MILLIS);
    }

    private void stopAlert() {
        Log.d(TAG, "Stopping alert");
        autoStopHandler.removeCallbacksAndMessages(null);

        vibrationController.stop();
        flashController.stop();
        soundController.stop();
        notificationHelper.cancelAlertNotification();

        releaseWakeLock();
        stopForeground(true);
        stopSelf();
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "BatSignal::AlertWakeLock"
            );
            wakeLock.acquire(2 * 60 * 1000L); // 2 minutes max
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        autoStopHandler.removeCallbacksAndMessages(null);
        vibrationController.stop();
        flashController.stop();
        soundController.stop();
        releaseWakeLock();
    }
}

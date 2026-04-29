package com.abhishek.batsignal.hardware;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.abhishek.batsignal.R;

import java.io.IOException;

/**
 * Concrete implementation of {@link SoundController} using MediaPlayer on the alarm stream.
 */
public class SoundControllerImpl implements SoundController {

    private static final String TAG = "SoundController";

    private final Context context;
    private final AudioManager audioManager;
    private MediaPlayer mediaPlayer;
    private int previousAlarmVolume = -1;

    public SoundControllerImpl(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void start(boolean overrideVolume) {
        stop(); // ensure clean state

        if (overrideVolume && audioManager != null) {
            previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
        }

        Uri soundUri = getAlertSoundUri();
        if (soundUri == null) {
            Log.w(TAG, "No alert sound URI available");
            return;
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(context, soundUri);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "Failed to prepare MediaPlayer", e);
            stop();
        }
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (IllegalStateException e) {
                Log.w(TAG, "MediaPlayer state error on stop", e);
            }
            mediaPlayer = null;
        }

        if (previousAlarmVolume >= 0 && audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, 0);
            previousAlarmVolume = -1;
        }
    }

    @NonNull
    private Uri getAlertSoundUri() {
        // Prefer the default alarm ringtone; fallback to notification, then system default
        Uri alarm = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
        if (alarm != null) {
            return alarm;
        }
        Uri notification = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
        if (notification != null) {
            return notification;
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    }
}

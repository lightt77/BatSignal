package com.abhishek.batsignal;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.abhishek.batsignal.service.BatSignalService;

/**
 * Full-screen alert activity displayed over the lock screen when a BatSignal fires.
 * Minimal UI: caller name and a dismiss button.
 */
public class AlertActivity extends AppCompatActivity {

    public static final String EXTRA_CALLER_NAME = "caller_name";
    public static final String EXTRA_ALERT_TYPE = "alert_type";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        );

        setContentView(R.layout.activity_alert);

        String callerName = getIntent().getStringExtra(EXTRA_CALLER_NAME);
        if (callerName == null || callerName.isEmpty()) {
            callerName = "Unknown";
        }
        int alertType = getIntent().getIntExtra(EXTRA_ALERT_TYPE,
                com.abhishek.batsignal.service.BatSignalService.ALERT_TYPE_PHONE_CALL);

        TextView title = findViewById(R.id.alert_title);
        TextView subtitle = findViewById(R.id.alert_subtitle);
        Button dismissButton = findViewById(R.id.alert_dismiss);

        title.setText(getString(R.string.alert_title));
        subtitle.setText(getAlertSubtitle(alertType, callerName));
        dismissButton.setText(getString(R.string.alert_dismiss));

        dismissButton.setOnClickListener(v -> dismissAlert());
    }

    @NonNull
    private String getAlertSubtitle(int alertType, @NonNull String callerName) {
        switch (alertType) {
            case com.abhishek.batsignal.service.BatSignalService.ALERT_TYPE_WHATSAPP_VOICE_CALL:
                return getString(R.string.alert_subtitle_whatsapp_voice, callerName);
            case com.abhishek.batsignal.service.BatSignalService.ALERT_TYPE_WHATSAPP_VIDEO_CALL:
                return getString(R.string.alert_subtitle_whatsapp_video, callerName);
            case com.abhishek.batsignal.service.BatSignalService.ALERT_TYPE_WHATSAPP_MESSAGE:
                return getString(R.string.alert_subtitle_whatsapp_message, callerName);
            default:
                return getString(R.string.alert_subtitle, callerName);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String callerName = intent.getStringExtra(EXTRA_CALLER_NAME);
        int alertType = intent.getIntExtra(EXTRA_ALERT_TYPE,
                com.abhishek.batsignal.service.BatSignalService.ALERT_TYPE_PHONE_CALL);
        if (callerName != null) {
            TextView subtitle = findViewById(R.id.alert_subtitle);
            subtitle.setText(getAlertSubtitle(alertType, callerName));
        }
    }

    @Override
    public void onBackPressed() {
        // Ignore back press; user must tap dismiss
    }

    private void dismissAlert() {
        Intent stopIntent = new Intent(this, BatSignalService.class);
        stopIntent.setAction(BatSignalService.ACTION_STOP);
        startService(stopIntent);
        finish();
    }
}

package com.abhishek.batsignal.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.abhishek.batsignal.AlertActivity;
import com.abhishek.batsignal.R;
import com.abhishek.batsignal.service.BatSignalService;

/**
 * Helper for creating notification channels and building notifications.
 * Single Responsibility: centralizes all notification construction logic.
 */
public class NotificationHelper {

    private static final String CHANNEL_ALERT = "alert_channel";
    private static final String CHANNEL_SERVICE = "fg_service_channel";

    private static final int NOTIF_ID_FOREGROUND = 1;
    private static final int NOTIF_ID_ALERT = 2;

    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel alertChannel = new NotificationChannel(
                CHANNEL_ALERT,
                context.getString(R.string.channel_alert_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        alertChannel.setDescription(context.getString(R.string.channel_alert_desc));
        alertChannel.enableVibration(true);
        alertChannel.enableLights(true);

        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_SERVICE,
                context.getString(R.string.channel_service_name),
                NotificationManager.IMPORTANCE_LOW
        );
        serviceChannel.setDescription(context.getString(R.string.channel_service_desc));
        serviceChannel.setShowBadge(false);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(alertChannel);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    @NonNull
    public Notification buildForegroundNotification(@NonNull String callerName, int alertType) {
        PendingIntent stopIntent = createServiceActionIntent(BatSignalService.ACTION_STOP);

        int textRes = getForegroundTextRes(alertType);
        return new NotificationCompat.Builder(context, CHANNEL_SERVICE)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(textRes, callerName))
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(0, context.getString(R.string.action_stop), stopIntent)
                .build();
    }

    @NonNull
    public Notification buildAlertNotification(@NonNull String callerName, int alertType) {
        PendingIntent fullScreenIntent = createAlertActivityPendingIntent(callerName, alertType);
        PendingIntent stopIntent = createServiceActionIntent(BatSignalService.ACTION_STOP);

        int textRes = getAlertTextRes(alertType);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ALERT)
                .setContentTitle(context.getString(R.string.notif_alert_title))
                .setContentText(context.getString(textRes, callerName))
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenIntent, true)
                .addAction(0, context.getString(R.string.action_stop), stopIntent);

        return builder.build();
    }

    public void showAlertNotification(@NonNull String callerName, int alertType) {
        if (notificationManager != null) {
            notificationManager.notify(NOTIF_ID_ALERT, buildAlertNotification(callerName, alertType));
        }
    }

    private static int getForegroundTextRes(int alertType) {
        switch (alertType) {
            case BatSignalService.ALERT_TYPE_WHATSAPP_VOICE_CALL:
                return R.string.notif_fg_whatsapp_voice;
            case BatSignalService.ALERT_TYPE_WHATSAPP_VIDEO_CALL:
                return R.string.notif_fg_whatsapp_video;
            case BatSignalService.ALERT_TYPE_WHATSAPP_MESSAGE:
                return R.string.notif_fg_whatsapp_message;
            default:
                return R.string.notif_fg_text;
        }
    }

    private static int getAlertTextRes(int alertType) {
        switch (alertType) {
            case BatSignalService.ALERT_TYPE_WHATSAPP_VOICE_CALL:
                return R.string.notif_alert_whatsapp_voice;
            case BatSignalService.ALERT_TYPE_WHATSAPP_VIDEO_CALL:
                return R.string.notif_alert_whatsapp_video;
            case BatSignalService.ALERT_TYPE_WHATSAPP_MESSAGE:
                return R.string.notif_alert_whatsapp_message;
            default:
                return R.string.notif_alert_text;
        }
    }

    public void cancelAlertNotification() {
        if (notificationManager != null) {
            notificationManager.cancel(NOTIF_ID_ALERT);
        }
    }

    @NonNull
    private PendingIntent createServiceActionIntent(@NonNull String action) {
        Intent intent = new Intent(context, BatSignalService.class);
        intent.setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getService(context, action.hashCode(), intent, flags);
    }

    @NonNull
    private PendingIntent createAlertActivityPendingIntent(@NonNull String callerName, int alertType) {
        Intent intent = new Intent(context, AlertActivity.class);
        intent.putExtra(AlertActivity.EXTRA_CALLER_NAME, callerName);
        intent.putExtra(AlertActivity.EXTRA_ALERT_TYPE, alertType);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(context, 0, intent, flags);
    }
}

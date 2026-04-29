package com.abhishek.batsignal.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.CallLog;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.abhishek.batsignal.BatSignalApplication;
import com.abhishek.batsignal.data.ContactRepository;
import com.abhishek.batsignal.data.SettingsRepository;
import com.abhishek.batsignal.service.BatSignalService;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * BroadcastReceiver that listens for phone state changes.
 * Detects missed calls from priority contacts and schedules a delayed alert.
 */
public class CallReceiver extends BroadcastReceiver {

    private static final String TAG = "CallReceiver";

    private static final AtomicReference<String> lastState = new AtomicReference<>("");
    private static final AtomicLong lastProcessedCallTime = new AtomicLong(0);

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (state == null) {
            return;
        }

        String previous = lastState.getAndSet(state);
        Log.d(TAG, "PHONE_STATE: " + previous + " -> " + state);

        // Detect missed call: was RINGING, now IDLE, and never went OFFHOOK
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(previous)
                && TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            Log.d(TAG, "Potential missed call detected. Waiting for call log to be written...");
            // Delay query because the system writes call log asynchronously.
            // 800ms is usually enough, but we also check debounce to avoid double-processing.
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                handlePotentialMissedCall(context);
            }, 800);
        }
    }

    private void handlePotentialMissedCall(@NonNull Context context) {
        Log.d(TAG, "handlePotentialMissedCall() running");

        BatSignalApplication app = (BatSignalApplication) context.getApplicationContext();
        SettingsRepository settings = app.getSettingsRepository();
        ContactRepository contacts = app.getContactRepository();

        if (!settings.isEnabled()) {
            Log.d(TAG, "BatSignal is disabled, ignoring call");
            return;
        }

        CallLogEntry entry = queryLatestCallLog(context);
        if (entry == null) {
            Log.d(TAG, "No call log entry found");
            return;
        }

        Log.d(TAG, "Latest call log: name=" + entry.name
                + " number=" + entry.number
                + " type=" + entry.type
                + " date=" + entry.date);

        // Debounce: don't process the same call twice
        long lastProcessed = lastProcessedCallTime.get();
        if (entry.date <= lastProcessed) {
            Log.d(TAG, "Already processed call at " + entry.date + " (last=" + lastProcessed + ")");
            return;
        }

        if (entry.type != CallLog.Calls.MISSED_TYPE) {
            Log.d(TAG, "Latest call is not missed, type=" + entry.type);
            return;
        }

        if (!contacts.isPriorityContact(entry.name, entry.number)) {
            Log.d(TAG, "Caller is not a priority contact: name=" + entry.name + " number=" + entry.number);
            Log.d(TAG, "Saved contacts: " + contacts.getContacts());
            return;
        }

        lastProcessedCallTime.set(entry.date);
        Log.d(TAG, "PRIORITY MISSED CALL from " + entry.name);
        scheduleAlert(context, entry.name, settings.getDelaySeconds());
    }

    private void scheduleAlert(@NonNull Context context, @NonNull String callerName, int delaySeconds) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available");
            return;
        }

        Intent intent = new Intent(context, BatSignalService.class);
        intent.setAction(BatSignalService.ACTION_START);
        intent.putExtra(BatSignalService.EXTRA_CALLER_NAME, callerName);
        intent.putExtra(BatSignalService.EXTRA_ALERT_TYPE, BatSignalService.ALERT_TYPE_PHONE_CALL);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getForegroundService(context, 0, intent, flags);

        long triggerAtMillis = SystemClock.elapsedRealtime() + (delaySeconds * 1000L);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
                Log.d(TAG, "Using exact alarm (canScheduleExactAlarms=true)");
            } else {
                Log.w(TAG, "Exact alarm permission NOT granted! Using inexact alarm.");
                Log.w(TAG, "Go to Settings > Apps > BatSignal > Alarms & reminders to enable exact alarms.");
                alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
        }

        Log.d(TAG, "Scheduled alert for " + callerName + " in " + delaySeconds + "s");
    }

    private CallLogEntry queryLatestCallLog(@NonNull Context context) {
        try (Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[]{
                        CallLog.Calls.CACHED_NAME,
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.TYPE,
                        CallLog.Calls.DATE
                },
                null, null,
                CallLog.Calls.DATE + " DESC")) {

            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }

            String name = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));
            String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
            int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
            long date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));

            return new CallLogEntry(name, number, type, date);
        } catch (SecurityException e) {
            Log.e(TAG, "Missing READ_CALL_LOG permission", e);
            return null;
        }
    }

    private static final class CallLogEntry {
        final String name;
        final String number;
        final int type;
        final long date;

        CallLogEntry(String name, String number, int type, long date) {
            this.name = name;
            this.number = number;
            this.type = type;
            this.date = date;
        }
    }
}

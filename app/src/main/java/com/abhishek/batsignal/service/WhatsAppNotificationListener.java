package com.abhishek.batsignal.service;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.abhishek.batsignal.BatSignalApplication;
import com.abhishek.batsignal.data.ContactRepository;
import com.abhishek.batsignal.data.SettingsRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Notification listener that intercepts WhatsApp notifications.
 * Detects missed voice/video calls and counts unread 1-on-1 messages
 * from priority contacts, triggering BatSignal alerts when thresholds are met.
 */
public class WhatsAppNotificationListener extends NotificationListenerService {

    private static final String TAG = "WhatsAppListener";
    private static final String WHATSAPP_PACKAGE = "com.whatsapp";

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "Notification listener CONNECTED");
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.d(TAG, "Notification listener DISCONNECTED");
    }

    // Per-notification-key message counts (conversation-level tracking)
    private final Map<String, Integer> keyMessageCounts = new HashMap<>();
    // Per-contact total unread message counts across all their notification keys
    private final Map<String, Integer> contactUnreadCounts = new HashMap<>();
    // Contacts we've already alerted for; reset when their unread count drops to zero
    private final Set<String> alertedContacts = new HashSet<>();
    // Deduplicate rapid duplicate notifications for the same contact
    private final Map<String, Long> lastPostTimePerContact = new HashMap<>();
    private static final long DEDUP_WINDOW_MS = 800;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!WHATSAPP_PACKAGE.equals(sbn.getPackageName())) {
            return;
        }

        String title = getTitle(sbn);
        String text = getText(sbn);
        String key = sbn.getKey();

        Log.d(TAG, "Posted: key=" + key + " title=" + title + " text=" + text);
        dumpExtras(sbn);

        if (TextUtils.isEmpty(title)) {
            Log.d(TAG, "Empty title, ignoring");
            return;
        }

        long now = System.currentTimeMillis();
        long lastPost = lastPostTimePerContact.getOrDefault(title, 0L);
        lastPostTimePerContact.put(title, now);
        if (now - lastPost < DEDUP_WINDOW_MS) {
            Log.d(TAG, "Duplicate post for " + title + " within " + (now - lastPost) + "ms, ignoring");
            return;
        }

        if (isMissedCall(text)) {
            handleMissedCall(title, text);
        } else {
            handleMessage(title, sbn);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (!WHATSAPP_PACKAGE.equals(sbn.getPackageName())) {
            return;
        }

        String key = sbn.getKey();
        String title = getTitle(sbn);

        Log.d(TAG, "Removed: title=" + title + " key=" + key);

        if (TextUtils.isEmpty(title)) {
            return;
        }

        int count = keyMessageCounts.getOrDefault(key, 0);
        if (count > 0) {
            int currentTotal = contactUnreadCounts.getOrDefault(title, 0);
            int newTotal = Math.max(0, currentTotal - count);
            contactUnreadCounts.put(title, newTotal);
            if (newTotal == 0) {
                alertedContacts.remove(title);
                Log.d(TAG, "Reset alert state for " + title);
            }
            Log.d(TAG, "Decremented " + title + ": " + currentTotal + " -> " + newTotal);
        }
        keyMessageCounts.remove(key);
    }

    private void handleMissedCall(@NonNull String title, @Nullable String text) {
        if (!isPriorityContact(title)) {
            Log.d(TAG, "Missed call from non-priority: " + title);
            return;
        }

        boolean isVideo = isMissedVideoCall(text);
        int alertType = isVideo
                ? BatSignalService.ALERT_TYPE_WHATSAPP_VIDEO_CALL
                : BatSignalService.ALERT_TYPE_WHATSAPP_VOICE_CALL;

        String typeLabel = isVideo ? "video" : "voice";
        Log.d(TAG, "PRIORITY missed WhatsApp " + typeLabel + " call from " + title);
        triggerAlert(title, alertType);
    }

    private void handleMessage(@NonNull String title, @NonNull StatusBarNotification sbn) {
        if (!isPriorityContact(title)) {
            Log.d(TAG, "Message from non-priority: " + title);
            return;
        }

        SettingsRepository settings = getSettings();
        if (settings == null || !settings.isWhatsappEnabled()) {
            Log.d(TAG, "WhatsApp monitoring disabled");
            return;
        }

        int messageCount = countMessages(sbn);
        String key = sbn.getKey();

        boolean isNewKey = !keyMessageCounts.containsKey(key);
        int oldCount = keyMessageCounts.getOrDefault(key, 0);
        keyMessageCounts.put(key, messageCount);

        // WhatsApp typically updates the same notification key for new messages in the
        // same conversation. We can't reliably parse the exact delta because the
        // notification format varies (inbox-style lines vs single text). Safest
        // assumption: every post for an existing key means at least 1 new message.
        int delta;
        if (isNewKey) {
            delta = Math.max(1, messageCount);
        } else {
            delta = 1;
        }

        Log.d(TAG, "handleMessage: key=" + key + " isNew=" + isNewKey
                + " parsedCount=" + messageCount + " oldCount=" + oldCount
                + " delta=" + delta);

        int newTotal = contactUnreadCounts.getOrDefault(title, 0) + delta;
        contactUnreadCounts.put(title, newTotal);

        Log.d(TAG, "Message count for " + title + ": +" + delta + " = " + newTotal);

        if (alertedContacts.contains(title)) {
            Log.d(TAG, "Already alerted for " + title + ", skipping");
            return;
        }

        int threshold = settings.getWhatsappMessageThreshold();
        if (newTotal >= threshold) {
            alertedContacts.add(title);
            Log.d(TAG, "Threshold reached (" + newTotal + "/" + threshold + "), alerting for " + title);
            triggerAlert(title, BatSignalService.ALERT_TYPE_WHATSAPP_MESSAGE);
        } else {
            Log.d(TAG, "Below threshold (" + newTotal + "/" + threshold + "), waiting");
        }
    }

    private void triggerAlert(@NonNull String contactName, int alertType) {
        Intent intent = new Intent(this, BatSignalService.class);
        intent.setAction(BatSignalService.ACTION_START);
        intent.putExtra(BatSignalService.EXTRA_CALLER_NAME, contactName);
        intent.putExtra(BatSignalService.EXTRA_ALERT_TYPE, alertType);
        try {
            startForegroundService(intent);
            Log.d(TAG, "triggerAlert: started service for " + contactName);
        } catch (Exception e) {
            Log.e(TAG, "triggerAlert: failed to start service", e);
        }
    }

    private boolean isPriorityContact(@NonNull String displayName) {
        ContactRepository contacts = getContacts();
        if (contacts == null) {
            return false;
        }
        return contacts.isPriorityContactByName(displayName);
    }

    private boolean isMissedCall(@Nullable String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return lower.contains("missed") && lower.contains("call");
    }

    private boolean isMissedVideoCall(@Nullable String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return lower.contains("missed") && lower.contains("video");
    }

    @Nullable
    private String getTitle(@NonNull StatusBarNotification sbn) {
        CharSequence title = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE);
        return title != null ? title.toString().trim() : null;
    }

    @Nullable
    private String getText(@NonNull StatusBarNotification sbn) {
        CharSequence text = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
        return text != null ? text.toString().trim() : null;
    }

    private int countMessages(@NonNull StatusBarNotification sbn) {
        Bundle extras = sbn.getNotification().extras;

        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null && lines.length > 0) {
            return lines.length;
        }

        try {
            Parcelable[] messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
            if (messages != null && messages.length > 0) {
                return messages.length;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse messaging style", e);
        }

        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
        if (!TextUtils.isEmpty(text)) {
            return 1;
        }

        return 0;
    }

    private void dumpExtras(@NonNull StatusBarNotification sbn) {
        Bundle extras = sbn.getNotification().extras;
        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        Parcelable[] messages = null;
        try {
            messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        } catch (Exception ignored) {
        }
        Log.d(TAG, "dumpExtras: EXTRA_TITLE=" + extras.getCharSequence(Notification.EXTRA_TITLE)
                + " EXTRA_TEXT=" + extras.getCharSequence(Notification.EXTRA_TEXT)
                + " EXTRA_TEXT_LINES=" + (lines != null ? lines.length : "null")
                + " EXTRA_MESSAGES=" + (messages != null ? messages.length : "null")
                + " ticker=" + sbn.getNotification().tickerText);
    }

    @Nullable
    private SettingsRepository getSettings() {
        if (getApplication() instanceof BatSignalApplication) {
            return ((BatSignalApplication) getApplication()).getSettingsRepository();
        }
        return null;
    }

    @Nullable
    private ContactRepository getContacts() {
        if (getApplication() instanceof BatSignalApplication) {
            return ((BatSignalApplication) getApplication()).getContactRepository();
        }
        return null;
    }
}

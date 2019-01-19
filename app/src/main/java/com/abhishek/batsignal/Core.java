package com.abhishek.batsignal;

import android.database.Cursor;
import android.os.Vibrator;
import android.provider.CallLog;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Core {
    private String[] contactList = {"dad", "omkar", "mom"};
    private Cursor managedCursor;
    private Notifier notifier;


    public Core(Cursor cursor, Vibrator vibrator) {
        this.managedCursor = cursor;
        notifier = new Notifier(vibrator);
    }

    public void run() {
        List<String> pendingCallslist = getPendingCallsList();

        if (pendingCallslist.size() > 0) {
            scheduleAlarms(pendingCallslist);
        }
    }

    private List<String> getPendingCallsList() {
        List<String> resultList = new ArrayList<>();

        for (int i = 0; i < contactList.length; i++) {
            if (isCallPendingFor(contactList[i]))
                resultList.add(contactList[i]);
        }

        return resultList;
    }

    private void scheduleAlarms(List<String> pendingCallsList) {
        // TODO: schedule alarm for 5 minutes after missed call

        notifier.startAlarms(pendingCallsList);
    }

    private boolean isCallPendingFor(String contactName) {

        //Cursor managedCursor = this.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE);

        int numberColumnIndex = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int nameColumnIndex = managedCursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
        int callTypeColumnIndex = managedCursor.getColumnIndex(CallLog.Calls.TYPE);

        // start from latest call
        managedCursor.moveToLast();

        while (!(managedCursor.getString(nameColumnIndex) == null ? "" : managedCursor.getString(nameColumnIndex))
                .equalsIgnoreCase(contactName)) {
            Log.d("DEBUGTAG", managedCursor.getString(nameColumnIndex) == null ? "null" : managedCursor.getString(nameColumnIndex));
            managedCursor.moveToPrevious();
        }

        if (Integer.parseInt(managedCursor.getString(callTypeColumnIndex)) != CallLog.Calls.MISSED_TYPE
                && (Integer.parseInt(managedCursor.getString(callTypeColumnIndex)) == CallLog.Calls.INCOMING_TYPE
                || Integer.parseInt(managedCursor.getString(callTypeColumnIndex)) == CallLog.Calls.OUTGOING_TYPE))
            return false;

        return true;
    }
}
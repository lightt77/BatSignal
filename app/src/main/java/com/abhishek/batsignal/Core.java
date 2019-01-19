package com.abhishek.batsignal;

import android.database.Cursor;
import android.os.Vibrator;
import android.provider.CallLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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

    private List<String> getPendingCallsList()
    {
        List<String> resultList = new ArrayList<>();

        HashSet<String> contactHashSet = new HashSet<>(Arrays.asList(contactList));

        int nameColumnIndex = managedCursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
        int callTypeColumnIndex = managedCursor.getColumnIndex(CallLog.Calls.TYPE);

        // start from latest call
        managedCursor.moveToLast();

        do {
            if(managedCursor.getString(nameColumnIndex) != null
                    && contactHashSet.contains(managedCursor.getString(nameColumnIndex).toLowerCase()))
            {
                contactHashSet.remove(managedCursor.getString(nameColumnIndex).toLowerCase());

                if(Integer.parseInt(managedCursor.getString(callTypeColumnIndex)) == CallLog.Calls.MISSED_TYPE)
                    resultList.add(managedCursor.getString(nameColumnIndex));
            }
        }
        while (managedCursor.moveToPrevious());

        return resultList;
    }

    private void scheduleAlarms(List<String> pendingCallsList) {
        // TODO: schedule alarm for 5 minutes after missed call

        notifier.startAlarms(pendingCallsList);
    }
}
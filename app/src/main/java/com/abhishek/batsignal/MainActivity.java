package com.abhishek.batsignal;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.CallLog;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String[] contatList = {"dad", "omkar", "mom"};
    private Core core;
    private Cursor managedCursor;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("MYAPP", "Abhishek");

        Log.d("Abhishek", CallLog.Calls.CACHED_FORMATTED_NUMBER);
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_CALL_LOG}, 1);
    }

    private void printCallLogs(Cursor managedCursor) {
        StringBuilder output = new StringBuilder();

        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int name = managedCursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
        int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);

        while (managedCursor.moveToNext()) {
            Log.d("Abhishek", managedCursor.getString(number) + " " + managedCursor.getString(name));
        }

        managedCursor.moveToLast();

        Log.d("last entry", managedCursor.getString(number));
        Log.d("last entry", managedCursor.getString(name));
        Log.d("last entry type", (Integer.parseInt(managedCursor.getString(type)) == CallLog.Calls.MISSED_TYPE) ? "missed" : "something else");

    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Abhishek", "Permission granted");

                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    Cursor managedCursor = this.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE);
                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

                    //printCallLogs(managedCursor);

                    core = new Core(managedCursor, vibrator);
                    core.run();
                } else
                    Log.d("Abhishek", "Permission denied");
            }

        }
    }
}

class Core {
    private String[] contactList = {"dad", "omkar", "mom"};
    private Cursor managedCursor;
    private Vibrator vibrator;

    public Core(Cursor cursor, Vibrator vibrator) {
        this.managedCursor = cursor;
        this.vibrator = vibrator;
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

        startAlarms(pendingCallsList);
    }

    private void startAlarms(List<String> pendingCallsList) {
        String pendingCalls = "";

        for (String contact : pendingCallsList) {
            pendingCalls += contact + " ";
        }

        Log.d("Abhishek", "Alarm started for " + pendingCalls);

        if(pendingCallsList.size()>0)
            vibrate();
    }

    private void vibrate() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE));
        else
            vibrator.vibrate(250);
    }

    private boolean isCallPendingFor(String contactName) {

        //Cursor managedCursor = this.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE);

        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int name = managedCursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
        int callType = managedCursor.getColumnIndex(CallLog.Calls.TYPE);

        managedCursor.moveToLast();

        while (!managedCursor.getString(name).equalsIgnoreCase(contactName)) {
            Log.d("abhishek", managedCursor.getString(name));
            managedCursor.moveToPrevious();
        }

        if (Integer.parseInt(managedCursor.getString(callType)) != CallLog.Calls.MISSED_TYPE
                && (Integer.parseInt(managedCursor.getString(callType)) == CallLog.Calls.INCOMING_TYPE
                || Integer.parseInt(managedCursor.getString(callType)) == CallLog.Calls.OUTGOING_TYPE))
            return false;

        return true;
    }
}
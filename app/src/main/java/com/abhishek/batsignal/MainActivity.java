package com.abhishek.batsignal;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.CallLog;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private Core core;
    private Cursor managedCursor;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("DEBUGTAG", "onCreate() called..");

        Log.d("DEBUGTAG", CallLog.Calls.CACHED_FORMATTED_NUMBER);

        // request permissions at start if not granted already
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_CALL_LOG}, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("DEBUGTAG", "onResume() called..");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            // don't proceed if permissions not granted
            return;
        }
        //printCallLogs(managedCursor);

        // TODO: find a way to initialise these objects only once; can't initialise the objects in onCreate() as onResume() is called before the permission grant dialogbox is resolved..
        // TODO: make use of singleton pattern?
        managedCursor = this.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        core = new Core(managedCursor, vibrator);

        core.run();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("DEBUGTAG", "onRequestPermissionsResult() called..");

        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("DEBUGTAG", "Permission granted");
//                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
//                        return;
//                    }

                } else
                    Log.d("DEBUGTAG", "Permission denied");
            }
        }
    }

    private void printCallLogs(Cursor managedCursor) {
        StringBuilder output = new StringBuilder();

        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int name = managedCursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
        int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);

        while (managedCursor.moveToNext()) {
            Log.d("DEBUGTAG", managedCursor.getString(number) + " " + managedCursor.getString(name));
        }

        managedCursor.moveToLast();

        Log.d("last entry", managedCursor.getString(number));
        Log.d("last entry", managedCursor.getString(name));
        Log.d("last entry type", (Integer.parseInt(managedCursor.getString(type)) == CallLog.Calls.MISSED_TYPE) ? "missed" : "something else");
    }
}
package com.abhishek.batsignal;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private String[] contatList={"dad","omkar","mom"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("MYAPP", "Abhishek");

        Log.d("Abhishek", CallLog.Calls.CACHED_FORMATTED_NUMBER);
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_CALL_LOG}, 1);
    }

    private void printCallLogs() {
        StringBuilder output = new StringBuilder();

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
        Cursor managedCursor = MainActivity.this.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE);

        int number=managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int name=managedCursor.getColumnIndex(CallLog.Calls.CACHED_NAME);

//        while (managedCursor.moveToNext())
//        {
//            Log.d("Abhishek",managedCursor.getString(number));
//        }

        managedCursor.moveToLast();

        Log.d("last entry",managedCursor.getString(number));
        Log.d("last entry",managedCursor.getString(name));

    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case 1:{
                if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED)
                {
                    Log.d("Abhishek","Permission granted");
                    printCallLogs();
                }
                else
                    Log.d("Abhishek","Permission denied");
            }

        }
    }
}
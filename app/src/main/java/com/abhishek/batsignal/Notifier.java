package com.abhishek.batsignal;

import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import java.util.List;

public class Notifier {
    private Vibrator vibrator;

    public Notifier(Vibrator vibrator) {
        this.vibrator = vibrator;
    }

    public void startAlarms(List<String> pendingCallsList) {
        String pendingCalls = "";

        for (String contact : pendingCallsList) {
            pendingCalls += contact + " ";
        }

        Log.d("DEBUGTAG", "Alarm started for " + pendingCalls);

        if(pendingCallsList.size()>0)
            vibrate();
    }

    private void vibrate() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE));
        else
            vibrator.vibrate(250);
    }


}

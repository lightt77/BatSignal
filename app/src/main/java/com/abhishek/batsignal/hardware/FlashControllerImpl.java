package com.abhishek.batsignal.hardware;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Concrete implementation of {@link FlashController} using CameraManager torch mode.
 * Executes patterns on a dedicated background thread to avoid blocking the caller.
 */
public class FlashControllerImpl implements FlashController {

    private static final String TAG = "FlashController";

    private final CameraManager cameraManager;
    private final String cameraId;
    private HandlerThread handlerThread;
    private Handler handler;
    private volatile boolean running = false;

    public FlashControllerImpl(@NonNull Context context) {
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        String id = null;
        try {
            if (cameraManager != null) {
                String[] ids = cameraManager.getCameraIdList();
                if (ids.length > 0) {
                    id = ids[0];
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to access camera", e);
        }
        this.cameraId = id;
    }

    @Override
    public void startPattern(@Nullable int[] onDurations, @Nullable int[] offDurations) {
        if (cameraManager == null || cameraId == null || onDurations == null || offDurations == null) {
            return;
        }
        if (onDurations.length == 0) {
            return;
        }

        stop(); // ensure clean state

        handlerThread = new HandlerThread("FlashThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        running = true;

        handler.post(() -> executePattern(onDurations, offDurations));
    }

    private void executePattern(@NonNull int[] onDurations, @NonNull int[] offDurations) {
        try {
            for (int i = 0; i < onDurations.length && running; i++) {
                int onMs = Math.max(0, onDurations[i]);
                int offMs = (i < offDurations.length) ? Math.max(0, offDurations[i]) : 0;

                if (onMs > 0) {
                    cameraManager.setTorchMode(cameraId, true);
                }
                sleep(onMs);

                if (!running) break;

                if (offMs > 0 || i < onDurations.length - 1) {
                    cameraManager.setTorchMode(cameraId, false);
                    sleep(offMs);
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Torch mode error", e);
        } finally {
            turnOffSafe();
        }
    }

    @Override
    public void stop() {
        running = false;
        turnOffSafe();
        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
        }
        handler = null;
    }

    private void turnOffSafe() {
        if (cameraManager != null && cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId, false);
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to turn off torch", e);
            }
        }
    }

    private void sleep(int millis) {
        if (millis <= 0) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}

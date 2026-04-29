package com.abhishek.batsignal.hardware;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Abstraction for device vibration hardware.
 */
public interface VibrationController {

    /**
     * Starts vibrating with the given pattern.
     * Pattern format: alternating wait/vibrate durations in milliseconds.
     * The first value is the wait time before vibration starts.
     *
     * @param pattern vibration pattern, or null to do nothing
     */
    void startPattern(@Nullable long[] pattern);

    /**
     * Immediately cancels any ongoing vibration.
     */
    void stop();
}

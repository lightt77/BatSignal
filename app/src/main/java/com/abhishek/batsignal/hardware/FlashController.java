package com.abhishek.batsignal.hardware;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Abstraction for device camera flash hardware.
 */
public interface FlashController {

    /**
     * Starts a repeating flash pattern.
     *
     * @param onDurations  milliseconds to keep flash on per cycle
     * @param offDurations milliseconds to keep flash off per cycle
     */
    void startPattern(@Nullable int[] onDurations, @Nullable int[] offDurations);

    /**
     * Immediately turns off the flash and cancels any ongoing pattern.
     */
    void stop();
}

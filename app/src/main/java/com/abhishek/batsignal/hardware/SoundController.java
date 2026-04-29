package com.abhishek.batsignal.hardware;

import androidx.annotation.NonNull;

/**
 * Abstraction for alert sound playback.
 */
public interface SoundController {

    /**
     * Starts playing the alert sound on the alarm stream.
     *
     * @param overrideVolume if true, temporarily sets alarm volume to maximum
     */
    void start(boolean overrideVolume);

    /**
     * Stops playback and restores previous volume if it was overridden.
     */
    void stop();
}

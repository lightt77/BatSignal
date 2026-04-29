package com.abhishek.batsignal.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Provides vibration and flash patterns for named presets and custom configurations.
 * Single Responsibility: decouples pattern definitions from execution logic.
 */
public final class AlertPatternProvider {

    private AlertPatternProvider() {
        // utility class
    }

    @Nullable
    public static long[] getVibrationPattern(@NonNull String presetName,
                                              @NonNull long[] customPattern) {
        switch (presetName) {
            case "Off":
                return null;
            case "Short Buzz":
                return new long[]{0, 250};
            case "Long Pulse":
                return new long[]{0, 1000};
            case "Double Tap":
                return new long[]{0, 200, 150, 200};
            case "SOS":
                return new long[]{0, 200, 100, 200, 100, 600, 100, 600, 100, 200, 100, 200, 100, 600};
            case "Heartbeat":
                return new long[]{0, 100, 100, 100, 400, 100, 100, 100};
            case "Custom":
                return customPattern.length > 0 ? customPattern : new long[]{0, 250};
            default:
                return new long[]{0, 250};
        }
    }

    @Nullable
    public static FlashPattern getFlashPattern(@NonNull String presetName,
                                                @NonNull int[] customOn,
                                                @NonNull int[] customOff) {
        switch (presetName) {
            case "Off":
                return null;
            case "Single":
                return new FlashPattern(new int[]{500}, new int[]{0});
            case "Strobe":
                return new FlashPattern(
                        new int[]{50, 50, 50, 50, 50},
                        new int[]{50, 50, 50, 50, 0}
                );
            case "Slow Pulse":
                return new FlashPattern(new int[]{300}, new int[]{700});
            case "SOS":
                return new FlashPattern(
                        new int[]{200, 200, 200, 600, 600, 600, 200, 200, 200},
                        new int[]{200, 200, 600, 200, 200, 200, 200, 200, 0}
                );
            case "Custom":
                return new FlashPattern(customOn, customOff);
            default:
                return new FlashPattern(new int[]{500}, new int[]{0});
        }
    }

    /**
     * Immutable data holder for flash on/off durations.
     */
    public static final class FlashPattern {
        private final int[] onDurations;
        private final int[] offDurations;

        public FlashPattern(@NonNull int[] onDurations, @NonNull int[] offDurations) {
            this.onDurations = onDurations.clone();
            this.offDurations = offDurations.clone();
        }

        @NonNull
        public int[] getOnDurations() {
            return onDurations.clone();
        }

        @NonNull
        public int[] getOffDurations() {
            return offDurations.clone();
        }
    }
}

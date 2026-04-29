package com.abhishek.batsignal.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Immutable data class representing a priority contact.
 */
public final class Contact {

    private final String displayName;
    private final String phoneNumber;

    public Contact(@NonNull String displayName, @Nullable String phoneNumber) {
        this.displayName = displayName;
        this.phoneNumber = phoneNumber != null ? phoneNumber : "";
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Returns a normalized phone number suitable for loose matching.
     * Strips all non-digit characters.
     */
    @NonNull
    public String getNormalizedPhoneNumber() {
        return phoneNumber.replaceAll("\\D", "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Contact)) return false;
        Contact contact = (Contact) o;
        return displayName.equals(contact.displayName)
                && phoneNumber.equals(contact.phoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, phoneNumber);
    }

    @NonNull
    @Override
    public String toString() {
        return displayName;
    }
}

package com.abhishek.batsignal.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repository for priority contacts. Backed by SharedPreferences with JSON serialization.
 * Thread-safe via synchronized methods.
 */
public class ContactRepository {

    private static final String PREFS_NAME = "batsignal_contacts";
    private static final String KEY_CONTACTS = "contacts";

    private final SharedPreferences prefs;
    private final Gson gson;
    private final Type contactListType;

    public ContactRepository(@NonNull Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.contactListType = new TypeToken<List<Contact>>() {}.getType();
    }

    public synchronized void addContact(@NonNull Contact contact) {
        List<Contact> contacts = getContactsInternal();
        // Prevent duplicates by normalized phone number
        for (Contact existing : contacts) {
            if (existing.getNormalizedPhoneNumber().equals(contact.getNormalizedPhoneNumber())) {
                return;
            }
        }
        contacts.add(contact);
        saveContacts(contacts);
    }

    public synchronized void removeContact(@NonNull String normalizedPhoneNumber) {
        List<Contact> contacts = getContactsInternal();
        contacts.removeIf(c -> c.getNormalizedPhoneNumber().equals(normalizedPhoneNumber));
        saveContacts(contacts);
    }

    @NonNull
    public synchronized List<Contact> getContacts() {
        return Collections.unmodifiableList(getContactsInternal());
    }

    /**
     * Checks whether the given call details match any stored priority contact.
     * Matches on display name (case-insensitive) OR normalized phone number.
     */
    public synchronized boolean isPriorityContact(@Nullable String displayName, @Nullable String phoneNumber) {
        if (getContactsInternal().isEmpty()) {
            return false;
        }
        String normalizedIncoming = phoneNumber != null ? phoneNumber.replaceAll("\\D", "") : "";
        for (Contact contact : getContactsInternal()) {
            boolean nameMatch = displayName != null
                    && !displayName.isEmpty()
                    && contact.getDisplayName().equalsIgnoreCase(displayName);
            boolean numberMatch = !normalizedIncoming.isEmpty()
                    && contact.getNormalizedPhoneNumber().equals(normalizedIncoming);
            if (nameMatch || numberMatch) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the given display name matches any stored priority contact by name only.
     * Used for WhatsApp notification matching where phone number is not available.
     */
    public synchronized boolean isPriorityContactByName(@Nullable String displayName) {
        if (getContactsInternal().isEmpty() || displayName == null || displayName.isEmpty()) {
            return false;
        }
        for (Contact contact : getContactsInternal()) {
            if (contact.getDisplayName().equalsIgnoreCase(displayName)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private List<Contact> getContactsInternal() {
        String json = prefs.getString(KEY_CONTACTS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        List<Contact> list = gson.fromJson(json, contactListType);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    private void saveContacts(@NonNull List<Contact> contacts) {
        String json = gson.toJson(contacts);
        prefs.edit().putString(KEY_CONTACTS, json).apply();
    }
}

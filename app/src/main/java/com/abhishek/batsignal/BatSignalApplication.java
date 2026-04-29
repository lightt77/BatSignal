package com.abhishek.batsignal;

import android.app.Application;

import com.abhishek.batsignal.data.ContactRepository;
import com.abhishek.batsignal.data.SettingsRepository;

/**
 * Application class that provides singleton access to repositories.
 */
public class BatSignalApplication extends Application {

    private ContactRepository contactRepository;
    private SettingsRepository settingsRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        this.contactRepository = new ContactRepository(this);
        this.settingsRepository = new SettingsRepository(this);
    }

    public ContactRepository getContactRepository() {
        return contactRepository;
    }

    public SettingsRepository getSettingsRepository() {
        return settingsRepository;
    }
}

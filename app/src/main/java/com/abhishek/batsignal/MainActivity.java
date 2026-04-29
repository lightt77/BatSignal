package com.abhishek.batsignal;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.abhishek.batsignal.ui.ContactsFragment;
import com.abhishek.batsignal.ui.PermissionHelper;
import com.abhishek.batsignal.ui.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Main entry point. Hosts the bottom navigation and permission wizard.
 */
public class MainActivity extends AppCompatActivity {

    private PermissionHelper permissionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionHelper = new PermissionHelper(this, () -> {
            // permissions complete — normal operation
        });
        permissionHelper.startIfNeeded();

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_contacts) {
                showFragment(new ContactsFragment());
                return true;
            } else if (itemId == R.id.nav_settings) {
                showFragment(new SettingsFragment());
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            showFragment(new ContactsFragment());
            nav.setSelectedItemId(R.id.nav_contacts);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHelper.onRequestPermissionsResult(requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissionHelper.onActivityResult();
    }

    private void showFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}

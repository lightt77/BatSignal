package com.abhishek.batsignal.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abhishek.batsignal.BatSignalApplication;
import com.abhishek.batsignal.R;
import com.abhishek.batsignal.data.Contact;
import com.abhishek.batsignal.data.ContactRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Fragment for managing the priority contact list.
 */
public class ContactsFragment extends Fragment {

    private static final int REQ_PICK_CONTACT = 1;

    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private TextView emptyView;
    private ContactRepository contactRepository;

    private final ActivityResultLauncher<Intent> contactPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleContactResult(result.getData().getData());
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        contactRepository = ((BatSignalApplication) requireActivity().getApplication())
                .getContactRepository();

        recyclerView = view.findViewById(R.id.recycler_contacts);
        emptyView = view.findViewById(R.id.empty_contacts);
        FloatingActionButton fab = view.findViewById(R.id.fab_add_contact);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ContactAdapter();
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v -> openContactPicker());

        setupSwipeToDelete();
        refreshList();
    }

    private void openContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        contactPickerLauncher.launch(intent);
    }

    private void handleContactResult(@Nullable Uri contactUri) {
        if (contactUri == null || getContext() == null) {
            return;
        }

        String contactId = null;
        String displayName = null;

        try (Cursor cursor = getContext().getContentResolver().query(contactUri,
                new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                contactId = cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                displayName = cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
            }
        }

        if (contactId == null || displayName == null) {
            Toast.makeText(getContext(), R.string.contact_pick_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        String phoneNumber = resolvePhoneNumber(contactId);
        Contact contact = new Contact(displayName, phoneNumber);
        contactRepository.addContact(contact);
        refreshList();
    }

    @Nullable
    private String resolvePhoneNumber(@NonNull String contactId) {
        if (getContext() == null) {
            return null;
        }
        try (Cursor cursor = getContext().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{contactId},
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
            }
        }
        return null;
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Contact contact = adapter.getItem(position);
                contactRepository.removeContact(contact.getNormalizedPhoneNumber());
                refreshList();
            }
        });
        helper.attachToRecyclerView(recyclerView);
    }

    private void refreshList() {
        List<Contact> contacts = contactRepository.getContacts();
        adapter.submitList(contacts);
        emptyView.setVisibility(contacts.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(contacts.isEmpty() ? View.GONE : View.VISIBLE);
    }
}

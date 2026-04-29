package com.abhishek.batsignal.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abhishek.batsignal.R;
import com.abhishek.batsignal.data.Contact;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying priority contacts.
 */
public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private final List<Contact> contacts = new ArrayList<>();

    public void submitList(@NonNull List<Contact> newContacts) {
        contacts.clear();
        contacts.addAll(newContacts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.nameText.setText(contact.getDisplayName());
        String number = contact.getPhoneNumber();
        holder.numberText.setText(number.isEmpty() ? "No number" : number);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    @NonNull
    public Contact getItem(int position) {
        return contacts.get(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final TextView numberText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.contact_name);
            numberText = itemView.findViewById(R.id.contact_number);
        }
    }
}

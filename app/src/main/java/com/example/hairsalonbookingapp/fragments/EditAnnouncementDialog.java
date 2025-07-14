package com.example.hairsalonbookingapp.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.hairsalonbookingapp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.widget.Button;

public class EditAnnouncementDialog extends DialogFragment {
    private EditText announcementEditText;
    private Button updateButton, cancelButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_announcement, container, false);
        announcementEditText = view.findViewById(R.id.announcement_text_input);
        updateButton = view.findViewById(R.id.update_button);
        cancelButton = view.findViewById(R.id.cancel_button);

        DatabaseReference announcementsRef = FirebaseDatabase.getInstance().getReference("announcements")
                .child("current_announcement");

        // read the current announcement and display it in the dialog
        announcementsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String existingText = snapshot.getValue(String.class);
                if (existingText != null) {
                    announcementEditText.setText(existingText);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load announcement", Toast.LENGTH_SHORT).show();
            }
        });

        updateButton.setOnClickListener(v -> {
            String updatedText = announcementEditText.getText().toString().trim();
            if (!updatedText.isEmpty()) {
                announcementsRef.setValue(updatedText)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(requireContext(), "Announcement updated", Toast.LENGTH_SHORT).show();
                                dismiss();
                            } else {
                                Toast.makeText(requireContext(), "Failed to update announcement", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
            } else {
                Toast.makeText(requireContext(), "Please enter a valid announcement", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}
package com.example.hairsalonbookingapp.fragments;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
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

import java.util.Calendar;
import java.util.Locale;

public class OpeningHoursDialog extends DialogFragment {

    private TextView openingTimeText;
    private TextView closingTimeText;
    private Button saveButton;
    private Button cancelButton;
    private int openingHour = 8;
    private int openingMinute = 0;
    private int closingHour = 20;
    private int closingMinute = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_opening_hours, container, false);

        openingTimeText = view.findViewById(R.id.opening_time_text);
        closingTimeText = view.findViewById(R.id.closing_time_text);
        saveButton = view.findViewById(R.id.save_button);
        cancelButton = view.findViewById(R.id.cancel_button);

        // Load current opening hours from Firebase
        loadCurrentHours();

        // Set up opening time picker
        openingTimeText.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    requireContext(),
                    (view1, hourOfDay, minute) -> {
                        openingHour = hourOfDay;
                        openingMinute = minute;
                        updateOpeningTimeDisplay();
                    },
                    openingHour, openingMinute, true);
            timePickerDialog.show();
        });

        // Set up closing time picker
        closingTimeText.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    requireContext(),
                    (view1, hourOfDay, minute) -> {
                        closingHour = hourOfDay;
                        closingMinute = minute;
                        updateClosingTimeDisplay();
                    },
                    closingHour, closingMinute, true);
            timePickerDialog.show();
        });

        // Set up save button
        saveButton.setOnClickListener(v -> {
            if (validateHours()) {
                saveHours();
            }
        });

        // Set up cancel button
        cancelButton.setOnClickListener(v -> dismiss());

        return view;
    }

    private void loadCurrentHours() {
        DatabaseReference hoursRef = FirebaseDatabase.getInstance().getReference("opening_hours");
        hoursRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer openHr = snapshot.child("opening_hour").getValue(Integer.class);
                    Integer openMin = snapshot.child("opening_minute").getValue(Integer.class);
                    Integer closeHr = snapshot.child("closing_hour").getValue(Integer.class);
                    Integer closeMin = snapshot.child("closing_minute").getValue(Integer.class);

                    openingHour = openHr != null ? openHr : 8;
                    openingMinute = openMin != null ? openMin : 0;
                    closingHour = closeHr != null ? closeHr : 20;
                    closingMinute = closeMin != null ? closeMin : 0;

                    updateOpeningTimeDisplay();
                    updateClosingTimeDisplay();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load opening hours", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateOpeningTimeDisplay() {
        String timeString = String.format(Locale.getDefault(), "%02d:%02d", openingHour, openingMinute);
        openingTimeText.setText(timeString);
    }

    private void updateClosingTimeDisplay() {
        String timeString = String.format(Locale.getDefault(), "%02d:%02d", closingHour, closingMinute);
        closingTimeText.setText(timeString);
    }

    private boolean validateHours() {
        // Convert to minutes for easier comparison
        int openingMinutes = openingHour * 60 + openingMinute;
        int closingMinutes = closingHour * 60 + closingMinute;

        if (openingMinutes >= closingMinutes) {
            Toast.makeText(requireContext(), "Opening time must be before closing time", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (closingMinutes - openingMinutes < 60) {
            Toast.makeText(requireContext(), "Salon must be open for at least 1 hour", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveHours() {
        DatabaseReference hoursRef = FirebaseDatabase.getInstance().getReference("opening_hours");

        // Create a map with all the values to save them atomically
        java.util.Map<String, Object> hoursMap = new java.util.HashMap<>();
        hoursMap.put("opening_hour", openingHour);
        hoursMap.put("opening_minute", openingMinute);
        hoursMap.put("closing_hour", closingHour);
        hoursMap.put("closing_minute", closingMinute);

        hoursRef.setValue(hoursMap).addOnSuccessListener(aVoid -> {
            Toast.makeText(requireContext(), "Opening hours updated successfully", Toast.LENGTH_SHORT).show();
            dismiss();
        })
                .addOnFailureListener(e -> {
                    String errorMessage = "Failed to update opening hours: " + e.getMessage();
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                    android.util.Log.e("OpeningHours", errorMessage, e);
                });
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
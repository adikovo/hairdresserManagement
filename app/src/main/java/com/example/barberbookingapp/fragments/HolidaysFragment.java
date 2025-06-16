package com.example.barberbookingapp.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.barberbookingapp.R;
import com.example.barberbookingapp.adapters.HolidaysAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/*
This fragment is shown after clicking the manage holidays button in the WorkersFragment
It displays a list of all holidays for the hair salon
Each holiday is shown with a date and a delete button
There is also a button to add a new holiday to the list
* */

public class HolidaysFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView emptyText;
    private List<String> holidaysList;
    private HolidaysAdapter adapter;
    private DatabaseReference databaseReference;
    private String currentUsername;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_holidays, container, false);

        recyclerView = view.findViewById(R.id.holidays_recycler_view);
        emptyText = view.findViewById(R.id.empty_holidays_text);
        Button addHolidayButton = view.findViewById(R.id.add_holiday_button);

        holidaysList = new ArrayList<>();
        
        // Get current user's username
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUsername = snapshot.getValue(String.class);
                if (currentUsername != null) {
                    // Initialize database reference with username
                    databaseReference = FirebaseDatabase.getInstance().getReference("holidays").child(currentUsername);
                    
                    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    adapter = new HolidaysAdapter(holidaysList, databaseReference);
                    recyclerView.setAdapter(adapter);
                    
                    loadHolidays();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });

        //When clicking the add holiday button, call the function that performs the action
        addHolidayButton.setOnClickListener(v -> openDatePicker());

        return view;
    }

    //Function that adds a holiday to the list
    //Opens a calendar where you can select a date for the holiday and adds it in the desired format
    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    Log.d("HolidaysFragment", "Selected date: " + selectedDate);
                    databaseReference.child(selectedDate).setValue(true)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("HolidaysFragment", "Holiday added successfully");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("HolidaysFragment", "Failed to add holiday", e);
                        });
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    //Load all holidays and display their updates
    private void loadHolidays() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                holidaysList.clear();
                for (DataSnapshot holidaySnap : snapshot.getChildren()) {
                    String holiday = holidaySnap.getKey();
                    holidaysList.add(holiday);
                    Log.d("HolidaysFragment", "Loaded holiday: " + holiday);
                }
                adapter.notifyDataSetChanged();
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HolidaysFragment", "Failed to load holidays", error.toException());
                Toast.makeText(getContext(), "Failed to load holidays", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Function that helps display what needs to be shown based on the list state
    private void updateUI() {
        if (holidaysList.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}

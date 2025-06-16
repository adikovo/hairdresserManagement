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
    private List<HolidaysAdapter.HolidayItem> holidaysList;
    private HolidaysAdapter adapter;
    private DatabaseReference databaseReference;
    private DatabaseReference generalHolidaysRef;
    private String currentUsername;
    private boolean isAdmin;
    private Button addGeneralHolidayButton;
    private String role;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_holidays, container, false);

        recyclerView = view.findViewById(R.id.holidays_recycler_view);
        emptyText = view.findViewById(R.id.empty_holidays_text);
        Button addHolidayButton = view.findViewById(R.id.add_holiday_button);
        addGeneralHolidayButton = view.findViewById(R.id.add_general_holiday_button);

        holidaysList = new ArrayList<>();
        
        // Get current user's username and role
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUsername = snapshot.child("username").getValue(String.class);
                role = snapshot.child("role").getValue(String.class);
                isAdmin = "admin".equals(role);
                
                if (currentUsername != null) {
                    Log.d("HolidaysFragment", "User loaded - Username: " + currentUsername + ", Is Admin: " + isAdmin);
                    
                    // Initialize database references
                    databaseReference = FirebaseDatabase.getInstance().getReference("holidays").child(currentUsername);
                    generalHolidaysRef = FirebaseDatabase.getInstance().getReference("general_holidays");
                    
                    // Show/hide general holiday button based on admin status
                    addGeneralHolidayButton.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    
                    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    adapter = new HolidaysAdapter(holidaysList, databaseReference, isAdmin);
                    recyclerView.setAdapter(adapter);
                    
                    loadHolidays();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HolidaysFragment", "Failed to load user data: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up personal holiday button
        addHolidayButton.setOnClickListener(v -> openDatePicker(false));

        // Set up general holiday button
        addGeneralHolidayButton.setOnClickListener(v -> openDatePicker(true));

        return view;
    }

    private void openDatePicker(boolean isGeneral) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    Log.d("HolidaysFragment", "Selected date: " + selectedDate + " (General: " + isGeneral + ")");
                    
                    if (isGeneral) {
                        // Add to general holidays
                        generalHolidaysRef.child(selectedDate).setValue(true)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("HolidaysFragment", "General holiday added successfully to general_holidays");
                                Toast.makeText(getContext(), "General holiday added successfully", Toast.LENGTH_SHORT).show();
                                // Add to all hairdressers
                                addHolidayToAllHairdressers(selectedDate);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("HolidaysFragment", "Failed to add general holiday: " + e.getMessage());
                                Toast.makeText(getContext(), "Failed to add general holiday", Toast.LENGTH_SHORT).show();
                            });
                    } else {
                        // Add to personal holidays
                        databaseReference.child(selectedDate).setValue(true)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("HolidaysFragment", "Personal holiday added successfully to holidays/" + currentUsername);
                                Toast.makeText(getContext(), "Personal holiday added successfully", Toast.LENGTH_SHORT).show();
                                // Add to the list immediately
                                holidaysList.add(new HolidaysAdapter.HolidayItem(selectedDate, false));
                                adapter.notifyDataSetChanged();
                                updateUI();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("HolidaysFragment", "Failed to add personal holiday: " + e.getMessage());
                                Toast.makeText(getContext(), "Failed to add personal holiday", Toast.LENGTH_SHORT).show();
                            });
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void addHolidayToAllHairdressers(String date) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.orderByChild("role").equalTo("hair dresser").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String username = userSnap.child("username").getValue(String.class);
                    if (username != null) {
                        DatabaseReference hairdresserHolidaysRef = FirebaseDatabase.getInstance()
                            .getReference("holidays")
                            .child(username);
                        hairdresserHolidaysRef.child(date).setValue(true);
                    }
                }
                
                // Add to the list immediately for admin
                if (isAdmin) {
                    holidaysList.add(new HolidaysAdapter.HolidayItem(date, true));
                    adapter.notifyDataSetChanged();
                    updateUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HolidaysFragment", "Failed to add holiday to all hairdressers", error.toException());
            }
        });
    }

    private void loadHolidays() {
        Log.d("HolidaysFragment", "Loading holidays for user: " + currentUsername);
        
        // Load personal holidays
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                holidaysList.clear();
                // Add personal holidays
                for (DataSnapshot holidaySnap : snapshot.getChildren()) {
                    String holiday = holidaySnap.getKey();
                    if (holiday != null) {
                        Log.d("HolidaysFragment", "Adding personal holiday: " + holiday);
                        holidaysList.add(new HolidaysAdapter.HolidayItem(holiday, false));
                    }
                }
                
                // Add general holidays for both admin and hairdressers
                if (isAdmin || "hair dresser".equals(role)) {
                    generalHolidaysRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot holidaySnap : snapshot.getChildren()) {
                                String holiday = holidaySnap.getKey();
                                if (holiday != null) {
                                    Log.d("HolidaysFragment", "Adding general holiday: " + holiday);
                                    holidaysList.add(new HolidaysAdapter.HolidayItem(holiday, true));
                                }
                            }
                            adapter.notifyDataSetChanged();
                            updateUI();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("HolidaysFragment", "Failed to load general holidays", error.toException());
                        }
                    });
                } else {
                    adapter.notifyDataSetChanged();
                    updateUI();
                }
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
            Log.d("HolidaysFragment", "No holidays to display");
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            Log.d("HolidaysFragment", "Displaying " + holidaysList.size() + " holidays");
        }
    }
}

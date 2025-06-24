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

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
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
        // Set minimum date to tomorrow
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long minDate = calendar.getTimeInMillis();

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);

                    // Validate the selected date
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    if (selectedCalendar.before(today)) {
                        Toast.makeText(getContext(), "Cannot set holiday for past dates", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isGeneral) {
                        // Add to general holidays
                        generalHolidaysRef.child(selectedDate).setValue(true)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "General holiday added successfully",
                                            Toast.LENGTH_SHORT).show();
                                    // Add to all hairdressers
                                    addHolidayToAllHairdressers(selectedDate);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to add general holiday", Toast.LENGTH_SHORT)
                                            .show();
                                });
                    } else {
                        // Add to personal holidays
                        databaseReference.child(selectedDate).setValue(true)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Personal holiday added successfully",
                                            Toast.LENGTH_SHORT).show();
                                    // Add to the list immediately
                                    holidaysList.add(new HolidaysAdapter.HolidayItem(selectedDate, false));
                                    adapter.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to add personal holiday", Toast.LENGTH_SHORT)
                                            .show();
                                });
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        // Set minimum date to prevent selecting past dates
        datePickerDialog.getDatePicker().setMinDate(minDate);
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
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to add holiday to all hairdressers", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadHolidays() {

        // Clear the list first
        holidaysList.clear();

        // Get current date for comparison
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // Load both personal and general holidays
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Add personal holidays
                for (DataSnapshot holidaySnap : snapshot.getChildren()) {
                    String holiday = holidaySnap.getKey();
                    if (holiday != null) {
                        // Parse the holiday date
                        try {
                            String[] dateParts = holiday.split("-");
                            Calendar holidayDate = Calendar.getInstance();
                            holidayDate.set(
                                    Integer.parseInt(dateParts[0]), // year
                                    Integer.parseInt(dateParts[1]) - 1, // month (0-based)
                                    Integer.parseInt(dateParts[2]) // day
                            );
                            holidayDate.set(Calendar.HOUR_OF_DAY, 0);
                            holidayDate.set(Calendar.MINUTE, 0);
                            holidayDate.set(Calendar.SECOND, 0);
                            holidayDate.set(Calendar.MILLISECOND, 0);

                            // Only add if the holiday is today or in the future
                            if (!holidayDate.before(today)) {
                                holidaysList.add(new HolidaysAdapter.HolidayItem(holiday, false));
                            }
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Error processing holiday date", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                // If admin or hairdresser, load general holidays
                if (isAdmin || "hair dresser".equals(role)) {
                    generalHolidaysRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot holidaySnap : snapshot.getChildren()) {
                                String holiday = holidaySnap.getKey();
                                if (holiday != null) {
                                    // Parse the holiday date
                                    try {
                                        String[] dateParts = holiday.split("-");
                                        Calendar holidayDate = Calendar.getInstance();
                                        holidayDate.set(
                                                Integer.parseInt(dateParts[0]), // year
                                                Integer.parseInt(dateParts[1]) - 1, // month (0-based)
                                                Integer.parseInt(dateParts[2]) // day
                                        );
                                        holidayDate.set(Calendar.HOUR_OF_DAY, 0);
                                        holidayDate.set(Calendar.MINUTE, 0);
                                        holidayDate.set(Calendar.SECOND, 0);
                                        holidayDate.set(Calendar.MILLISECOND, 0);

                                        // Only add if the holiday is today or in the future
                                        if (!holidayDate.before(today)) {
                                            holidaysList.add(new HolidaysAdapter.HolidayItem(holiday, true));
                                        }
                                    } catch (Exception e) {
                                        Toast.makeText(getContext(), "Error processing holiday date",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                            updateRecyclerView();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(getContext(), "Failed to load general holidays", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    updateRecyclerView();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load holidays", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRecyclerView() {
        if (!isAdded())
            return; // Check if fragment is still attached
        RecyclerView recyclerView = requireView().findViewById(R.id.holidays_recycler_view);
        adapter = createHolidaysAdapter();
        recyclerView.setAdapter(adapter);

        if (holidaysList.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private HolidaysAdapter createHolidaysAdapter() {
        return new HolidaysAdapter(holidaysList, databaseReference, isAdmin);
    }
}

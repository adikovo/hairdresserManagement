package com.example.barberbookingapp.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.barberbookingapp.MainActivity;
import com.example.barberbookingapp.R;
import com.example.barberbookingapp.models.Appointments;
import com.example.barberbookingapp.models.Worker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// HomeFragment: Allows clients to book appointments at the hair salon.
// - Displays a welcome message with the client's name.
// - Shows the latest announcement from the salon.
// - Lets the user select a service, hair dresser, date, and time.
// - Checks for hair dresser holidays and existing appointments to prevent double booking.
// - Provides buttons to book an appointment and view all of the client's appointments.

public class HomeFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Spinner serviceSpinner;
    private Spinner hairdresserSpinner;
    private TextView selectedDateTimeText;
    private MainActivity mainActivity;
    private List<Worker> availableHairdressers;
    private List<String> selectedHairdresserHolidays;
    private List<String> selectedHairdresserAppointments;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        mainActivity = (MainActivity) getActivity();

        // Initialize spinners
        serviceSpinner = view.findViewById(R.id.service_spinner);
        hairdresserSpinner = view.findViewById(R.id.hairdresser_spinner);
        selectedDateTimeText = view.findViewById(R.id.selected_datetime_text);

        // Display the fragment title
        // Shows the name of the connected client along with the Welcome back message
        TextView welcomeText = view.findViewById(R.id.welcome_text);
        if (mainActivity != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            mainActivity.fetchUsername(userId).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String username = task.getResult();
                    welcomeText.setText("Welcome back, " + username);
                } else {
                    welcomeText.setText("WELCOME Guest");
                }
            });
        }

        // Get reference to the announcements board path
        // If there is a message, display the updated message
        // If there isn't, don't display the board
        TextView announcementText = view.findViewById(R.id.announcement_text);
        DatabaseReference announcementsRef = FirebaseDatabase.getInstance().getReference("announcements")
                .child("current_announcement");

        announcementsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String announcement = snapshot.getValue(String.class);
                if (announcement != null && !announcement.isEmpty()) {
                    announcementText.setText(announcement);
                    announcementText.setVisibility(View.VISIBLE);
                } else {
                    announcementText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        // Set up service spinner with the salon services
        ArrayAdapter<CharSequence> serviceAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.hairdresser_services,
                android.R.layout.simple_spinner_item);
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serviceSpinner.setAdapter(serviceAdapter);

        // Set up time spinner with available time slots
        Spinner timeSpinner = view.findViewById(R.id.time_spinner);
        ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.default_times,
                android.R.layout.simple_spinner_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(timeAdapter);

        // Initialize available hairdressers list
        availableHairdressers = new ArrayList<>();

        // Load all hairdressers first
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    List<String> hairdresserNames = new ArrayList<>();
                    hairdresserNames.add("No hairdresser selected");
                    availableHairdressers.clear();

                    if (!snapshot.exists()) {
                        return;
                    }

                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                        String username = userSnap.child("username").getValue(String.class);
                        String email = userSnap.child("email").getValue(String.class);
                        String phone = userSnap.child("phone").getValue(String.class);
                        String role = userSnap.child("role").getValue(String.class);

                        // Include both hairdressers and admin users
                        if (username != null && email != null && phone != null && role != null &&
                                (role.equals("hair dresser") || role.equals("admin"))) {
                            Worker worker = new Worker(username, email, phone, role, new ArrayList<>());
                            worker.setId(userSnap.getKey());
                            availableHairdressers.add(worker);
                            hairdresserNames.add(username);
                        }
                    }

                    // Update hairdresser spinner
                    ArrayAdapter<String> hairdresserAdapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            hairdresserNames);
                    hairdresserAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    hairdresserSpinner.setAdapter(hairdresserAdapter);

                    // Add listener for hairdresser selection
                    hairdresserSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position > 0) { // Skip "No hairdresser selected"
                                String selectedHairdresser = parent.getItemAtPosition(position).toString();
                                fetchHairdresserAvailability(selectedHairdresser);
                            } else {
                                selectedHairdresserHolidays = new ArrayList<>();
                                selectedHairdresserAppointments = new ArrayList<>();
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Error loading hairdressers", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load hairdressers", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up date selection
        Button selectDateButton = view.findViewById(R.id.select_date_button);
        selectDateButton.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        String selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        selectedDateTimeText.setText(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));

            // Set minimum date to today
            datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());

            // Disable holiday dates in the date picker
            if (selectedHairdresserHolidays != null && !selectedHairdresserHolidays.isEmpty()) {
                datePickerDialog.getDatePicker().setOnDateChangedListener((view1, year, month, dayOfMonth) -> {
                    String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    if (selectedHairdresserHolidays.contains(date)) {
                        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setEnabled(false);
                        Toast.makeText(requireContext(), "This date is a holiday for the selected hairdresser",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setEnabled(true);
                    }
                });
            }

            datePickerDialog.show();
        });

        // Set up time selection
        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0)
                    return; // Skip if no time selected

                String selectedTime = parent.getItemAtPosition(position).toString();
                String selectedDate = selectedDateTimeText.getText().toString();
                String selectedHairdresser = hairdresserSpinner.getSelectedItem().toString();

                if (!selectedDate.equals("No date selected")
                        && !selectedHairdresser.equals("No hairdresser selected")) {
                    String dateTime = selectedDate + " " + selectedTime;

                    // Check if the selected time is in the past
                    try {
                        // Get current date and time
                        Calendar currentTime = Calendar.getInstance();

                        // Parse the selected date and time
                        String[] dateParts = selectedDate.split("-");
                        String[] timeParts = selectedTime.split(":");

                        // Create calendar object for selected date and time
                        Calendar selectedDateTime = Calendar.getInstance();
                        selectedDateTime.set(
                                Integer.parseInt(dateParts[0]), // year
                                Integer.parseInt(dateParts[1]) - 1, // month (0-based)
                                Integer.parseInt(dateParts[2]), // day
                                Integer.parseInt(timeParts[0]), // hour
                                Integer.parseInt(timeParts[1]) // minute
                        );

                        // If selected time is in the past, show error
                        if (selectedDateTime.before(currentTime)) {
                            Toast.makeText(requireContext(), "Cannot book appointments in the past", Toast.LENGTH_SHORT)
                                    .show();
                            timeSpinner.setSelection(0); // Reset to default selection
                            return;
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error checking appointment time", Toast.LENGTH_SHORT).show();
                    }

                    if (selectedHairdresserAppointments != null
                            && selectedHairdresserAppointments.contains(dateTime)) {
                        Toast.makeText(requireContext(), "Selected hairdresser is not available at this time",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Set up book appointment button
        Button bookAppointmentButton = view.findViewById(R.id.book_appointment_button);
        bookAppointmentButton.setOnClickListener(v -> {
            String selectedService = serviceSpinner.getSelectedItem().toString();
            String selectedHairdresser = hairdresserSpinner.getSelectedItem().toString();
            String selectedTime = timeSpinner.getSelectedItem().toString();
            String selectedDate = selectedDateTimeText.getText().toString();

            if (selectedService != null && !selectedDate.equals("No date selected")
                    && !selectedHairdresser.equals("No hairdresser selected")) {
                String dateTime = selectedDate + " " + selectedTime;

                // Final validation before booking
                Calendar now = Calendar.getInstance();
                Calendar selectedDateTime = Calendar.getInstance();

                try {
                    // Parse the selected date and time
                    String[] dateParts = selectedDate.split("-");
                    String[] timeParts = selectedTime.split(":");

                    selectedDateTime.set(
                            Integer.parseInt(dateParts[0]), // year
                            Integer.parseInt(dateParts[1]) - 1, // month (0-based)
                            Integer.parseInt(dateParts[2]), // day
                            Integer.parseInt(timeParts[0]), // hour
                            Integer.parseInt(timeParts[1]) // minute
                    );

                    // If selected time is in the past, show error
                    if (selectedDateTime.before(now)) {
                        Toast.makeText(getContext(), "Cannot book appointments in the past", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (mainActivity != null) {
                        mainActivity.bookAppointment(selectedService, dateTime, selectedHairdresser);
                        // Reset input fields after booking
                        resetInputFields();
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Invalid date or time format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Please select all required fields.", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up view appointments button
        Button viewAppointmentsButton = view.findViewById(R.id.view_appointments_button);
        viewAppointmentsButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_appointmentsFragment);
        });

        return view;
    }

    private void fetchHairdresserAvailability(String hairdresserUsername) {
        selectedHairdresserHolidays = new ArrayList<>();
        selectedHairdresserAppointments = new ArrayList<>();

        // Find the selected worker
        Worker selectedWorker = null;
        for (Worker worker : availableHairdressers) {
            if (worker.getUsername().equals(hairdresserUsername)) {
                selectedWorker = worker;
                break;
            }
        }

        if (selectedWorker == null) {
            Toast.makeText(getContext(), "Selected hairdresser not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch holidays for the specific hairdresser
        DatabaseReference holidaysRef = FirebaseDatabase.getInstance().getReference("holidays")
                .child(hairdresserUsername);
        holidaysRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                selectedHairdresserHolidays.clear();
                for (DataSnapshot holidaySnap : snapshot.getChildren()) {
                    String holiday = holidaySnap.getKey();
                    if (holiday != null) {
                        selectedHairdresserHolidays.add(holiday);
                    }
                }

                // After loading personal holidays, load general holidays
                DatabaseReference generalHolidaysRef = FirebaseDatabase.getInstance().getReference("general_holidays");
                generalHolidaysRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot holidaySnap : snapshot.getChildren()) {
                            String holiday = holidaySnap.getKey();
                            if (holiday != null) {
                                selectedHairdresserHolidays.add(holiday);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to load general holidays", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load holidays", Toast.LENGTH_SHORT).show();
            }
        });

        // Fetch appointments
        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");
        appointmentsRef.orderByChild("hairdresserUsername").equalTo(hairdresserUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        selectedHairdresserAppointments.clear();
                        for (DataSnapshot appointmentSnap : snapshot.getChildren()) {
                            Appointments appointment = appointmentSnap.getValue(Appointments.class);
                            if (appointment != null && appointment.getDateTime() != null) {
                                selectedHairdresserAppointments.add(appointment.getDateTime());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to load appointments", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resetInputFields() {
        serviceSpinner.setSelection(0);
        hairdresserSpinner.setSelection(0);
        selectedDateTimeText.setText("No date selected");
        Spinner timeSpinner = requireView().findViewById(R.id.time_spinner);
        timeSpinner.setSelection(0);
    }
}
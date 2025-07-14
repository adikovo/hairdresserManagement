package com.example.hairsalonbookingapp.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hairsalonbookingapp.MainActivity;
import com.example.hairsalonbookingapp.R;
import com.example.hairsalonbookingapp.models.Appointments;
import com.example.hairsalonbookingapp.models.Worker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// HomeFragment: Allows clients to book appointments at the hair salon.
// - Displays a welcome message with the client's name.
// - Shows the latest announcement from the salon.
// - Lets the user select a service, hair dresser, date, and time.
// - Checks for hair dresser holidays and existing appointments to prevent double booking.
// - Provides buttons to book an appointment and view all of the client's appointments.

public class HomeFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private Spinner serviceSpinner;
    private Spinner hairdresserSpinner;
    private TextView selectedDateTimeText;
    private MainActivity mainActivity;
    private List<Worker> availableHairdressers;
    private List<String> selectedHairdresserHolidays;
    private List<String> selectedHairdresserAppointments;
    private Spinner timeSpinner;
    private List<Appointments> selectedHairdresserAppointmentObjects;
    private int openingHour = 8;
    private int openingMinute = 0;
    private int closingHour = 20;
    private int closingMinute = 0;

    public HomeFragment() {

    }

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

        fetchOpeningHours();

        // Initialize spinners
        serviceSpinner = view.findViewById(R.id.service_spinner);
        hairdresserSpinner = view.findViewById(R.id.hairdresser_spinner);
        selectedDateTimeText = view.findViewById(R.id.selected_datetime_text);

        // Display welcome message with client name
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

        // Load and display salon announcements
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

        // Add listener for service selection to update available time slots
        serviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String selectedService = parent.getItemAtPosition(position).toString();
                    updateAvailableTimeSlots(selectedService);
                } else {
                    // Reset to default time slots
                    ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(
                            requireContext(),
                            R.array.default_times,
                            android.R.layout.simple_spinner_item);
                    timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    timeSpinner.setAdapter(timeAdapter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Set up time spinner with available time slots
        timeSpinner = view.findViewById(R.id.time_spinner);
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>());
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(timeAdapter);

        availableHairdressers = new ArrayList<>();

        // Load available hairdressers
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
                            if (position > 0) {
                                String selectedHairdresser = parent.getItemAtPosition(position).toString();
                                fetchHairdresserAvailability(selectedHairdresser);

                                // If a service is alreapddy selected, update time slots
                                if (serviceSpinner.getSelectedItemPosition() > 0) {
                                    String selectedService = serviceSpinner.getSelectedItem().toString();
                                    updateAvailableTimeSlots(selectedService);
                                }
                            } else {
                                selectedHairdresserHolidays = new ArrayList<>();
                                selectedHairdresserAppointments = new ArrayList<>();
                                selectedHairdresserAppointmentObjects = new ArrayList<>();
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

                        // If both service and hairdresser are selected, update time slots
                        if (serviceSpinner.getSelectedItemPosition() > 0
                                && hairdresserSpinner.getSelectedItemPosition() > 0) {
                            String selectedService = serviceSpinner.getSelectedItem().toString();
                            updateAvailableTimeSlots(selectedService);
                        }
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
                    return;

                String selectedTime = parent.getItemAtPosition(position).toString();
                String selectedDate = selectedDateTimeText.getText().toString();
                String selectedHairdresser = hairdresserSpinner.getSelectedItem().toString();

                if (!selectedDate.equals("No date selected")
                        && !selectedHairdresser.equals("No hairdresser selected")) {
                    String dateTime = selectedDate + " " + selectedTime;

                    // Check if the selected time is in the past
                    try {
                        Calendar currentTime = Calendar.getInstance();

                        String[] dateParts = selectedDate.split("-");
                        String[] timeParts = selectedTime.split(":");

                        // Create calendar object for selected date and time
                        Calendar selectedDateTime = Calendar.getInstance();
                        selectedDateTime.set(
                                Integer.parseInt(dateParts[0]),
                                Integer.parseInt(dateParts[1]) - 1,
                                Integer.parseInt(dateParts[2]),
                                Integer.parseInt(timeParts[0]),
                                Integer.parseInt(timeParts[1]));

                        // If selected time is in the past, show error
                        if (selectedDateTime.before(currentTime)) {
                            Toast.makeText(requireContext(), "Cannot book appointments in the past", Toast.LENGTH_SHORT)
                                    .show();
                            timeSpinner.setSelection(0);
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

            if (selectedService != null && !selectedService.equals("No service selected")
                    && !selectedDate.equals("No date selected")
                    && !selectedHairdresser.equals("No hairdresser selected")
                    && !selectedTime.equals("No time selected")) {
                String dateTime = selectedDate + " " + selectedTime;

                Calendar now = Calendar.getInstance();
                Calendar selectedDateTime = Calendar.getInstance();

                try {
                    // Parse the selected date and time
                    String[] dateParts = selectedDate.split("-");
                    String[] timeParts = selectedTime.split(":");

                    selectedDateTime.set(
                            Integer.parseInt(dateParts[0]),
                            Integer.parseInt(dateParts[1]) - 1,
                            Integer.parseInt(dateParts[2]),
                            Integer.parseInt(timeParts[0]),
                            Integer.parseInt(timeParts[1]));

                    if (selectedDateTime.before(now)) {
                        Toast.makeText(getContext(), "Cannot book appointments in the past", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (mainActivity != null) {
                        mainActivity.bookAppointment(selectedService, dateTime, selectedHairdresser);
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

    /**
     * Fetches salon opening hours from Firebase and stores them for time slot
     * calculations
     */
    private void fetchOpeningHours() {
        DatabaseReference hoursRef = FirebaseDatabase.getInstance().getReference("opening_hours");
        hoursRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer openHr = snapshot.child("opening_hour").getValue(Integer.class);
                Integer openMin = snapshot.child("opening_minute").getValue(Integer.class);
                Integer closeHr = snapshot.child("closing_hour").getValue(Integer.class);
                Integer closeMin = snapshot.child("closing_minute").getValue(Integer.class);
                openingHour = openHr != null ? openHr : 8;
                openingMinute = openMin != null ? openMin : 0;
                closingHour = closeHr != null ? closeHr : 20;
                closingMinute = closeMin != null ? closeMin : 0;

                updateTimeSpinnerWithOpeningHours();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateTimeSpinnerWithOpeningHours();
            }
        });
    }

    private void updateTimeSpinnerWithOpeningHours() {
        List<String> timeSlots = new ArrayList<>();
        timeSlots.add("No time selected");

        // Generate time slots based on opening/closing hours
        int startMinutes = openingHour * 60 + openingMinute;
        int endMinutes = closingHour * 60 + closingMinute;
        for (int minutes = startMinutes; minutes < endMinutes; minutes += 30) {
            int hour = minutes / 60;
            int min = minutes % 60;
            String timeSlot = String.format("%02d:%02d", hour, min);
            timeSlots.add(timeSlot);
        }

        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                timeSlots);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(timeAdapter);
    }

    private void fetchHairdresserAvailability(String hairdresserUsername) {
        selectedHairdresserHolidays = new ArrayList<>();
        selectedHairdresserAppointments = new ArrayList<>();
        selectedHairdresserAppointmentObjects = new ArrayList<>();

        // Clean up outdated appointments and holidays before fetching current ones
        if (mainActivity != null) {
            mainActivity.cleanupOutdatedAppointments();
            mainActivity.cleanupOutdatedHolidays();
        }

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
                        selectedHairdresserAppointmentObjects.clear();
                        for (DataSnapshot appointmentSnap : snapshot.getChildren()) {
                            Appointments appointment = appointmentSnap.getValue(Appointments.class);
                            if (appointment != null && appointment.getDateTime() != null) {
                                String status = appointment.getStatus();
                                if (status == null || "scheduled".equals(status)) {
                                    selectedHairdresserAppointments.add(appointment.getDateTime());
                                    selectedHairdresserAppointmentObjects.add(appointment);
                                }
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
        timeSpinner.setSelection(0);
        selectedHairdresserAppointmentObjects = new ArrayList<>();
    }

    // method to get duration for a service type
    private String getServiceDuration(String serviceType) {
        if (serviceType == null)
            return "30";

        if (serviceType.contains("Classic Mens Cut"))
            return "30";
        if (serviceType.contains("Premium Womens Cut"))
            return "60";
        if (serviceType.contains("Kids Cut"))
            return "30";
        if (serviceType.contains("Classic Shave"))
            return "15";
        if (serviceType.contains("Full Color"))
            return "90";
        if (serviceType.contains("Root Touch-up Color"))
            return "60";
        if (serviceType.contains("Full Highlight Service"))
            return "120";
        if (serviceType.contains("Quick Blow Dry"))
            return "30";
        if (serviceType.contains("Styling Blow Dry"))
            return "45";
        if (serviceType.contains("Beard Trim and Shape"))
            return "20";
        if (serviceType.contains("Keratin Straightening"))
            return "180";
        if (serviceType.contains("Japanese Straightening"))
            return "240";
        if (serviceType.contains("Deep Conditioning Treatment"))
            return "45";
        if (serviceType.contains("Hydrating Hair Mask"))
            return "30";
        if (serviceType.contains("Event Styling"))
            return "60";

        return "30"; // Default duration
    }

    // update available time slots based on selected service duration and opening
    // hours
    private void updateAvailableTimeSlots(String selectedService) {
        String durationStr = getServiceDuration(selectedService);
        int durationMinutes = Integer.parseInt(durationStr);

        List<String> availableTimeSlots = new ArrayList<>();
        availableTimeSlots.add("No time selected");

        // generate time slots based on opening/closing hours
        int startMinutes = openingHour * 60 + openingMinute;
        int endMinutes = closingHour * 60 + closingMinute;
        for (int minutes = startMinutes; minutes + durationMinutes <= endMinutes; minutes += 30) {
            int hour = minutes / 60;
            int min = minutes % 60;
            String timeSlot = String.format("%02d:%02d", hour, min);
            if (isTimeSlotAvailable(timeSlot, durationMinutes)) {
                availableTimeSlots.add(timeSlot);
            }
        }

        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                availableTimeSlots);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(timeAdapter);
    }

    // check if a time slot can accommodate the service duration
    private boolean isTimeSlotAvailable(String timeSlot, int durationMinutes) {
        if (selectedHairdresserAppointments == null || selectedHairdresserAppointments.isEmpty()) {
            return true;
        }

        String selectedDate = selectedDateTimeText.getText().toString();
        if (selectedDate.equals("No date selected")) {
            return true;
        }

        String[] timeParts = timeSlot.split(":");
        int startHour = Integer.parseInt(timeParts[0]);
        int startMinute = Integer.parseInt(timeParts[1]);

        // Calculate end time
        int endHour = startHour + (durationMinutes / 60);
        int endMinute = startMinute + (durationMinutes % 60);
        if (endMinute >= 60) {
            endHour += 1;
            endMinute -= 60;
        }

        // check if the appointment would end after salon hours
        if (endHour > 20 || (endHour == 20 && endMinute > 0)) {
            return false;
        }

        // check for conflicts with existing appointments
        for (String existingAppointment : selectedHairdresserAppointments) {
            if (existingAppointment.startsWith(selectedDate)) {
                String existingTime = existingAppointment.substring(selectedDate.length() + 1);
                String existingDuration = getExistingAppointmentDuration(existingAppointment);
                int existingDurationMinutes = Integer.parseInt(existingDuration);

                String[] existingTimeParts = existingTime.split(":");
                int existingStartHour = Integer.parseInt(existingTimeParts[0]);
                int existingStartMinute = Integer.parseInt(existingTimeParts[1]);

                // calculate existing appointment end time
                int existingEndHour = existingStartHour + (existingDurationMinutes / 60);
                int existingEndMinute = existingStartMinute + (existingDurationMinutes % 60);
                if (existingEndMinute >= 60) {
                    existingEndHour += 1;
                    existingEndMinute -= 60;
                }

                if (hasTimeOverlap(startHour, startMinute, endHour, endMinute,
                        existingStartHour, existingStartMinute, existingEndHour, existingEndMinute)) {
                    return false;
                }
            }
        }

        return true;
    }

    // method to check if two time ranges overlap
    private boolean hasTimeOverlap(int start1Hour, int start1Minute, int end1Hour, int end1Minute,
            int start2Hour, int start2Minute, int end2Hour, int end2Minute) {
        int start1 = start1Hour * 60 + start1Minute;
        int end1 = end1Hour * 60 + end1Minute;
        int start2 = start2Hour * 60 + start2Minute;
        int end2 = end2Hour * 60 + end2Minute;

        return start1 < end2 && start2 < end1;
    }

    // method to get duration of existing appointment
    private String getExistingAppointmentDuration(String appointmentDateTime) {
        if (selectedHairdresserAppointmentObjects != null) {
            for (Appointments appointment : selectedHairdresserAppointmentObjects) {
                if (appointment.getDateTime() != null && appointment.getDateTime().equals(appointmentDateTime)) {
                    return appointment.getDuration() != null ? appointment.getDuration() : "30";
                }
            }
        }
        return "30";
    }
}
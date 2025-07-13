package com.example.hairsalonbookingapp.fragments;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hairsalonbookingapp.R;
import com.example.hairsalonbookingapp.adapters.AdminAppointmentsAdapter;
import com.example.hairsalonbookingapp.models.Appointments;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// Admin fragment for managing the hair salon system
// Allows admin to view and manage appointments, users, and holidays

public class AdminFragment extends Fragment {

    private RecyclerView adminAppointmentsRecyclerView;
    private AdminAppointmentsAdapter adminAppointmentsAdapter;
    private List<Appointments> appointmentsList;
    private List<Appointments> allAppointmentsList; // Store all appointments for filtering
    private Button workersButton;
    private Button postAnnouncementButton;
    private Button manageHoursButton;
    private Spinner hairdresserFilterSpinner;
    private List<String> hairdresserNames;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin, container, false);

        adminAppointmentsRecyclerView = view.findViewById(R.id.admin_appointments_recycler_view);
        appointmentsList = new ArrayList<>();
        allAppointmentsList = new ArrayList<>();
        hairdresserNames = new ArrayList<>();
        adminAppointmentsAdapter = new AdminAppointmentsAdapter(requireContext(), appointmentsList);

        adminAppointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adminAppointmentsRecyclerView.setAdapter(adminAppointmentsAdapter);

        // Find the buttons
        workersButton = view.findViewById(R.id.workers_button);
        postAnnouncementButton = view.findViewById(R.id.post_announcement_button);
        manageHoursButton = view.findViewById(R.id.manage_hours_button);
        hairdresserFilterSpinner = view.findViewById(R.id.hairdresser_filter_spinner);

        // Check user role and set button visibility
        checkUserRole();

        // Navigate to workers fragment on click
        workersButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_adminFragment_to_workersFragment);
        });

        // When clicking the salon holidays button, the hairdresser will navigate to the relevant fragment
        Button HolidaysFragmentButton = view.findViewById(R.id.manage_holidays_button);
        HolidaysFragmentButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_adminFragment_to_holidaysFragment);
        });

        // When clicking the edit announcement button, show the new DialogFragment
        postAnnouncementButton.setOnClickListener(v -> {
            new EditAnnouncementDialog().show(getParentFragmentManager(), "EditAnnouncementDialog");
        });

        // Show OpeningHoursDialog
        manageHoursButton.setOnClickListener(v -> {
            new OpeningHoursDialog().show(getParentFragmentManager(), "OpeningHoursDialog");
        });
        setupHairdresserFilter();
        loadHairdresserAppointments();

        return view;
    }

    private void checkUserRole() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        userRef.child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.getValue(String.class);
                boolean isAdmin = "admin".equals(role);
                workersButton.setVisibility(isAdmin ? VISIBLE : INVISIBLE);
                postAnnouncementButton.setVisibility(isAdmin ? VISIBLE : INVISIBLE);
                manageHoursButton.setVisibility(isAdmin ? VISIBLE : INVISIBLE);
                hairdresserFilterSpinner.setVisibility(isAdmin ? VISIBLE : INVISIBLE);

                if (isAdmin) {
                    loadAllAppointments();
                } else {
                    loadHairdresserAppointments();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                workersButton.setVisibility(INVISIBLE);
                postAnnouncementButton.setVisibility(INVISIBLE);
                manageHoursButton.setVisibility(INVISIBLE);
                hairdresserFilterSpinner.setVisibility(INVISIBLE);
                Toast.makeText(requireContext(), "Failed to check user role", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupHairdresserFilter() {
        // Load all hairdresser names for the filter
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hairdresserNames.clear();
                hairdresserNames.add("All Hairdressers"); // Default option

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String username = userSnap.child("username").getValue(String.class);
                    String role = userSnap.child("role").getValue(String.class);

                    if (username != null && role != null &&
                            (role.equals("hair dresser") || role.equals("admin"))) {
                        hairdresserNames.add(username);
                    }
                }

                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        hairdresserNames);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                hairdresserFilterSpinner.setAdapter(spinnerAdapter);


                hairdresserFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position == 0) {
                            // "All Hairdressers" selected - show all appointments
                            filterAppointmentsByHairdresser(null);
                        } else {
                            // Specific hairdresser selected
                            String selectedHairdresser = hairdresserNames.get(position);
                            filterAppointmentsByHairdresser(selectedHairdresser);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        filterAppointmentsByHairdresser(null);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load hairdressers", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterAppointmentsByHairdresser(String hairdresserName) {
        appointmentsList.clear();

        if (hairdresserName == null) {
            // Show all appointments
            appointmentsList.addAll(allAppointmentsList);
        } else {
            // Filter by specific hairdresser
            for (Appointments appointment : allAppointmentsList) {
                if (hairdresserName.equals(appointment.getHairdresserUsername())) {
                    appointmentsList.add(appointment);
                }
            }
        }

        adminAppointmentsAdapter.notifyDataSetChanged();
    }

    // Load all client appointments from Firebase
    private void loadAllAppointments() {
        // Create path to client appointments
        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");
        appointmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                appointmentsList.clear();
                allAppointmentsList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Appointments appointment = child.getValue(Appointments.class);
                    if (appointment != null) {
                        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference("users")
                                .child(currentUserId);

                        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                String currentUsername = userSnapshot.child("username").getValue(String.class);
                                String appointmentHairdresser = appointment.getHairdresserUsername();

                                boolean isPersonalAppointment = currentUsername != null &&
                                        appointmentHairdresser != null &&
                                        currentUsername.equals(appointmentHairdresser);

                                appointment.setPersonalAppointment(isPersonalAppointment);

                                if (!isPersonalAppointment && appointmentHairdresser != null) {
                                    appointment.setHairdresserUsername(appointmentHairdresser);
                                }

                                if (isAppoitmentDateValid(appointment.getDateTime())) {
                                    allAppointmentsList.add(appointment);
                                    appointmentsList.add(appointment);
                                    adminAppointmentsAdapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load appointments.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isAppoitmentDateValid(String dateToCheck) {
        String[] dateTimeParts = dateToCheck.split(" ");
        String[] dateParts = dateTimeParts[0].split("-");
        String[] timeParts = dateTimeParts[1].split(":");

        Calendar appointmentDateTime = Calendar.getInstance();
        appointmentDateTime.set(
                Integer.parseInt(dateParts[0]), // year
                Integer.parseInt(dateParts[1]) - 1, // month (0-based)
                Integer.parseInt(dateParts[2]), // day
                Integer.parseInt(timeParts[0]), // hour
                Integer.parseInt(timeParts[1]) // minute
        );

        Calendar currentDateTime = Calendar.getInstance();

        return !appointmentDateTime.before(currentDateTime);
    }

    // Load appointments for the currently logged-in hairdresser
    private void loadHairdresserAppointments() {
        // Get current user's username
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentUsername = snapshot.child("username").getValue(String.class);
                if (currentUsername != null) {
                    // Create path to client appointments and filter by hairdresser
                    DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");
                    appointmentsRef.orderByChild("hairdresserUsername").equalTo(currentUsername)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    appointmentsList.clear();
                                    for (DataSnapshot child : snapshot.getChildren()) {
                                        Appointments appointment = child.getValue(Appointments.class);

                                        if (appointment != null && isAppoitmentDateValid(appointment.getDateTime())) {
                                            appointmentsList.add(appointment);
                                        }
                                    }
                                    adminAppointmentsAdapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(requireContext(), "Failed to load appointments.", Toast.LENGTH_SHORT)
                                            .show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

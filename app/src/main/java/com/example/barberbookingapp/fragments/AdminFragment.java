package com.example.barberbookingapp.fragments;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barberbookingapp.R;
import com.example.barberbookingapp.adapters.AdminAppointmentsAdapter;
import com.example.barberbookingapp.models.Appointments;
import com.example.barberbookingapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

// Admin fragment for managing the hair salon system
// Allows admin to view and manage appointments, users, and holidays

public class AdminFragment extends Fragment {

    private RecyclerView adminAppointmentsRecyclerView;
    private AdminAppointmentsAdapter adminAppointmentsAdapter;
    private List<Appointments> appointmentsList;
    private Button workersButton;
    private Button postAnnouncementButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin, container, false);

        adminAppointmentsRecyclerView = view.findViewById(R.id.admin_appointments_recycler_view);
        appointmentsList = new ArrayList<>();
        adminAppointmentsAdapter = new AdminAppointmentsAdapter(requireContext(), appointmentsList);

        adminAppointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adminAppointmentsRecyclerView.setAdapter(adminAppointmentsAdapter);

        // Find the workers button
        workersButton = view.findViewById(R.id.workers_button);
        postAnnouncementButton = view.findViewById(R.id.post_announcement_button);

        // Check user role and set button visibility
        checkUserRole();

        // Navigate to workers fragment on click
        workersButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_adminFragment_to_workersFragment);
        });

        // When clicking the salon holidays button, the hairdresser will navigate to the
        // relevant fragment
        Button HolidaysFragmentButton = view.findViewById(R.id.manage_holidays_button);
        HolidaysFragmentButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_adminFragment_to_holidaysFragment);
        });

        // When clicking the edit announcement button, a dialog will open with the
        // current announcement
        // This way the hair dresser can update it whenever they want
        // The announcement is stored in Firebase
        postAnnouncementButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Edit Announcement");

            // Create a custom View for the dialog
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_announcement, null);
            EditText announcementEditText = dialogView.findViewById(R.id.announcement_text_input);

            // Reference to the announcement - there is only one
            DatabaseReference announcementsRef = FirebaseDatabase.getInstance().getReference("announcements")
                    .child("current_announcement");

            // Read the current announcement and display it in the dialog
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

            builder.setView(dialogView);

            // Update announcement button
            builder.setPositiveButton("Update", (dialog, which) -> {
                String updatedText = announcementEditText.getText().toString().trim();
                if (!updatedText.isEmpty()) {
                    // Update the announcement in Firebase
                    // Save the new announcement in Firebase instead of the previous one (not in
                    // addition)
                    announcementsRef.setValue(updatedText)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(requireContext(), "Announcement updated", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(requireContext(), "Failed to update announcement",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid announcement", Toast.LENGTH_SHORT).show();
                }
            });

            // Cancel button (closes the dialog)
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

            builder.create().show();
        });

        // Load appointments for the current hairdresser
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

                // Load different appointments based on role
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
                Toast.makeText(requireContext(), "Failed to check user role", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Load all client appointments from Firebase
    private void loadAllAppointments() {
        // Create path to client appointments
        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");
        appointmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                appointmentsList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Appointments appointment = child.getValue(Appointments.class);
                    if (appointment != null) {
                        // Get current user's username to compare
                        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference("users")
                                .child(currentUserId);

                        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                String currentUsername = userSnapshot.child("username").getValue(String.class);
                                String appointmentHairdresser = appointment.getHairdresserUsername();

                                // Only compare if both values are not null
                                boolean isPersonalAppointment = currentUsername != null &&
                                        appointmentHairdresser != null &&
                                        currentUsername.equals(appointmentHairdresser);

                                appointment.setPersonalAppointment(isPersonalAppointment);

                                // If it's not a personal appointment, set the hairdresser username
                                if (!isPersonalAppointment && appointmentHairdresser != null) {
                                    appointment.setHairdresserUsername(appointmentHairdresser);
                                }

                                appointmentsList.add(appointment);
                                adminAppointmentsAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            // If reading the data failed, display an error message
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load appointments.", Toast.LENGTH_SHORT).show();
            }
        });
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
                                        if (appointment != null) {
                                            appointmentsList.add(appointment);
                                        }
                                    }
                                    // After loading the updated appointments, display them in RECVIEW
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

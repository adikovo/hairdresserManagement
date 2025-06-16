package com.example.barberbookingapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.barberbookingapp.R;
import com.example.barberbookingapp.adapters.AppointmentsAdapter;
import com.example.barberbookingapp.models.Appointments;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

// Fragment for displaying all appointments for the current user
// Allows users to view and cancel their appointments

public class AppointmentsFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public AppointmentsFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AppointmentsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AppointmentsFragment newInstance(String param1, String param2) {
        AppointmentsFragment fragment = new AppointmentsFragment();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appointments, container, false);

        //Get path to user in Firebase and display their appointments accordingly
        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        appointmentsRef.orderByChild("clientId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Appointments> appointments = new ArrayList<>();
                for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    Appointments appointment = appointmentSnapshot.getValue(Appointments.class);
                    if (appointment != null) {
                        appointments.add(appointment);
                    }
                }

                // After the list is ready, update the RECVIEW
                updateRecyclerView(appointments);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }


            private void updateRecyclerView(List<Appointments> appointmentsList) {
                RecyclerView recyclerView = requireView().findViewById(R.id.appointments_recycler_view);
                recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                AppointmentsAdapter adapter = createAppointmentsAdapter(appointmentsList);
                recyclerView.setAdapter(adapter);
            }

            private AppointmentsAdapter createAppointmentsAdapter(List<Appointments> appointmentsList) {
                return new AppointmentsAdapter(requireContext(), appointmentsList, appointment -> {
                    cancelAppointment(appointment, appointmentsList);
                });
            }

            //Function to cancel an appointment
            private void cancelAppointment(Appointments appointment, List<Appointments> appointmentsList) {
                //Change the format to reach the correct path of the appointment
                DatabaseReference appointmentRef = FirebaseDatabase.getInstance()
                        .getReference("appointments")
                        .child(appointment.getDateTime().replace(" ", "_").replace(":", "-"));

                //Delete from Firebase and the list (if deletion was successful)
                appointmentRef.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Remove the appointment from the list
                        appointmentsList.remove(appointment);

                        // Update the adapter
                        RecyclerView recyclerView = requireView().findViewById(R.id.appointments_recycler_view);
                        AppointmentsAdapter adapter = (AppointmentsAdapter) recyclerView.getAdapter();
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }

                        // Show success message to user
                        Toast.makeText(requireContext(), "The appointment has been canceled! remove it from the calendar.", Toast.LENGTH_SHORT).show();
                    } else {
                        // Handle failure case
                        Toast.makeText(requireContext(), "Failed to cancel appointment", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        return view;
    }
}
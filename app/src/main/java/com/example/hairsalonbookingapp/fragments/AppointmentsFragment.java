package com.example.hairsalonbookingapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.hairsalonbookingapp.MainActivity;
import com.example.hairsalonbookingapp.R;
import com.example.hairsalonbookingapp.adapters.AppointmentsAdapter;
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
import java.text.ParseException;

// Fragment for displaying all appointments for the current user
// Allows users to view and cancel their appointments

public class AppointmentsFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public AppointmentsFragment() {
    }

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

        RecyclerView recyclerView = view.findViewById(R.id.appointments_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        AppointmentsAdapter adapter = new AppointmentsAdapter(requireContext(), new ArrayList<>(), null);
        recyclerView.setAdapter(adapter);

        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        appointmentsRef.orderByChild("clientId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Appointments> appointments = new ArrayList<>();
                        for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                            Appointments appointment = appointmentSnapshot.getValue(Appointments.class);
                            if (appointment != null) {
                                try {
                                    String dateTime = appointment.getDateTime();
                                    String[] dateTimeParts = dateTime.split(" ");
                                    String[] dateParts = dateTimeParts[0].split("-");
                                    String[] timeParts = dateTimeParts[1].split(":");

                                    Calendar appointmentDate = Calendar.getInstance();
                                    appointmentDate.set(
                                            Integer.parseInt(dateParts[0]),
                                            Integer.parseInt(dateParts[1]) - 1,
                                            Integer.parseInt(dateParts[2]),
                                            Integer.parseInt(timeParts[0]),
                                            Integer.parseInt(timeParts[1]));

                                    if (!appointmentDate.before(today)) {
                                        appointments.add(appointment);
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(getContext(), "Error processing appointment date",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        updateRecyclerView(appointments);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), "Failed to load appointments: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        return view;
    }

    private void updateRecyclerView(List<Appointments> appointmentsList) {
        if (!isAdded())
            return;
        RecyclerView recyclerView = requireView().findViewById(R.id.appointments_recycler_view);
        AppointmentsAdapter adapter = createAppointmentsAdapter(appointmentsList);
        recyclerView.setAdapter(adapter);
    }

    private AppointmentsAdapter createAppointmentsAdapter(List<Appointments> appointmentsList) {
        return new AppointmentsAdapter(requireContext(), appointmentsList, appointment -> {
            cancelAppointment(appointment, appointmentsList);
        });
    }

    // Function to cancel an appointment
    private void cancelAppointment(Appointments appointment, List<Appointments> appointmentsList) {
        if (!isAdded())
            return;

        String appointmentKey = appointment.getDateTime().replace(" ", "_").replace(":", "-");

        MainActivity mainActivity = (MainActivity) requireActivity();
        mainActivity.markAppointmentAsCanceled(appointmentKey);

        appointmentsList.remove(appointment);
        RecyclerView recyclerView = requireView().findViewById(R.id.appointments_recycler_view);
        AppointmentsAdapter adapter = (AppointmentsAdapter) recyclerView.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        Toast.makeText(requireContext(),
                "Appointment deleted from calendar",
                Toast.LENGTH_SHORT).show();
    }
}
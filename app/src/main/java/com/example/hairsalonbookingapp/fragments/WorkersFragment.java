package com.example.hairsalonbookingapp.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.navigation.Navigation;
import com.example.hairsalonbookingapp.R;
import com.example.hairsalonbookingapp.adapters.WorkerAdapter;
import com.example.hairsalonbookingapp.models.Worker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

// This fragment manages the list of hair dressers (workers)
// Allows admin to view, add, and remove hair dressers
// Also manages holidays for each hair dresser
public class WorkersFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private RecyclerView workersRecyclerView;
    private WorkerAdapter workerAdapter;
    private List<Worker> workerList;
    private DatabaseReference usersRef;
    private DatabaseReference hairdressersRef;

    public WorkersFragment() {

    }

    public static WorkersFragment newInstance(String param1, String param2) {
        WorkersFragment fragment = new WorkersFragment();
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
        View view = inflater.inflate(R.layout.fragment_workers, container, false);

        workersRecyclerView = view.findViewById(R.id.workers_recycler_view);
        workersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        workerList = new ArrayList<>();
        workerAdapter = new WorkerAdapter(workerList, new WorkerAdapter.OnWorkerDeleteListener() {
            @Override
            public void onDelete(Worker worker, int position) {
                deleteWorker(worker, position);
            }
        });
        workersRecyclerView.setAdapter(workerAdapter);

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        hairdressersRef = FirebaseDatabase.getInstance().getReference("hairdressers");

        // Load and display workers list
        usersRef.orderByChild("role").equalTo("hair dresser").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                workerList.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String username = userSnap.child("username").getValue(String.class);
                    String email = userSnap.child("email").getValue(String.class);
                    String phone = userSnap.child("phone").getValue(String.class);
                    String role = userSnap.child("role").getValue(String.class);

                    if (username != null && email != null && phone != null && role != null) {
                        Worker worker = new Worker(username, email, phone, role, new ArrayList<>());
                        worker.setId(userSnap.getKey());
                        workerList.add(worker);
                    }
                }
                workerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch workers", Toast.LENGTH_SHORT).show();
            }
        });

        Button addHairdresserButton = view.findViewById(R.id.add_hairdresser_button);
        addHairdresserButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean("register_hairdresser", true);
            Navigation.findNavController(v).navigate(R.id.action_workersFragment_to_registerFragment, bundle);
        });

        return view;
    }

    // deletes a worker and all their associated data including appointments
    private void deleteWorker(Worker worker, int position) {
        // Remove from users table
        usersRef.orderByChild("email").equalTo(worker.getEmail())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(getContext(), "Worker not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            String userId = userSnap.getKey();

                            // Remove from users table
                            userSnap.getRef().removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        // Remove from hairdressers table
                                        hairdressersRef.orderByChild("email").equalTo(worker.getEmail())
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(
                                                            @NonNull DataSnapshot hairdresserSnapshot) {
                                                        if (hairdresserSnapshot.exists()) {
                                                            for (DataSnapshot hairdresserSnap : hairdresserSnapshot
                                                                    .getChildren()) {
                                                                hairdresserSnap.getRef().removeValue();
                                                            }
                                                        }

                                                        // Remove from holidays table
                                                        DatabaseReference holidaysRef = FirebaseDatabase.getInstance()
                                                                .getReference("holidays");
                                                        holidaysRef.child(worker.getUsername()).removeValue();

                                                        // Remove from appointments table
                                                        DatabaseReference appointmentsRef = FirebaseDatabase
                                                                .getInstance().getReference("appointments");
                                                        appointmentsRef.orderByChild("hairdresserUsername")
                                                                .equalTo(worker.getUsername())
                                                                .addListenerForSingleValueEvent(
                                                                        new ValueEventListener() {
                                                                            @Override
                                                                            public void onDataChange(
                                                                                    @NonNull DataSnapshot appointmentsSnapshot) {
                                                                                for (DataSnapshot appointmentSnap : appointmentsSnapshot
                                                                                        .getChildren()) {
                                                                                    appointmentSnap.getRef()
                                                                                            .removeValue();
                                                                                }

                                                                                Toast.makeText(getContext(),
                                                                                        "Worker deleted successfully",
                                                                                        Toast.LENGTH_SHORT).show();
                                                                            }

                                                                            @Override
                                                                            public void onCancelled(
                                                                                    @NonNull DatabaseError error) {
                                                                                Toast.makeText(getContext(),
                                                                                        "Failed to remove appointments",
                                                                                        Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        });
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                        Toast.makeText(getContext(),
                                                                "Failed to remove from hairdressers",
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Failed to remove worker", Toast.LENGTH_SHORT)
                                                .show();
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to fetch worker data", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
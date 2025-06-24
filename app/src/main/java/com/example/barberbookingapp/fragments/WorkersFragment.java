package com.example.barberbookingapp.fragments;

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
import com.example.barberbookingapp.R;
import com.example.barberbookingapp.adapters.WorkerAdapter;
import com.example.barberbookingapp.models.Worker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WorkersFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
// This fragment manages the list of hair dressers (workers)
// Allows admin to view, add, and remove hair dressers
// Also manages holidays for each hair dresser
public class WorkersFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
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
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment WorkersFragment.
     */
    // TODO: Rename and change types and number of parameters
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

        // Set up real-time listener for worker updates
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

    private void deleteWorker(Worker worker, int position) {
        // Remove from users
        usersRef.orderByChild("email").equalTo(worker.getEmail())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            return;
                        }

                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            userSnap.getRef().removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        // Remove from hairdressers
                                        hairdressersRef.orderByChild("email").equalTo(worker.getEmail())
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        if (!snapshot.exists()) {
                                                            return;
                                                        }

                                                        for (DataSnapshot hairdresserSnap : snapshot.getChildren()) {
                                                            hairdresserSnap.getRef().removeValue()
                                                                    .addOnSuccessListener(aVoid1 -> {
                                                                        workerList.remove(position);
                                                                        workerAdapter.notifyItemRemoved(position);
                                                                        Toast.makeText(getContext(), "Worker deleted",
                                                                                Toast.LENGTH_SHORT).show();
                                                                    });
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                    }
                                                });
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }
}
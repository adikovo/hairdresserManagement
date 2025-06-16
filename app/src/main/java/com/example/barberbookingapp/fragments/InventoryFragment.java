package com.example.barberbookingapp.fragments;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barberbookingapp.R;
import com.example.barberbookingapp.adapters.InventoryAdapter;
import com.example.barberbookingapp.models.InventoryItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;



// InventoryFragment: Allows admin to manage the hair salon's inventory.
// - Displays a list of inventory items with their quantities.
// - Provides buttons to increase, decrease, or delete each item.
// - Allows adding new inventory items.

public class InventoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<InventoryItem> itemList;
    private DatabaseReference inventoryRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory, container, false);

        recyclerView = view.findViewById(R.id.inventory_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        itemList = new ArrayList<>();
        adapter = new InventoryAdapter(itemList);
        recyclerView.setAdapter(adapter);

        inventoryRef = FirebaseDatabase.getInstance().getReference("inventory");

        Button addItemButton = view.findViewById(R.id.add_item_button);
        addItemButton.setOnClickListener(v -> showAddItemDialog());

        loadInventoryItems();

        return view;
    }

    //Open a new dialog to add the product name and its quantity
    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Item");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_item, null);
        EditText itemNameInput = dialogView.findViewById(R.id.item_name_input);
        EditText itemQuantityInput = dialogView.findViewById(R.id.item_quantity_input);

        builder.setView(dialogView);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = itemNameInput.getText().toString().trim();
            String quantityText = itemQuantityInput.getText().toString().trim();

            if (!name.isEmpty() && !quantityText.isEmpty()) {
                int quantity = Integer.parseInt(quantityText);
                InventoryItem newItem = new InventoryItem(name, quantity);

                //Create a key and add the new item to Firebase
                inventoryRef.push().setValue(newItem).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Item added successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to add item", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(requireContext(), "Please enter a name and quantity", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    //Load inventory from Firebase
    private void loadInventoryItems() {
        inventoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    InventoryItem item = itemSnapshot.getValue(InventoryItem.class);
                    if (item != null) {
                        item.setKey(itemSnapshot.getKey());
                        itemList.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load inventory", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

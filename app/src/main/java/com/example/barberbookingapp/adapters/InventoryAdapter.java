package com.example.barberbookingapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barberbookingapp.R;
import com.example.barberbookingapp.models.InventoryItem;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

/*
Adapter for displaying hair salon equipment inventory in RecView
* Each item includes the item name and its quantity, as well as:
Minus button - to decrease quantity by 1
Plus button - to increase quantity by 1
And a cancel button that allows deleting the item from the list
* */

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private final List<InventoryItem> itemList;

    public InventoryAdapter(List<InventoryItem> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        InventoryItem item = itemList.get(position);
        holder.itemName.setText(item.getName());
        holder.itemQuantity.setText(String.valueOf(item.getQuantity()));

        //Find the path to the current item
        DatabaseReference itemRef = FirebaseDatabase.getInstance().getReference("inventory").child(item.getKey());

        //When clicking the minus button, decrease quantity by 1 and update in list and Firebase
        holder.decreaseButton.setOnClickListener(v -> {
            if (item.getQuantity() > 0) {
                int newQuantity = item.getQuantity() - 1;
                item.setQuantity(newQuantity);
                itemRef.child("quantity").setValue(newQuantity);
            }
        });

        //When clicking the plus button, increase quantity by 1 and update in list and Firebase
        holder.increaseButton.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() + 1;
            item.setQuantity(newQuantity);
            itemRef.child("quantity").setValue(newQuantity);
        });

        //When clicking REMOVE, delete the item from Firebase and the list
        holder.deleteButton.setOnClickListener(v -> {
            itemRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                } else {
                    Toast.makeText(v.getContext(), "Failed to delete item", Toast.LENGTH_SHORT).show();
                }
            });
        });

    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    //Display the final fields for each item in the list
    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        TextView itemName;
        TextView itemQuantity;
        Button decreaseButton, increaseButton, deleteButton;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
            itemQuantity = itemView.findViewById(R.id.item_quantity);
            decreaseButton = itemView.findViewById(R.id.button_decrease);
            increaseButton = itemView.findViewById(R.id.button_increase);
            deleteButton = itemView.findViewById(R.id.button_delete);
        }
    }
}

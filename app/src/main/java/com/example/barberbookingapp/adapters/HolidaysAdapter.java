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
import com.google.firebase.database.DatabaseReference;
import java.util.List;

// Adapter for displaying all holidays in a RecyclerView
// Each holiday shows the date and a delete button
// Allows hair dressers to remove holidays from the list

public class HolidaysAdapter extends RecyclerView.Adapter<HolidaysAdapter.HolidayViewHolder> {
    private final List<HolidayItem> holidaysList;
    private final DatabaseReference holidaysRef;
    private final boolean isAdmin;

    public static class HolidayItem {
        private final String date;
        private final boolean isGeneral;

        public HolidayItem(String date, boolean isGeneral) {
            this.date = date;
            this.isGeneral = isGeneral;
        }

        public String getDate() {
            return date;
        }

        public boolean isGeneral() {
            return isGeneral;
        }
    }

    public HolidaysAdapter(List<HolidayItem> holidaysList, DatabaseReference holidaysRef, boolean isAdmin) {
        this.holidaysList = holidaysList;
        this.holidaysRef = holidaysRef;
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public HolidayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.holiday_item, parent, false);
        return new HolidayViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull HolidayViewHolder holder, int position) {
        HolidayItem holiday = holidaysList.get(position);
        holder.dateText.setText(holiday.getDate());
        holder.typeText.setText(holiday.isGeneral() ? "General Holiday" : "Personal Holiday");

        // Set different background for general holidays
        if (holiday.isGeneral()) {
            holder.itemView.setBackgroundResource(R.drawable.general_holiday_bg);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.holiday_item_bg);
        }

        // Show delete button only for personal holidays or if user is admin
        if (!holiday.isGeneral() || isAdmin) {
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setOnClickListener(v -> {
                if (holiday.isGeneral()) {
                    // Remove from general holidays
                    DatabaseReference generalHolidaysRef = holidaysRef.getRoot().child("general_holidays");
                    generalHolidaysRef.child(holiday.getDate()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                // Remove from all hairdressers
                                removeHolidayFromAllHairdressers(holiday.getDate());
                                // Remove from the list
                                if (position >= 0 && position < holidaysList.size()) {
                                    holidaysList.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position, holidaysList.size());
                                }
                                Toast.makeText(v.getContext(), "General holiday removed successfully",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(v.getContext(), "Failed to remove general holiday", Toast.LENGTH_SHORT)
                                        .show();
                            });
                } else {
                    // Remove personal holiday
                    holidaysRef.child(holiday.getDate()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                if (position >= 0 && position < holidaysList.size()) {
                                    holidaysList.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position, holidaysList.size());
                                }
                                Toast.makeText(v.getContext(), "Holiday removed successfully", Toast.LENGTH_SHORT)
                                        .show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(v.getContext(), "Failed to remove holiday", Toast.LENGTH_SHORT).show();
                            });
                }
            });
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }
    }

    private void removeHolidayFromAllHairdressers(String date) {
        DatabaseReference usersRef = holidaysRef.getRoot().child("users");
        usersRef.orderByChild("role").equalTo("hair dresser")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        for (com.google.firebase.database.DataSnapshot userSnap : snapshot.getChildren()) {
                            String username = userSnap.child("username").getValue(String.class);
                            if (username != null) {
                                DatabaseReference hairdresserHolidaysRef = holidaysRef.getRoot()
                                        .child("holidays")
                                        .child(username);
                                hairdresserHolidaysRef.child(date).removeValue();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        // Handle error
                    }
                });
    }

    @Override
    public int getItemCount() {
        return holidaysList.size();
    }

    static class HolidayViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;
        TextView typeText;
        Button deleteButton;

        HolidayViewHolder(View view) {
            super(view);
            dateText = view.findViewById(R.id.holiday_date);
            typeText = view.findViewById(R.id.holiday_type_text);
            deleteButton = view.findViewById(R.id.delete_holiday_button);
        }
    }
}

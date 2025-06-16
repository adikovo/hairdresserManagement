package com.example.barberbookingapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.barberbookingapp.R;
import com.google.firebase.database.DatabaseReference;
import java.util.List;

// Adapter for displaying all holidays in a RecyclerView
// Each holiday shows the date and a delete button
// Allows hair dressers to remove holidays from the list

public class HolidaysAdapter extends RecyclerView.Adapter<HolidaysAdapter.ViewHolder> {
    private List<String> holidaysList;
    private DatabaseReference databaseReference;

    public HolidaysAdapter(List<String> holidaysList, DatabaseReference databaseReference) {
        this.holidaysList = holidaysList;
        this.databaseReference = databaseReference;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holiday_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String holidayDate = holidaysList.get(position);
        holder.holidayDateText.setText(holidayDate);

        /*
        *בעת לחיצה על כפתור הביטול
        * מוצא את הנתיב המתאים
        * ומוחק את התאריך מהפיירבייס וכמו כן מודיע על שינוי בשביל לעדכן את הרשימה
        */
        holder.deleteHolidayButton.setOnClickListener(v -> {
            databaseReference.child(holidayDate).removeValue();
            holidaysList.remove(position);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return holidaysList.size();
    }

    //הצגת השדות הסופים בהם יופיע תאריך וכפתור ביטול לכל פריט ב RECVIEW
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView holidayDateText;
        Button deleteHolidayButton;

        public ViewHolder(View itemView) {
            super(itemView);
            holidayDateText = itemView.findViewById(R.id.holiday_date_text);
            deleteHolidayButton = itemView.findViewById(R.id.delete_holiday_button);
        }
    }
}

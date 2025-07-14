package com.example.hairsalonbookingapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hairsalonbookingapp.MainActivity;
import com.example.hairsalonbookingapp.R;
import com.example.hairsalonbookingapp.models.Appointments;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

// Adapter for displaying all appointments for admin in a RecyclerView
// Each appointment shows the date, time, service type, and hair dresser
// Allows admin to approve or cancel appointments

public class AdminAppointmentsAdapter extends RecyclerView.Adapter<AdminAppointmentsAdapter.AppointmentViewHolder> {

    private final Context context;
    private final List<Appointments> appointments;

    public AdminAppointmentsAdapter(Context context, List<Appointments> appointments) {
        this.context = context;
        this.appointments = appointments;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointments appointment = appointments.get(position);

        holder.dateTimeText.setText(appointment.getDateTime());
        holder.serviceTypeText.setText(appointment.getServiceType());

        // set background color based on appointment type
        int backgroundColor = appointment.isPersonalAppointment()
                ? context.getResources().getColor(R.color.color_lighter)
                : context.getResources().getColor(R.color.color_darker);
        holder.cardView.setCardBackgroundColor(backgroundColor);

        // Show hairdresser username for non-personal appointments
        if (!appointment.isPersonalAppointment() && appointment.getHairdresserUsername() != null) {
            holder.hairdresserText.setText("Hairdresser: " + appointment.getHairdresserUsername());
            holder.hairdresserText.setVisibility(View.VISIBLE);
        } else {
            holder.hairdresserText.setVisibility(View.GONE);
        }

        // handle appointment cancellation
        holder.cancelButton.setOnClickListener(v -> {

            String appointmentKey = appointment.getDateTime().replace(" ", "_").replace(":", "-");

            // cancel the appointment and remove from calendar
            if (context instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) context;
                mainActivity.markAppointmentAsCanceled(appointmentKey);

                // Remove from the list and update the RecyclerView
                appointments.remove(appointment);
                notifyItemRemoved(holder.getAdapterPosition());
                Toast.makeText(context, "Appointment cancelled and removed from calendar! Please notify the client.",
                        Toast.LENGTH_SHORT)
                        .show();
            } else {

                DatabaseReference appointmentRef = FirebaseDatabase.getInstance()
                        .getReference("appointments")
                        .child(appointmentKey);

                appointmentRef.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Firebase deletion successful - remove and update the recview
                        appointments.remove(appointment);
                        notifyItemRemoved(holder.getAdapterPosition());
                        Toast.makeText(context, "Appointment cancelled! Please notify the client.", Toast.LENGTH_SHORT)
                                .show();
                    } else {

                        Toast.makeText(context, "Failed to cancel appointment", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Load and display client information
        String clientId = appointment.getClientId();
        if (clientId != null && !clientId.isEmpty()) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(clientId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Update display with client information
                    String username = snapshot.child("username").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    holder.userName.setText(username != null ? username : "N/A");
                    holder.userPhone.setText(phone != null ? phone : "N/A");
                    holder.userEmail.setText(email != null ? email : "N/A");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        } else {

            holder.userName.setText("Unknown");
            holder.userPhone.setText("Unknown");
            holder.userEmail.setText("Unknown");
        }
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView dateTimeText;
        TextView serviceTypeText;
        TextView hairdresserText;
        TextView userName;
        TextView userPhone;
        TextView userEmail;
        Button cancelButton;

        AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            dateTimeText = itemView.findViewById(R.id.appointment_date_time);
            serviceTypeText = itemView.findViewById(R.id.appointment_service_type);
            hairdresserText = itemView.findViewById(R.id.appointment_hairdresser);
            userName = itemView.findViewById(R.id.user_name);
            userPhone = itemView.findViewById(R.id.user_phone);
            userEmail = itemView.findViewById(R.id.user_email);
            cancelButton = itemView.findViewById(R.id.cancel_appointment_button);
        }
    }
}

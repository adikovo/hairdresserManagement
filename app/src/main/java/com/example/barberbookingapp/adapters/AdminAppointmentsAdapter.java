package com.example.barberbookingapp.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barberbookingapp.R;
import com.example.barberbookingapp.models.Appointments;
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

        // Display date, time and service type
        holder.dateTimeText.setText(appointment.getDateTime());
        holder.serviceTypeText.setText(appointment.getServiceType());

        // Set background color based on whether it's a personal appointment
        int backgroundColor = appointment.isPersonalAppointment()
                ? context.getResources().getColor(R.color.personal_holiday_color)
                : context.getResources().getColor(R.color.general_holiday_color);
        holder.cardView.setCardBackgroundColor(backgroundColor);

        // Show hairdresser username for non-personal appointments
        if (!appointment.isPersonalAppointment() && appointment.getHairdresserUsername() != null) {
            holder.hairdresserText.setText("Hairdresser: " + appointment.getHairdresserUsername());
            holder.hairdresserText.setVisibility(View.VISIBLE);
        } else {
            holder.hairdresserText.setVisibility(View.GONE);
        }

        // Delete the selected appointment from Firebase when clicking Cancel
        holder.cancelButton.setOnClickListener(v -> {
            // Delete the appointment from Firebase - convert to appropriate format to reach
            // the correct path
            DatabaseReference appointmentRef = FirebaseDatabase.getInstance()
                    .getReference("appointments")
                    .child(appointment.getDateTime().replace(" ", "_").replace(":", "-"));

            appointmentRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Firebase deletion successful - remove and update the RECVIEW
                    appointments.remove(appointment);
                    notifyItemRemoved(holder.getAdapterPosition());
                    Toast.makeText(context, "Appointment cancelled! Please notify the client.", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    // Handle failure case
                    Toast.makeText(context, "Failed to cancel appointment", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Get clientId from APPOINTMENTS
        String clientId = appointment.getClientId();
        if (clientId != null && !clientId.isEmpty()) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(clientId);

            // Read and display user data from USERS
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Update user values in the appointment
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
            // If details not found, display UNKNOWN for the relevant part of the
            // appointment
            holder.userName.setText("Unknown");
            holder.userPhone.setText("Unknown");
            holder.userEmail.setText("Unknown");
        }
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    // Information sent to display the appointment in RECVIEW
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

package com.example.hairsalonbookingapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hairsalonbookingapp.R;
import com.example.hairsalonbookingapp.models.Appointments;

import java.util.List;

// Adapter for displaying all appointments in a RecyclerView
// Each appointment shows the date, time, service type, and hair dresser
// Allows users to cancel appointments

public class AppointmentsAdapter extends RecyclerView.Adapter<AppointmentsAdapter.AppointmentsViewHolder> {

    private Context context;
    private final List<Appointments> appointmentsList;
    private final OnAppointmentCancelListener cancelListener;

    // When clicking the cancel button, notifies the client fragment that an
    // appointment was deleted and the deletion action is performed there
    public interface OnAppointmentCancelListener {
        void onAppointmentCancel(Appointments appointment);
    }

    public AppointmentsAdapter(Context context, List<Appointments> appointmentsList,
            OnAppointmentCancelListener cancelListener) {
        this.context = context;
        this.appointmentsList = appointmentsList;
        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public AppointmentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_appointment, parent, false);
        return new AppointmentsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentsViewHolder holder, int position) {

        Appointments appointment = appointmentsList.get(position);
        holder.dateTimeText.setText(appointment.getDateTime());
        holder.serviceTypeText.setText(appointment.getServiceType());
        holder.hairdresserText.setText("Hairdresser: " + appointment.getHairdresserUsername());

        // Call for cancellation and deletion of the appointment when clicking the
        holder.cancelButton.setOnClickListener(v -> {
            if (cancelListener != null) {
                cancelListener.onAppointmentCancel(appointment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointmentsList.size();
    }

    // Final fields that will be displayed for each client appointment in RECVIEW
    public static class AppointmentsViewHolder extends RecyclerView.ViewHolder {
        TextView dateTimeText, serviceTypeText, hairdresserText;
        Button cancelButton;

        public AppointmentsViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTimeText = itemView.findViewById(R.id.appointment_date_time);
            serviceTypeText = itemView.findViewById(R.id.appointment_service_type);
            hairdresserText = itemView.findViewById(R.id.appointment_hairdresser);
            cancelButton = itemView.findViewById(R.id.cancel_appointment_button);
        }
    }
}

package com.example.hairsalonbookingapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hairsalonbookingapp.R;
import com.example.hairsalonbookingapp.models.Worker;

import java.util.List;

// Adapter for displaying all hair dressers (workers) in a RecyclerView
// Each worker shows their username, email, and phone number
// Allows admin to delete a worker from the list

public class WorkerAdapter extends RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder> {
    private List<Worker> workerList;
    private OnWorkerDeleteListener deleteListener;

    // Interface for handling worker deletion events
    public interface OnWorkerDeleteListener {
        void onDelete(Worker worker, int position);
    }

    public WorkerAdapter(List<Worker> workerList, OnWorkerDeleteListener deleteListener) {
        this.workerList = workerList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public WorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_worker, parent, false);
        return new WorkerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkerViewHolder holder, int position) {
        Worker worker = workerList.get(position);

        // Display worker information
        holder.usernameTextView.setText(worker.getUsername());
        holder.emailTextView.setText(worker.getEmail());
        holder.phoneTextView.setText(worker.getPhone());

        // Set up delete button click listener
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(worker, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return workerList.size();
    }

    // ViewHolder for worker item views
    public static class WorkerViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView, emailTextView, phoneTextView;
        Button deleteButton;

        public WorkerViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.worker_username);
            emailTextView = itemView.findViewById(R.id.worker_email);
            phoneTextView = itemView.findViewById(R.id.worker_phone);
            deleteButton = itemView.findViewById(R.id.delete_worker_button);
        }
    }
}
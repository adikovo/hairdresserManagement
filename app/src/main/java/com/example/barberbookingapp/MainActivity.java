package com.example.barberbookingapp;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.barberbookingapp.models.Appointments;
import com.example.barberbookingapp.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/*
Main file of the application
Contains functions that are called from fragments
Login function
Registration function
Appointment booking function
Username display function
And other functions related to fragment navigation
*/

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mAuth = FirebaseAuth.getInstance();

    }

    private FirebaseAuth mAuth;

    // Function for booking an appointment for a client
    public void bookAppointment(String serviceType, String dateTime, String hairdresserUsername) {
        // Get the current user's key
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Appointments appointment = new Appointments(serviceType, userId, dateTime, hairdresserUsername);

        // Create path to Firebase
        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");

        // Unique key for the appointment
        String appointmentKey = dateTime.replace(" ", "_").replace(":", "-");

        // Save the object in Firebase and display a message
        appointmentsRef.child(appointmentKey).setValue(appointment)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Convert date and time to milliseconds to add to calendar
                        long startTime = convertDateTimeToMillis(dateTime);
                        long endTime = startTime + 3600000; // Duration of one hour

                        // Add the event to the calendar
                        addAppointmentToCalendar(serviceType, getString(R.string.salon_info), startTime, endTime);
                    } else {
                        Toast.makeText(this, "Failed to book appointment", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Functions for adding the event to calendar
    private void addAppointmentToCalendar(String title, String location, long startTime, long endTime) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_CALENDAR }, 100);
            return;
        }

        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();

        values.put(CalendarContract.Events.CALENDAR_ID, 1);
        values.put(CalendarContract.Events.TITLE, title);
        values.put(CalendarContract.Events.EVENT_LOCATION, location);
        values.put(CalendarContract.Events.DTSTART, startTime);
        values.put(CalendarContract.Events.DTEND, endTime);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

        if (uri != null) {
            Toast.makeText(this, "Appointment scheduled successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to add appointment to calendar", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No permission to modify the calendar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private long convertDateTimeToMillis(String dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());

        try {
            Date date = sdf.parse(dateTime);
            if (date != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.setTimeZone(TimeZone.getDefault());

                return calendar.getTimeInMillis();
            } else {
                return System.currentTimeMillis();
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return System.currentTimeMillis();
        }
    }

    // Function that receives the user's key and returns the username
    // If no username is found, return GUEST
    public Task<String> fetchUsername(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();

        userRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    taskCompletionSource.setResult(dataSnapshot.getValue(String.class));
                } else {
                    taskCompletionSource.setResult("Guest");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                taskCompletionSource.setException(databaseError.toException());
            }
        });

        return taskCompletionSource.getTask();
    }

    // User login function
    // Receives email and password and checks in Firebase if the user exists
    // If exists, checks the ROLE to determine if they are a hairdresser or client
    // and
    // directs them to the appropriate fragment
    public void loginUser(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid();
                                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users")
                                        .child(userId);

                                // Check the ROLE
                                userRef.child("role").get().addOnCompleteListener(roleTask -> {
                                    if (roleTask.isSuccessful()) {
                                        DataSnapshot dataSnapshot = roleTask.getResult();
                                        if (dataSnapshot.exists()) {
                                            String role = dataSnapshot.getValue(String.class);
                                            if (role != null) {
                                                if ("client".equals(role)) {
                                                    navigateToHomeFragment();
                                                } else if ("hair dresser".equals(role) || "admin".equals(role)) {
                                                    navigateToAdminFragment();
                                                } else {
                                                    Toast.makeText(this, "Invalid user role", Toast.LENGTH_SHORT)
                                                            .show();
                                                }
                                            }
                                        } else {
                                            Toast.makeText(this, "Role not found", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(this,
                                                "Failed to fetch role: " + roleTask.getException().getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error during login: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Navigation functions to appropriate fragments
    private void navigateToAdminFragment() {
        NavController navController = Navigation.findNavController(this, R.id.fragmentContainerView);
        navController.navigate(R.id.action_loginFragment_to_adminFragment);
    }

    private void navigateToHomeFragment() {
        NavController navController = Navigation.findNavController(this, R.id.fragmentContainerView);
        navController.navigate(R.id.action_loginFragment_to_homeFragment);
    }

    private void navigateToLoginFragment() {
        NavController navController = Navigation.findNavController(this, R.id.fragmentContainerView);
        navController.navigate(R.id.action_registerFragment_to_loginFragment);
    }

    // Function for registering a new user
    public void registerUser(String email, String password, String confirmPassword, String username, String phone,
            String role) {
        // Check if all fields are filled
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || username.isEmpty() || phone.isEmpty()
                || role.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check password verification
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // If everything is valid, create user and save in Firebase
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            User newUser = new User(username, email, role, phone);

                            FirebaseDatabase.getInstance().getReference("users")
                                    .child(userId)
                                    .setValue(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        // If the user is a hair dresser, also add to hairdressers table
                                        if ("hair dresser".equals(role)) {
                                            FirebaseDatabase.getInstance().getReference("hairdressers")
                                                    .child(userId)
                                                    .setValue(newUser)
                                                    .addOnSuccessListener(aVoid1 -> {
                                                        Toast.makeText(this, "Hairdresser registered successfully",
                                                                Toast.LENGTH_SHORT).show();
                                                        // Navigate back to WorkersFragment if we came from there
                                                        NavController navController = Navigation.findNavController(this,
                                                                R.id.fragmentContainerView);
                                                        navController.navigate(
                                                                R.id.action_registerFragment_to_workersFragment);
                                                    });
                                        } else {
                                            Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT)
                                                    .show();
                                            navigateToLoginFragment();
                                        }
                                    })
                                    .addOnFailureListener(e -> Toast
                                            .makeText(this, "Failed to save user", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
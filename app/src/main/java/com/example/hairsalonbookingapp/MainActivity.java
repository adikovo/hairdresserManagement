package com.example.hairsalonbookingapp;

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

import com.example.hairsalonbookingapp.models.Appointments;
import com.example.hairsalonbookingapp.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import android.content.ContentUris;
import android.content.Intent;

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

        // Clean up outdated appointments and holidayswhen app starts
        cleanupOutdatedAppointments();
        cleanupOutdatedHolidays();

        // Check and request calendar permissions if needed
        checkCalendarPermissions();
    }

    private FirebaseAuth mAuth;

    // function for booking an appointment for a client
    public void bookAppointment(String serviceType, String dateTime, String hairdresserUsername) {
        // get the current user's key
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Appointments appointment = new Appointments(serviceType, userId, dateTime, hairdresserUsername);

        // create path to Firebase
        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");

        String appointmentKey = dateTime.replace(" ", "_").replace(":", "-");

        // save the object in Firebase and display a message
        appointmentsRef.child(appointmentKey).setValue(appointment)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        long startTime = convertDateTimeToMillis(dateTime);
                        long durationMillis = Long.parseLong(appointment.getDuration()) * 60 * 1000;
                        long endTime = startTime + durationMillis;

                        // Add the event to the calendar and store the event ID
                        addAppointmentToCalendar(serviceType, getString(R.string.salon_info), startTime, endTime,
                                appointmentKey);
                    } else {
                        Toast.makeText(this, "Failed to book appointment", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Function to clean up outdated appointments
    public void cleanupOutdatedAppointments() {
        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");

        appointmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long currentTime = System.currentTimeMillis();

                for (DataSnapshot appointmentSnap : snapshot.getChildren()) {
                    Appointments appointment = appointmentSnap.getValue(Appointments.class);
                    if (appointment != null && appointment.getDateTime() != null) {
                        // Check if appointment should be deleted (canceled, completed, or past)
                        if ("canceled".equals(appointment.getStatus()) ||
                                "completed".equals(appointment.getStatus()) ||
                                (appointment.getStatus() == null || "scheduled".equals(appointment.getStatus())) &&
                                        convertDateTimeToMillis(appointment.getDateTime())
                                                + Long.parseLong(appointment.getDuration()) * 60 * 1000 < currentTime) {

                            appointmentSnap.getRef().removeValue();
                        }
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Function to clean up outdated holidays
    public void cleanupOutdatedHolidays() {
        long currentTime = System.currentTimeMillis();

        // Clean up personal holidays for each hairdresser
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String role = userSnap.child("role").getValue(String.class);
                    if ("hair dresser".equals(role) || "admin".equals(role)) {
                        String username = userSnap.child("username").getValue(String.class);
                        if (username != null) {
                            cleanupHolidays("holidays/" + username, currentTime);
                        }
                    }
                }

                // Clean up general holidays
                cleanupHolidays("general_holidays", currentTime);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Helper method to clean up holidays from any path
    private void cleanupHolidays(String path, long currentTime) {
        DatabaseReference holidaysRef = FirebaseDatabase.getInstance().getReference(path);

        holidaysRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot holidaySnap : snapshot.getChildren()) {
                    String holidayDate = holidaySnap.getKey();
                    if (holidayDate != null && isDateInPast(holidayDate, currentTime)) {
                        holidaySnap.getRef().removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Helper method to check if a date is in the past
    private boolean isDateInPast(String dateString, long currentTime) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date holidayDate = formatter.parse(dateString);

            if (holidayDate != null) {
                // Convert to start of day for comparison
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(holidayDate);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                long holidayTime = calendar.getTimeInMillis();
                return holidayTime < currentTime;
            }
        } catch (ParseException e) {
            // If date parsing fails, assume it's not in the past
            return false;
        }
        return false;
    }

    // Function to update appointment status (completed, canceled, etc.)
    public void updateAppointmentStatus(String appointmentKey, String newStatus) {
        DatabaseReference appointmentRef = FirebaseDatabase.getInstance().getReference("appointments")
                .child(appointmentKey);

        appointmentRef.child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    // If appointment is canceled, remove the calendar event and remove from firebase
                    if ("canceled".equals(newStatus)) {
                        removeCalendarEventForAppointment(appointmentKey);
                        // Remove the appointment from firebase after a delay to ensure calendarevent is removed
                        new android.os.Handler().postDelayed(() -> {
                            appointmentRef.removeValue();
                        }, 500);
                    }

                    String message = "Appointment marked as " + newStatus;
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    String message = "Failed to mark appointment as " + newStatus;
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                });
    }

    // Convenience methods for common status updates
    public void markAppointmentAsCompleted(String appointmentKey) {
        updateAppointmentStatus(appointmentKey, "completed");
    }

    public void markAppointmentAsCanceled(String appointmentKey) {
        updateAppointmentStatus(appointmentKey, "canceled");
    }

    // Function to remove calendar event for an appointment
    private void removeCalendarEventForAppointment(String appointmentKey) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Calendar", "No permission to remove calendar event");
            return;
        }

        DatabaseReference appointmentRef = FirebaseDatabase.getInstance().getReference("appointments")
                .child(appointmentKey);

        appointmentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Appointments appointment = snapshot.getValue(Appointments.class);
                if (appointment != null && appointment.getCalendarEventId() != null) {
                    try {
                        ContentResolver cr = getContentResolver();
                        Uri deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,
                                Long.parseLong(appointment.getCalendarEventId()));
                        int deletedRows = cr.delete(deleteUri, null, null);

                        if (deletedRows > 0) {
                            Log.d("Calendar", "Calendar event removed successfully");
                        } else {
                            Log.e("Calendar", "Failed to remove calendar event");
                        }
                    } catch (Exception e) {
                        Log.e("Calendar", "Error removing calendar event: " + e.getMessage(), e);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Calendar", "Error fetching appointment for calendar removal: " + error.getMessage());
            }
        });
    }

    // Function to add appointment to calendar (with optional callback to store event ID)
    private void addAppointmentToCalendar(String title, String location, long startTime, long endTime,
            String appointmentKey) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR }, 100);
            // Store the appointment details to add after permission is granted
            pendingCalendarEvent = new CalendarEvent(title, location, startTime, endTime);
            pendingAppointmentKey = appointmentKey;
            return;
        }

        insertCalendarEvent(title, location, startTime, endTime, appointmentKey);
    }

    // Helper class to store pending calendar events
    private static class CalendarEvent {
        String title, location;
        long startTime, endTime;

        CalendarEvent(String title, String location, long startTime, long endTime) {
            this.title = title;
            this.location = location;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    private CalendarEvent pendingCalendarEvent;
    private String pendingAppointmentKey;

    // Function to check and request calendar permissions
    private void checkCalendarPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {

            // Show a dialog explaining why we need calendar permissions
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Calendar Permission")
                    .setMessage(
                            "This app needs calendar permissions to automatically add your appointments to your calendar. This helps you keep track of your scheduled visits.")
                    .setPositiveButton("Grant Permission", (dialog, which) -> {
                        ActivityCompat.requestPermissions(this,
                                new String[] { Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR },
                                101);
                    })
                    .setNegativeButton("Not Now", (dialog, which) -> {
                        // User chose not to grant permission now
                        Log.d("Calendar", "User declined calendar permission request");
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private void insertCalendarEvent(String title, String location, long startTime, long endTime,
            String appointmentKey) {
        ContentResolver cr = getContentResolver();

        long calendarId = getWritableCalendarId();
        if (calendarId == -1) {
            Toast.makeText(this, "No writable calendar found. Please add a calendar account.", Toast.LENGTH_LONG)
                    .show();
            Log.e("Calendar", "No writable calendar found");
            showCalendarSetupInstructions();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        values.put(CalendarContract.Events.TITLE, title);
        values.put(CalendarContract.Events.EVENT_LOCATION, location);
        values.put(CalendarContract.Events.DTSTART, startTime);
        values.put(CalendarContract.Events.DTEND, endTime);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        values.put(CalendarContract.Events.HAS_ALARM, 1); // Enable reminders
        values.put(CalendarContract.Events.DESCRIPTION, "Hair Salon Appointment - " + title);

        try {
            Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

            if (uri != null) {
                // Extract the event ID from the URI and store it if appointmentKey is provided
                if (appointmentKey != null) {
                    String eventId = uri.getLastPathSegment();

                    // Store the calendar event ID in the appointment record
                    DatabaseReference appointmentRef = FirebaseDatabase.getInstance().getReference("appointments")
                            .child(appointmentKey);
                    appointmentRef.child("calendarEventId").setValue(eventId);
                }

                // Add a reminder (15 minutes before)
                addCalendarReminder(uri, 15);

                Toast.makeText(this, "Appointment added to calendar successfully", Toast.LENGTH_SHORT).show();
                Log.d("Calendar", "Event added successfully: " + uri.toString());
            } else {
                Toast.makeText(this, "Failed to add appointment to calendar", Toast.LENGTH_SHORT).show();
                Log.e("Calendar", "Failed to insert event - uri is null");
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Calendar permission denied. Please grant calendar permissions.", Toast.LENGTH_LONG)
                    .show();
            Log.e("Calendar", "Security exception while inserting event: " + e.getMessage(), e);
            checkCalendarPermissions();
        } catch (Exception e) {
            Toast.makeText(this, "Error adding to calendar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("Calendar", "Exception while inserting event: " + e.getMessage(), e);
        }
    }

    private long getWritableCalendarId() {
        ContentResolver cr = getContentResolver();
        String[] projection = new String[] {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.SYNC_EVENTS
        };

        String selection = CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= " +
                CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR + " AND " +
                CalendarContract.Calendars.SYNC_EVENTS + " = 1";

        try {
            android.database.Cursor cursor = cr.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    selection,
                    null,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                long calendarId = cursor.getLong(0);
                String calendarName = cursor.getString(1);
                String accountName = cursor.getString(2);

                Log.d("Calendar", "Using calendar: " + calendarName + " (" + accountName + ")");
                cursor.close();
                return calendarId;
            }

            if (cursor != null) {
                cursor.close();
            }

            // If no synced calendars found, try to find any writable calendar
            selection = CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= " +
                    CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR;

            cursor = cr.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    selection,
                    null,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                long calendarId = cursor.getLong(0);
                String calendarName = cursor.getString(1);
                String accountName = cursor.getString(2);

                Log.d("Calendar", "Using non-synced calendar: " + calendarName + " (" + accountName + ")");
                cursor.close();
                return calendarId;
            }

            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("Calendar", "Error querying calendars: " + e.getMessage(), e);
        }

        return -1; // No writable calendar found
    }

    private void addCalendarReminder(Uri eventUri, int minutesBefore) {
        try {
            ContentResolver cr = getContentResolver();
            ContentValues reminderValues = new ContentValues();

            // Extract the event ID from the URI
            String eventId = eventUri.getLastPathSegment();

            reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventId);
            reminderValues.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
            reminderValues.put(CalendarContract.Reminders.MINUTES, minutesBefore);

            Uri reminderUri = cr.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues);

            if (reminderUri != null) {
                Log.d("Calendar", "Reminder added successfully");
            } else {
                Log.e("Calendar", "Failed to add reminder");
            }
        } catch (Exception e) {
            Log.e("Calendar", "Error adding reminder: " + e.getMessage(), e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 || requestCode == 101) {
            boolean allPermissionsGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    Log.e("Calendar", "Permission denied: " + permissions[i]);
                }
            }

            if (allPermissionsGranted) {
                Toast.makeText(this, "Calendar permissions granted!", Toast.LENGTH_SHORT).show();
                Log.d("Calendar", "All calendar permissions granted successfully");
                // Retry adding the calendar event if there was a pending one
                if (pendingCalendarEvent != null) {
                    insertCalendarEvent(pendingCalendarEvent.title, pendingCalendarEvent.location,
                            pendingCalendarEvent.startTime, pendingCalendarEvent.endTime, pendingAppointmentKey);
                    pendingCalendarEvent = null;
                    pendingAppointmentKey = null;
                }
            } else {
                if (requestCode == 100) {
                    Toast.makeText(this, "Calendar permissions denied. Cannot add events to calendar.",
                            Toast.LENGTH_LONG)
                            .show();
                }
                Log.e("Calendar", "Some calendar permissions were denied");
                // Clear pending event since permissions were denied
                pendingCalendarEvent = null;
                pendingAppointmentKey = null;
            }
        }
    }

    private long convertDateTimeToMillis(String dateTime) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        try {
            Date date = formatter.parse(dateTime);
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

    // Function that receives the user's key and returns the username if no username is found, return GUEST
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
    /*
    User login function
    Receives email and password and checks in Firebase if the user exists
    If exists, checks the ROLE to determine if they are a hairdresser or client
    and directs them to the appropriate fragment
     */
    public void loginUser(String email, String password, Runnable onComplete) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            if (onComplete != null)
                onComplete.run();
            return;
        }

        Log.d("Login", "Starting login process for: " + email);

        try {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d("Login", "Firebase Auth successful");
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid();
                                Log.d("Login", "User ID: " + userId);
                                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users")
                                        .child(userId);

                                // Check the ROLE
                                userRef.child("role").get().addOnCompleteListener(roleTask -> {
                                    if (roleTask.isSuccessful()) {
                                        Log.d("Login", "Role fetch successful");
                                        DataSnapshot dataSnapshot = roleTask.getResult();
                                        if (dataSnapshot.exists()) {
                                            String role = dataSnapshot.getValue(String.class);
                                            Log.d("Login", "User role: " + role);
                                            if (role != null) {
                                                if ("client".equals(role)) {
                                                    Log.d("Login", "Navigating to HomeFragment");
                                                    navigateToHomeFragment();
                                                } else if ("hair dresser".equals(role) || "admin".equals(role)) {
                                                    Log.d("Login", "Navigating to AdminFragment");
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
                                        Log.e("Login", "Role fetch failed: " + roleTask.getException().getMessage());
                                        Toast.makeText(this,
                                                "Failed to fetch role: " + roleTask.getException().getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    if (onComplete != null)
                                        onComplete.run();
                                });
                            }
                        } else {
                            Log.e("Login", "Firebase Auth failed: " + task.getException().getMessage());
                            // Provide more specific error messages
                            String errorMessage = "Login failed";
                            if (task.getException() != null) {
                                String exceptionMessage = task.getException().getMessage();
                                if (exceptionMessage != null) {
                                    if (exceptionMessage.contains("password")) {
                                        errorMessage = "Invalid password";
                                    } else if (exceptionMessage.contains("user")) {
                                        errorMessage = "User not found";
                                    } else if (exceptionMessage.contains("network")) {
                                        errorMessage = "Network error. Please check your connection";
                                    } else {
                                        errorMessage = "Login failed: " + exceptionMessage;
                                    }
                                }
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                            if (onComplete != null)
                                onComplete.run();
                        }
                    });
        } catch (Exception e) {
            Log.e("Login", "Exception during login: " + e.getMessage());
            Toast.makeText(this, "Error during login: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (onComplete != null)
                onComplete.run();
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

        // Check password length
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
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

                            FirebaseDatabase.getInstance().getReference("users").child(userId).setValue(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        // If the user is a hair dresser, also add to hairdressers table
                                        if ("hair dresser".equals(role)) {
                                            FirebaseDatabase.getInstance().getReference("hairdressers").child(userId)
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
                                            .makeText(this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_LONG)
                                            .show());
                        }
                    } else {
                        String errorMessage = "Registration failed";
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("email")) {
                                    errorMessage = "Email already exists";
                                } else if (exceptionMessage.contains("network")) {
                                    errorMessage = "Network error. Please check your connection";
                                } else {
                                    errorMessage = "Registration failed: " + exceptionMessage;
                                }
                            }
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Function to check if device has any writable calendars
    public boolean hasWritableCalendar() {
        return getWritableCalendarId() != -1;
    }

    // Function to show calendar setup instructions
    public void showCalendarSetupInstructions() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Calendar Setup Required")
                .setMessage("To add appointments to your calendar, you need to:\n\n" +
                        "1. Add a calendar account (Google, Outlook, etc.)\n" +
                        "2. Grant calendar permissions to this app\n" +
                        "3. Make sure the calendar is set to sync\n\n" +
                        "Would you like to open calendar settings?")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_SYNC_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
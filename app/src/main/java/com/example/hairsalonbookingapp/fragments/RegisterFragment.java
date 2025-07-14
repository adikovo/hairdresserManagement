package com.example.hairsalonbookingapp.fragments;

import static android.view.View.INVISIBLE;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.hairsalonbookingapp.MainActivity;
import com.example.hairsalonbookingapp.R;

/*
Registration fragment for the system
In this fragment, users fill in details for initial registration: username, email, phone, password, and password confirmation
 */

public class RegisterFragment extends Fragment {
    private EditText usernameField, emailField, passwordField, confirmPasswordField, phoneField;
    private Button registerButton;
    private String userRole = "client"; // default

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public RegisterFragment() {

    }

    public static RegisterFragment newInstance(String param1, String param2) {
        RegisterFragment fragment = new RegisterFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        usernameField = view.findViewById(R.id.usernameRegister);
        emailField = view.findViewById(R.id.emailRegister);
        passwordField = view.findViewById(R.id.passwordRegister);
        confirmPasswordField = view.findViewById(R.id.confirmPasswordRegister);
        registerButton = view.findViewById(R.id.register_buttonRegister);
        phoneField = view.findViewById(R.id.phoneField);

        // check if we came from Workers Fragment
        Bundle args = getArguments();
        if (args != null && args.getBoolean("register_hairdresser", false)) {
            userRole = "hair dresser";
        }

        registerButton.setOnClickListener(v -> {
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();
            String confirmPassword = confirmPasswordField.getText().toString();
            String username = usernameField.getText().toString();
            String phone = phoneField.getText().toString();

            // Call the function for registering a new user
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.registerUser(email, password, confirmPassword, username, phone, userRole);
        });

        return view;
    }
}
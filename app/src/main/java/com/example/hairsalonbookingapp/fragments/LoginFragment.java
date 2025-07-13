package com.example.hairsalonbookingapp.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.widget.Button;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.hairsalonbookingapp.MainActivity;
import com.example.hairsalonbookingapp.R;

/*
Login fragment for the system
Allows users to log in with email and password
Redirects users to the appropriate fragment based on their role (hair dresser or client)
 */

public class LoginFragment extends Fragment {

    private EditText emailField, passwordField;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public LoginFragment() {

    }

    public static LoginFragment newInstance(String param1, String param2) {
        LoginFragment fragment = new LoginFragment();
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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // When clicking the register button, the user navigates to a fragment where they can register
        Button registerButton = view.findViewById(R.id.register_buttonLogin);
        registerButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_registerFragment);
        });

        emailField = view.findViewById(R.id.emailLogin);
        passwordField = view.findViewById(R.id.passwordLogin);

        Button loginButton = view.findViewById(R.id.login_buttonLogin);
        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();

            // Check if email and password are not empty
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            loginButton.setEnabled(false); // Disable button during login

            // Check if activity is not null before proceeding
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.loginUser(email, password, () -> loginButton.setEnabled(true));
            } else {
                Toast.makeText(getContext(), "Error: Please try again", Toast.LENGTH_SHORT).show();
                loginButton.setEnabled(true); // Re-enable on error
            }
        });

        return view;
    }
}
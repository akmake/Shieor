package com.example.goodstart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView goToRegister, errorText;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // Auto-login if already signed in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            try {
                navigateToMain();
                return;
            } catch (Exception e) {
                auth.signOut();
            }
        }

        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        goToRegister = findViewById(R.id.goToRegister);
        errorText = findViewById(R.id.errorText);
        progressBar = findViewById(R.id.progressBar);

        // Clear pre-filled text if it matches hints (safety for some device behaviors)
        if (emailInput.getText().toString().equals(getString(R.string.email_hint))) {
            emailInput.setText("");
        }
        if (passwordInput.getText().toString().equals(getString(R.string.password_hint))) {
            passwordInput.setText("");
        }

        loginButton.setOnClickListener(v -> login());
        goToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void login() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_empty_fields));
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> navigateToMain())
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError(getString(R.string.error_login_failed) + ": " + e.getMessage());
                });
    }

    private void navigateToMain() {
        // Check if user has a household
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    Intent intent;
                    if (doc.exists() && doc.getString("householdId") != null) {
                        intent = new Intent(this, MainActivity.class);
                    } else {
                        intent = new Intent(this, HouseholdSetupActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // No user doc yet, go to household setup
                    Intent intent = new Intent(this, HouseholdSetupActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
    }
}

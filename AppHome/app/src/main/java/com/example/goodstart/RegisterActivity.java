package com.example.goodstart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput, emailInput, passwordInput;
    private Button registerButton;
    private TextView goToLogin, errorText;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        registerButton = findViewById(R.id.registerButton);
        goToLogin = findViewById(R.id.goToLogin);
        errorText = findViewById(R.id.errorText);
        progressBar = findViewById(R.id.progressBar);

        // Clear pre-filled text if it matches hints
        if (nameInput.getText().toString().equals(getString(R.string.name_hint))) nameInput.setText("");
        if (emailInput.getText().toString().equals(getString(R.string.email_hint))) emailInput.setText("");
        if (passwordInput.getText().toString().equals(getString(R.string.password_hint))) passwordInput.setText("");

        registerButton.setOnClickListener(v -> register());
        goToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void register() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_empty_fields));
            return;
        }

        if (password.length() < 6) {
            showError(getString(R.string.error_short_password));
            return;
        }

        setLoading(true);
        errorText.setVisibility(View.GONE);

        try {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() == null) {
                        setLoading(false);
                        showError(getString(R.string.error_register_failed));
                        return;
                    }
                    String uid = authResult.getUser().getUid();
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name", name);
                    userData.put("email", email);
                    userData.put("uid", uid);

                    db.collection("users").document(uid)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Intent intent = new Intent(this, HouseholdSetupActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                showError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError(getString(R.string.error_register_failed) + ": " + e.getMessage());
                });
        } catch (Exception e) {
            setLoading(false);
            showError(getString(R.string.error_register_failed) + ": " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!loading);
    }
}

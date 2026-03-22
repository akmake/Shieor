package com.example.goodstart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class HouseholdSetupActivity extends AppCompatActivity {

    private Button createHouseholdBtn, continueBtn, joinBtn;
    private LinearLayout codeDisplaySection;
    private TextView householdCodeText, errorText;
    private EditText codeInput;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentHouseholdId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_household_setup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        createHouseholdBtn = findViewById(R.id.createHouseholdBtn);
        continueBtn = findViewById(R.id.continueBtn);
        joinBtn = findViewById(R.id.joinBtn);
        codeDisplaySection = findViewById(R.id.codeDisplaySection);
        householdCodeText = findViewById(R.id.householdCodeText);
        codeInput = findViewById(R.id.codeInput);
        errorText = findViewById(R.id.errorText);
        progressBar = findViewById(R.id.progressBar);

        createHouseholdBtn.setOnClickListener(v -> createHousehold());
        continueBtn.setOnClickListener(v -> goToMain());
        joinBtn.setOnClickListener(v -> joinHousehold());
    }

    private void createHousehold() {
        setLoading(true);
        if (auth.getCurrentUser() == null) {
            setLoading(false);
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        String code = generateCode();

        Map<String, Object> household = new HashMap<>();
        household.put("code", code);
        household.put("members", Arrays.asList(uid));
        household.put("createdAt", FieldValue.serverTimestamp());

        db.collection("households").add(household)
                .addOnSuccessListener(docRef -> {
                    currentHouseholdId = docRef.getId();

                    // Update user with household ID
                    Map<String, Object> update = new HashMap<>();
                    update.put("householdId", currentHouseholdId);
                    db.collection("users").document(uid)
                            .update(update)
                            .addOnSuccessListener(aVoid -> {
                                setLoading(false);
                                createHouseholdBtn.setVisibility(View.GONE);
                                codeDisplaySection.setVisibility(View.VISIBLE);
                                householdCodeText.setText(code);
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                showError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
    }

    private void joinHousehold() {
        String code = codeInput.getText().toString().trim();
        if (code.isEmpty()) {
            showError(getString(R.string.error_empty_fields));
            return;
        }

        setLoading(true);
        if (auth.getCurrentUser() == null) {
            setLoading(false);
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        db.collection("households")
                .whereEqualTo("code", code)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        setLoading(false);
                        showError("קוד לא נמצא");
                        return;
                    }

                    String householdId = querySnapshot.getDocuments().get(0).getId();

                    // Add user to household members
                    db.collection("households").document(householdId)
                            .update("members", FieldValue.arrayUnion(uid))
                            .addOnSuccessListener(aVoid -> {
                                // Update user with household ID
                                Map<String, Object> update = new HashMap<>();
                                update.put("householdId", householdId);
                                db.collection("users").document(uid)
                                        .update(update)
                                        .addOnSuccessListener(aVoid2 -> {
                                            setLoading(false);
                                            goToMain();
                                        })
                                        .addOnFailureListener(e -> {
                                            setLoading(false);
                                            showError(e.getMessage());
                                        });
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                showError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}

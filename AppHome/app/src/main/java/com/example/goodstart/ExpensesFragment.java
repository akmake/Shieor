package com.example.goodstart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goodstart.adapters.ExpenseAdapter;
import com.example.goodstart.models.Expense;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ExpensesFragment extends Fragment {

    private static final String ARG_HOUSEHOLD_ID = "householdId";

    private String householdId;
    private FirebaseFirestore db;
    private String currentUid;
    private String currentUserName;

    private RecyclerView expensesRecycler;
    private TextView emptyText, monthlyTotalText;
    private ExtendedFloatingActionButton addExpenseFab;
    private ExpenseAdapter adapter;
    private List<Expense> expenses = new ArrayList<>();
    private ListenerRegistration listener;

    private String[] categories;
    private String[] categoryKeys = {"food", "transport", "entertainment", "bills",
            "shopping", "health", "education", "other"};

    public static ExpensesFragment newInstance(String householdId) {
        ExpensesFragment fragment = new ExpensesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_HOUSEHOLD_ID, householdId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            householdId = getArguments().getString(ARG_HOUSEHOLD_ID);
        }
        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        categories = new String[]{
                getString(R.string.cat_food),
                getString(R.string.cat_transport),
                getString(R.string.cat_entertainment),
                getString(R.string.cat_bills),
                getString(R.string.cat_shopping),
                getString(R.string.cat_health),
                getString(R.string.cat_education),
                getString(R.string.cat_other)
        };

        expensesRecycler = view.findViewById(R.id.expensesRecycler);
        emptyText = view.findViewById(R.id.emptyText);
        monthlyTotalText = view.findViewById(R.id.monthlyTotalText);
        addExpenseFab = view.findViewById(R.id.addExpenseFab);

        adapter = new ExpenseAdapter(expenses, getContext());
        expensesRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        expensesRecycler.setAdapter(adapter);

        // Load user name
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserName = doc.getString("name");
                    }
                });

        addExpenseFab.setOnClickListener(v -> showAddExpenseDialog());

        loadExpenses();

        return view;
    }

    private void loadExpenses() {
        if (householdId == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date monthStart = cal.getTime();

        listener = db.collection("households").document(householdId)
                .collection("expenses")
                .whereGreaterThanOrEqualTo("timestamp", monthStart)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || !isAdded()) return;

                    expenses.clear();
                    double total = 0;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Expense expense = doc.toObject(Expense.class);
                        expense.setId(doc.getId());
                        expenses.add(expense);
                        total += expense.getAmount();
                    }
                    adapter.notifyDataSetChanged();
                    monthlyTotalText.setText(getString(R.string.monthly_total,
                            String.format("%.0f", total)));
                    updateEmptyState();
                });
    }

    private void showAddExpenseDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_expense, null);

        EditText titleInput = dialogView.findViewById(R.id.expenseTitleInput);
        EditText amountInput = dialogView.findViewById(R.id.expenseAmountInput);
        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(spinnerAdapter);

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String amountStr = amountInput.getText().toString().trim();

                    if (title.isEmpty() || amountStr.isEmpty()) return;

                    double amount;
                    try {
                        amount = Double.parseDouble(amountStr);
                    } catch (NumberFormatException e) {
                        return;
                    }

                    int selectedIndex = categorySpinner.getSelectedItemPosition();
                    String category = categoryKeys[selectedIndex];

                    Expense expense = new Expense(title, amount, category,
                            currentUid, currentUserName != null ? currentUserName : "");

                    db.collection("households").document(householdId)
                            .collection("expenses")
                            .add(expense);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateEmptyState() {
        if (expenses.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            expensesRecycler.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            expensesRecycler.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) listener.remove();
    }
}

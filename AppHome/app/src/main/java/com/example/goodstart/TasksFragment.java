package com.example.goodstart;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goodstart.adapters.TaskAdapter;
import com.example.goodstart.models.HouseholdTask;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TasksFragment extends Fragment {

    private static final String ARG_HOUSEHOLD_ID = "householdId";
    private static final String ARG_PARTNER_UID = "partnerUid";
    private static final String ARG_PARTNER_NAME = "partnerName";

    private String householdId, partnerUid, partnerName;
    private FirebaseFirestore db;
    private String currentUid;
    private String currentUserName;

    private RecyclerView tasksRecycler;
    private TextView emptyText;
    private EditText quickAddTaskInput;
    private TextView quickAddTaskButton;
    private TaskAdapter adapter;
    private final List<HouseholdTask> tasksList = new ArrayList<>();
    private ListenerRegistration listener;

    public static TasksFragment newInstance(String householdId, String partnerUid, String partnerName) {
        TasksFragment fragment = new TasksFragment();
        Bundle args = new Bundle();
        args.putString(ARG_HOUSEHOLD_ID, householdId);
        args.putString(ARG_PARTNER_UID, partnerUid);
        args.putString(ARG_PARTNER_NAME, partnerName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            householdId = getArguments().getString(ARG_HOUSEHOLD_ID);
            partnerUid = getArguments().getString(ARG_PARTNER_UID);
            partnerName = getArguments().getString(ARG_PARTNER_NAME);
        }
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUid = user != null ? user.getUid() : null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        tasksRecycler = view.findViewById(R.id.tasksRecycler);
        emptyText = view.findViewById(R.id.emptyText);
        quickAddTaskInput = view.findViewById(R.id.quickAddTaskInput);
        quickAddTaskButton = view.findViewById(R.id.quickAddTaskButton);

        adapter = new TaskAdapter(tasksList, currentUid, new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onToggleCompleted(HouseholdTask task, boolean completed) {
                toggleTaskCompleted(task, completed);
            }

            @Override
            public void onDelete(HouseholdTask task) {
                deleteTask(task);
            }
        });

        tasksRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        tasksRecycler.setAdapter(adapter);

        if (currentUid == null) {
            showError("You are not logged in.");
            return view;
        }

        if (householdId == null || householdId.trim().isEmpty()) {
            showError("Household is missing.");
            return view;
        }

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserName = doc.getString("name");
                    }
                });

        setupQuickAdd();
        loadTasks();

        return view;
    }

    private void setupQuickAdd() {
        if (quickAddTaskButton != null) {
            quickAddTaskButton.setOnClickListener(v -> submitQuickAdd());
        }

        quickAddTaskInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                submitQuickAdd();
                return true;
            }
            return false;
        });
    }

    private void submitQuickAdd() {
        String title = quickAddTaskInput.getText().toString().trim();
        if (!title.isEmpty()) {
            addNewTask(title);
            quickAddTaskInput.setText("");
        }
    }

    private void addNewTask(String title) {
        if (householdId == null || currentUid == null) {
            showError("Cannot add task right now.");
            return;
        }

        String displayName = currentUserName != null ? currentUserName : "Member";
        HouseholdTask task = new HouseholdTask(
                title,
                "",
                currentUid,
                displayName,
                currentUid,
                displayName,
                false
        );

        db.collection("households").document(householdId)
                .collection("tasks")
                .add(task)
                .addOnFailureListener(e -> showError("Failed to save task."));
    }

    private void loadTasks() {
        listener = db.collection("households").document(householdId)
                .collection("tasks")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (!isAdded()) return;
                    if (error != null) {
                        showError("Failed to load tasks.");
                        return;
                    }
                    if (querySnapshot == null) return;

                    tasksList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        HouseholdTask task = doc.toObject(HouseholdTask.class);
                        task.setId(doc.getId());
                        tasksList.add(task);
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    private void toggleTaskCompleted(HouseholdTask task, boolean completed) {
        if (householdId == null || task.getId() == null) return;
        db.collection("households").document(householdId)
                .collection("tasks")
                .document(task.getId())
                .update("completed", completed);
    }

    private void deleteTask(HouseholdTask task) {
        if (householdId == null || task.getId() == null) return;
        db.collection("households").document(householdId)
                .collection("tasks")
                .document(task.getId())
                .delete();
    }

    private void updateEmptyState() {
        if (tasksList.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            tasksRecycler.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            tasksRecycler.setVisibility(View.VISIBLE);
        }
    }

    private void showError(String message) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) listener.remove();
    }
}

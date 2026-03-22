package com.example.goodstart;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goodstart.adapters.ShoppingAdapter;
import com.example.goodstart.models.ShoppingItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ShoppingFragment extends Fragment {

    private static final String ARG_HOUSEHOLD_ID = "householdId";

    private String householdId;
    private FirebaseFirestore db;
    private String currentUid;
    private String currentUserName;
    private String selectedCategory = ShoppingItem.CAT_SUPERMARKET;

    private RecyclerView shoppingRecycler;
    private EditText itemInput;
    private ImageView addButton;
    private TextView emptyText;
    private TextView clearCheckedBtn;
    private TabLayout tabLayout;
    private FloatingActionButton addFab;
    private ShoppingAdapter adapter;
    private List<ShoppingItem> allItems = new ArrayList<>();
    private List<ShoppingItem> filteredItems = new ArrayList<>();
    private ListenerRegistration listener;
    private String currentCategory = "all";

    private static final String[] CATEGORIES = {
            "all", ShoppingItem.CAT_SUPERMARKET, ShoppingItem.CAT_ELECTRONICS,
            ShoppingItem.CAT_PHARMACY, ShoppingItem.CAT_CLOTHING,
            ShoppingItem.CAT_HOME, ShoppingItem.CAT_OTHER
    };

    public static ShoppingFragment newInstance(String householdId) {
        ShoppingFragment fragment = new ShoppingFragment();
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
        View view = inflater.inflate(R.layout.fragment_shopping, container, false);

        shoppingRecycler = view.findViewById(R.id.shoppingRecycler);
        itemInput = view.findViewById(R.id.itemInput);
        addButton = view.findViewById(R.id.addButton);
        emptyText = view.findViewById(R.id.emptyText);
        clearCheckedBtn = view.findViewById(R.id.clearCheckedBtn);
        tabLayout = view.findViewById(R.id.tabLayout);
        addFab = view.findViewById(R.id.addFab);

        adapter = new ShoppingAdapter(filteredItems, new ShoppingAdapter.OnItemActionListener() {
            @Override
            public void onCheckedChanged(ShoppingItem item, boolean checked) {
                toggleItemChecked(item, checked);
            }

            @Override
            public void onDelete(ShoppingItem item) {
                deleteItem(item);
            }
        });

        shoppingRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        shoppingRecycler.setAdapter(adapter);

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) currentUserName = doc.getString("name");
                });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentCategory = CATEGORIES[tab.getPosition()];
                filterAndDisplay();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        addButton.setOnClickListener(v -> showAddItemDialog());
        if (addFab != null) addFab.setOnClickListener(v -> showAddItemDialog());
        clearCheckedBtn.setOnClickListener(v -> clearCheckedItems());

        itemInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                quickAddItem();
                return true;
            }
            return false;
        });

        loadItems();
        return view;
    }

    private void loadItems() {
        if (householdId == null) return;

        listener = db.collection("households").document(householdId)
                .collection("shoppingItems")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || !isAdded()) return;

                    allItems.clear();
                    boolean hasChecked = false;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        ShoppingItem item = doc.toObject(ShoppingItem.class);
                        item.setId(doc.getId());
                        allItems.add(item);
                        if (item.isChecked()) hasChecked = true;
                    }
                    filterAndDisplay();
                    clearCheckedBtn.setVisibility(hasChecked ? View.VISIBLE : View.GONE);
                });
    }

    private void filterAndDisplay() {
        filteredItems.clear();
        for (ShoppingItem item : allItems) {
            if (currentCategory.equals("all") || item.getCategory().equals(currentCategory)) {
                filteredItems.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void showAddItemDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_shopping, null);

        EditText nameInput = dialogView.findViewById(R.id.itemNameInput);
        RadioGroup categoryGroup = dialogView.findViewById(R.id.categoryGroup);

        new AlertDialog.Builder(requireContext())
                .setTitle("הוסף פריט")
                .setView(dialogView)
                .setPositiveButton(getString(R.string.save), (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) return;

                    String cat = ShoppingItem.CAT_SUPERMARKET;
                    int checkedId = categoryGroup.getCheckedRadioButtonId();
                    if (checkedId == R.id.catElectronics) cat = ShoppingItem.CAT_ELECTRONICS;
                    else if (checkedId == R.id.catPharmacy) cat = ShoppingItem.CAT_PHARMACY;
                    else if (checkedId == R.id.catClothing) cat = ShoppingItem.CAT_CLOTHING;
                    else if (checkedId == R.id.catHome) cat = ShoppingItem.CAT_HOME;
                    else if (checkedId == R.id.catOther) cat = ShoppingItem.CAT_OTHER;

                    addItemToList(name, cat);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void quickAddItem() {
        String name = itemInput.getText().toString().trim();
        if (!name.isEmpty()) {
            addItemToList(name, currentCategory.equals("all") ? ShoppingItem.CAT_SUPERMARKET : currentCategory);
            itemInput.setText("");
        }
    }

    private void addItemToList(String name, String category) {
        if (householdId == null) return;

        for (ShoppingItem existing : allItems) {
            if (existing.getName().equalsIgnoreCase(name) && existing.getCategory().equals(category)) {
                Toast.makeText(getContext(), "הפריט כבר קיים", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        ShoppingItem item = new ShoppingItem(name, currentUid,
                currentUserName != null ? currentUserName : "", category);

        db.collection("households").document(householdId)
                .collection("shoppingItems")
                .add(item);
    }

    private void toggleItemChecked(ShoppingItem item, boolean checked) {
        if (householdId == null || item.getId() == null) return;
        db.collection("households").document(householdId)
                .collection("shoppingItems")
                .document(item.getId())
                .update("checked", checked);
    }

    private void deleteItem(ShoppingItem item) {
        if (householdId == null || item.getId() == null) return;
        db.collection("households").document(householdId)
                .collection("shoppingItems")
                .document(item.getId())
                .delete();
    }

    private void clearCheckedItems() {
        if (householdId == null) return;
        for (ShoppingItem item : allItems) {
            if (item.isChecked() && item.getId() != null) {
                db.collection("households").document(householdId)
                        .collection("shoppingItems")
                        .document(item.getId())
                        .delete();
            }
        }
    }

    private void updateEmptyState() {
        if (filteredItems.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            shoppingRecycler.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            shoppingRecycler.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) listener.remove();
    }
}

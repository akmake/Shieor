package com.example.goodstart;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class FamilyManagementFragment extends Fragment {

    private static final String ARG_HOUSEHOLD_ID = "householdId";
    private static final String ARG_USER_NAME = "userName";
    private static final String ARG_USER_UID = "userUid";

    private String householdId;
    private String userName;
    private String userUid;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView householdCodeText;
    private TextView userNameText;
    private TextView userInitialText;
    private LinearLayout membersContainer;
    private LinearLayout copyCodeBtn;
    private LinearLayout shareCodeBtn;
    private TextView logoutBtn;

    public static FamilyManagementFragment newInstance(String householdId, String userName, String userUid) {
        FamilyManagementFragment fragment = new FamilyManagementFragment();
        Bundle args = new Bundle();
        args.putString(ARG_HOUSEHOLD_ID, householdId);
        args.putString(ARG_USER_NAME, userName);
        args.putString(ARG_USER_UID, userUid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            householdId = getArguments().getString(ARG_HOUSEHOLD_ID);
            userName = getArguments().getString(ARG_USER_NAME);
            userUid = getArguments().getString(ARG_USER_UID);
        }
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_family_management, container, false);

        householdCodeText = view.findViewById(R.id.householdCodeText);
        userNameText = view.findViewById(R.id.userNameText);
        userInitialText = view.findViewById(R.id.userInitialText);
        membersContainer = view.findViewById(R.id.membersContainer);
        copyCodeBtn = view.findViewById(R.id.copyCodeBtn);
        shareCodeBtn = view.findViewById(R.id.shareCodeBtn);
        logoutBtn = view.findViewById(R.id.logoutBtn);

        if (userName != null) {
            userNameText.setText(userName);
            if (!userName.isEmpty()) {
                userInitialText.setText(userName.substring(0, 1).toUpperCase());
            }
        }

        copyCodeBtn.setOnClickListener(v -> copyCode());
        shareCodeBtn.setOnClickListener(v -> shareCode());
        logoutBtn.setOnClickListener(v -> logout());

        loadHouseholdData();

        return view;
    }

    private void loadHouseholdData() {
        if (householdId == null) return;

        db.collection("households").document(householdId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null) return;

                    String code = doc.getString("code");
                    if (householdCodeText != null && code != null) {
                        householdCodeText.setText(code);
                    }

                    List<String> memberIds = (List<String>) doc.get("members");
                    if (memberIds != null) {
                        loadMemberDetails(memberIds);
                    }
                });
    }

    private void loadMemberDetails(List<String> memberIds) {
        if (membersContainer == null) return;
        membersContainer.removeAllViews();

        for (String memberId : memberIds) {
            db.collection("users").document(memberId).get()
                    .addOnSuccessListener(userDoc -> {
                        if (!isAdded() || userDoc == null) return;

                        String name = userDoc.getString("name");
                        String email = userDoc.getString("email");
                        boolean isMe = memberId.equals(userUid);

                        View memberView = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_member, membersContainer, false);

                        TextView memberName = memberView.findViewById(R.id.memberName);
                        TextView memberEmail = memberView.findViewById(R.id.memberEmail);
                        TextView memberInitial = memberView.findViewById(R.id.memberInitial);
                        TextView meBadge = memberView.findViewById(R.id.meBadge);

                        if (name != null) {
                            memberName.setText(name);
                            memberInitial.setText(name.substring(0, 1).toUpperCase());
                        }
                        if (email != null) memberEmail.setText(email);
                        meBadge.setVisibility(isMe ? View.VISIBLE : View.GONE);

                        membersContainer.addView(memberView);
                    });
        }
    }

    private void copyCode() {
        String code = householdCodeText.getText().toString();
        if (code.isEmpty() || code.equals("------")) return;
        ClipboardManager clipboard = (ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Family Code", code);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), getString(R.string.code_copied), Toast.LENGTH_SHORT).show();
    }

    private void shareCode() {
        String code = householdCodeText.getText().toString();
        if (code.isEmpty() || code.equals("------")) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "הצטרף למשפחה שלי ב-FamilyHub! קוד: " + code);
        startActivity(Intent.createChooser(shareIntent, "שתף קוד"));
    }

    private void logout() {
        auth.signOut();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}

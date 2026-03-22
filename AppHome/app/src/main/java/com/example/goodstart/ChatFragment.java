package com.example.goodstart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.goodstart.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ChatFragment extends Fragment {

    private static final String ARG_HOUSEHOLD_ID = "householdId";

    private String householdId;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUid;

    private View familyChatCard;
    private View channelsCard;
    private TextView lastFamilyMsg;
    private TextView unreadBadge;
    private ListenerRegistration unreadListener;
    private ListenerRegistration lastMsgListener;

    public static ChatFragment newInstance(String householdId) {
        ChatFragment fragment = new ChatFragment();
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
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUid = auth.getCurrentUser().getUid();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_hub, container, false);

        familyChatCard = view.findViewById(R.id.familyChatCard);
        channelsCard = view.findViewById(R.id.channelsCard);
        lastFamilyMsg = view.findViewById(R.id.lastFamilyMsg);
        unreadBadge = view.findViewById(R.id.unreadBadge);

        familyChatCard.setOnClickListener(v -> openFamilyChat());
        channelsCard.setOnClickListener(v -> openChannels());

        loadLastMessage();
        loadUnreadCount();

        return view;
    }

    private void openFamilyChat() {
        FamilyChatFragment chatFrag = FamilyChatFragment.newInstance(householdId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, chatFrag)
                .addToBackStack(null)
                .commit();
    }

    private void openChannels() {
        ChannelsFragment channelsFrag = ChannelsFragment.newInstance(householdId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, channelsFrag)
                .addToBackStack(null)
                .commit();
    }

    private void loadLastMessage() {
        if (householdId == null) return;
        lastMsgListener = db.collection("households").document(householdId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || !isAdded() || snap == null || snap.isEmpty()) return;
                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    Message msg = doc.toObject(Message.class);
                    if (msg != null && lastFamilyMsg != null) {
                        String preview = msg.getText() != null ? msg.getText() : "הודעה קולית 🎤";
                        lastFamilyMsg.setText(msg.getSenderName() + ": " + preview);
                    }
                });
    }

    private void loadUnreadCount() {
        if (householdId == null || currentUid == null) return;
        unreadListener = db.collection("households").document(householdId)
                .collection("messages")
                .whereEqualTo("read", false)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || !isAdded() || snap == null) return;
                    int count = 0;
                    for (QueryDocumentSnapshot doc : snap) {
                        String senderId = doc.getString("senderId");
                        if (senderId != null && !senderId.equals(currentUid)) count++;
                    }
                    if (unreadBadge != null) {
                        if (count > 0) {
                            unreadBadge.setVisibility(View.VISIBLE);
                            unreadBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                        } else {
                            unreadBadge.setVisibility(View.GONE);
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (unreadListener != null) unreadListener.remove();
        if (lastMsgListener != null) lastMsgListener.remove();
    }
}

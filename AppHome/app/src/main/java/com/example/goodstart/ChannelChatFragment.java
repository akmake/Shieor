package com.example.goodstart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goodstart.adapters.ChatAdapter;
import com.example.goodstart.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChannelChatFragment extends Fragment {

    private static final String ARG_CHANNEL_ID = "channelId";
    private static final String ARG_CHANNEL_NAME = "channelName";

    private String channelId;
    private String channelName;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUid;
    private String currentUserName;

    private RecyclerView messagesRecycler;
    private EditText messageInput;
    private ImageView sendButton;
    private ImageView backButton;
    private TextView headerTitle;
    private TextView emptyText;
    private ChatAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private ListenerRegistration listener;

    public static ChannelChatFragment newInstance(String channelId, String channelName) {
        ChannelChatFragment f = new ChannelChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHANNEL_ID, channelId);
        args.putString(ARG_CHANNEL_NAME, channelName);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            channelId = getArguments().getString(ARG_CHANNEL_ID);
            channelName = getArguments().getString(ARG_CHANNEL_NAME);
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
        View view = inflater.inflate(R.layout.fragment_family_chat, container, false);

        messagesRecycler = view.findViewById(R.id.messagesRecycler);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);
        backButton = view.findViewById(R.id.backButton);
        headerTitle = view.findViewById(R.id.headerTitle);
        emptyText = view.findViewById(R.id.emptyText);

        ImageView voiceButton = view.findViewById(R.id.voiceButton);
        if (voiceButton != null) voiceButton.setVisibility(View.GONE);

        if (headerTitle != null && channelName != null) {
            headerTitle.setText("# " + channelName);
        }

        adapter = new ChatAdapter(messages, currentUid);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        lm.setStackFromEnd(true);
        messagesRecycler.setLayoutManager(lm);
        messagesRecycler.setAdapter(adapter);

        backButton.setOnClickListener(v -> {
            if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        sendButton.setOnClickListener(v -> sendMessage());

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) currentUserName = doc.getString("name");
                });

        loadMessages();

        return view;
    }

    private void loadMessages() {
        if (channelId == null) return;
        listener = db.collection("channels").document(channelId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || !isAdded()) return;
                    messages.clear();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            Message msg = doc.toObject(Message.class);
                            msg.setId(doc.getId());
                            messages.add(msg);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    if (!messages.isEmpty()) {
                        messagesRecycler.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty() || channelId == null) return;
        Message msg = new Message(currentUid,
                currentUserName != null ? currentUserName : "", text);
        db.collection("channels").document(channelId)
                .collection("messages")
                .add(msg)
                .addOnSuccessListener(d -> messageInput.setText(""));
    }

    private void updateEmptyState() {
        if (emptyText == null) return;
        if (messages.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            messagesRecycler.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            messagesRecycler.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) listener.remove();
    }
}

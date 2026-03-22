package com.example.goodstart;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goodstart.adapters.ChatAdapter;
import com.example.goodstart.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FamilyChatFragment extends Fragment {

    private static final String ARG_HOUSEHOLD_ID = "householdId";
    private static final int REQUEST_RECORD_AUDIO = 101;

    private String householdId;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUid;
    private String currentUserName;

    private RecyclerView messagesRecycler;
    private EditText messageInput;
    private ImageView sendButton;
    private ImageView voiceButton;
    private ImageView backButton;
    private TextView headerTitle;
    private TextView headerSubtitle;
    private TextView emptyText;
    private ChatAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private ListenerRegistration listener;

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String recordingPath;

    public static FamilyChatFragment newInstance(String householdId) {
        FamilyChatFragment fragment = new FamilyChatFragment();
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
        View view = inflater.inflate(R.layout.fragment_family_chat, container, false);

        messagesRecycler = view.findViewById(R.id.messagesRecycler);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);
        voiceButton = view.findViewById(R.id.voiceButton);
        backButton = view.findViewById(R.id.backButton);
        headerTitle = view.findViewById(R.id.headerTitle);
        headerSubtitle = view.findViewById(R.id.headerSubtitle);
        emptyText = view.findViewById(R.id.emptyText);

        headerTitle.setText(getString(R.string.family_chat));

        adapter = new ChatAdapter(messages, currentUid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        messagesRecycler.setLayoutManager(layoutManager);
        messagesRecycler.setAdapter(adapter);

        backButton.setOnClickListener(v -> {
            if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        sendButton.setOnClickListener(v -> sendMessage());

        voiceButton.setOnLongClickListener(v -> {
            startRecording();
            return true;
        });
        voiceButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecordingAndSend();
            }
        });

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) currentUserName = doc.getString("name");
                });

        loadMembersCount();
        loadMessages();

        return view;
    }

    private void loadMembersCount() {
        if (householdId == null) return;
        db.collection("households").document(householdId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && headerSubtitle != null) {
                        List<String> members = (List<String>) doc.get("members");
                        int count = members != null ? members.size() : 0;
                        headerSubtitle.setText(getString(R.string.members_count, count));
                    }
                });
    }

    private void loadMessages() {
        if (householdId == null) return;

        listener = db.collection("households").document(householdId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || !isAdded()) return;

                    messages.clear();
                    for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            String senderId = dc.getDocument().getString("senderId");
                            Boolean read = dc.getDocument().getBoolean("read");
                            if (senderId != null && !senderId.equals(currentUid) &&
                                    (read == null || !read)) {
                                dc.getDocument().getReference().update("read", true);
                            }
                        }
                    }

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        Message msg = doc.toObject(Message.class);
                        msg.setId(doc.getId());
                        messages.add(msg);
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
        if (text.isEmpty() || householdId == null) return;

        Message message = new Message(currentUid,
                currentUserName != null ? currentUserName : "", text);

        db.collection("households").document(householdId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(docRef -> messageInput.setText(""));
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }

        try {
            recordingPath = requireContext().getCacheDir().getAbsolutePath() + "/voice_msg.3gp";
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(recordingPath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            voiceButton.setImageResource(R.drawable.ic_send);
            Toast.makeText(getContext(), "מקליט... לחץ לשליחה", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "שגיאה בהקלטה", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecordingAndSend() {
        if (!isRecording || mediaRecorder == null) return;
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            voiceButton.setImageResource(R.drawable.ic_voice);

            // Create voice message entry (without Firebase Storage for now)
            Message voiceMsg = new Message(currentUid,
                    currentUserName != null ? currentUserName : "", "🎤 הודעה קולית");
            voiceMsg.setType(Message.TYPE_VOICE);

            db.collection("households").document(householdId)
                    .collection("messages")
                    .add(voiceMsg);
        } catch (Exception e) {
            Toast.makeText(getContext(), "שגיאה בשמירת ההקלטה", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateEmptyState() {
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
        if (mediaRecorder != null && isRecording) {
            mediaRecorder.stop();
            mediaRecorder.release();
        }
    }
}

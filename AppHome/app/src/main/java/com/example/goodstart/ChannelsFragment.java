package com.example.goodstart;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goodstart.adapters.ChannelAdapter;
import com.example.goodstart.models.Channel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ChannelsFragment extends Fragment {

    private static final String ARG_HOUSEHOLD_ID = "householdId";

    private String householdId;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUid;
    private String currentUserName;

    private RecyclerView channelsRecycler;
    private TextView emptyText;
    private ImageView backButton;
    private FloatingActionButton addChannelFab;
    private ChannelAdapter adapter;
    private List<Channel> channels = new ArrayList<>();
    private ListenerRegistration listener;

    public static ChannelsFragment newInstance(String householdId) {
        ChannelsFragment fragment = new ChannelsFragment();
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
        View view = inflater.inflate(R.layout.fragment_channels, container, false);

        channelsRecycler = view.findViewById(R.id.channelsRecycler);
        emptyText = view.findViewById(R.id.emptyText);
        backButton = view.findViewById(R.id.backButton);
        addChannelFab = view.findViewById(R.id.addChannelFab);

        adapter = new ChannelAdapter(channels, channel -> openChannelChat(channel));
        channelsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        channelsRecycler.setAdapter(adapter);

        backButton.setOnClickListener(v -> {
            if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        addChannelFab.setOnClickListener(v -> showCreateChannelDialog());

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) currentUserName = doc.getString("name");
                });

        loadChannels();

        return view;
    }

    private void loadChannels() {
        listener = db.collection("channels")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || !isAdded()) return;
                    channels.clear();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            Channel channel = doc.toObject(Channel.class);
                            channel.setId(doc.getId());
                            channels.add(channel);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    emptyText.setVisibility(channels.isEmpty() ? View.VISIBLE : View.GONE);
                    channelsRecycler.setVisibility(channels.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void openChannelChat(Channel channel) {
        ChannelChatFragment frag = ChannelChatFragment.newInstance(channel.getId(), channel.getName());
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, frag)
                .addToBackStack(null)
                .commit();
    }

    private void showCreateChannelDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_create_channel, null);

        EditText nameInput = dialogView.findViewById(R.id.channelNameInput);
        EditText descInput = dialogView.findViewById(R.id.channelDescInput);

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.new_channel))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.save), (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String desc = descInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "יש להזין שם לערוץ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createChannel(name, desc);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void createChannel(String name, String desc) {
        String inviteCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Channel channel = new Channel(name, desc, currentUid,
                currentUserName != null ? currentUserName : "");
        channel.setInviteLink("familyhub://channel/" + inviteCode);
        channel.setMembers(Arrays.asList(currentUid));
        channel.setMembersCount(1);

        db.collection("channels").add(channel)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "הערוץ נוצר!", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) listener.remove();
    }
}

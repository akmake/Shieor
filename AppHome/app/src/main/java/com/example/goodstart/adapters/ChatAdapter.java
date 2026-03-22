package com.example.goodstart.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goodstart.R;
import com.example.goodstart.models.Message;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 0;
    private static final int TYPE_RECEIVED = 1;

    private List<Message> messages;
    private String currentUid;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("d/M", Locale.getDefault());

    public ChatAdapter(List<Message> messages, String currentUid) {
        this.messages = messages;
        this.currentUid = currentUid;
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        if (msg.getSenderId() != null && msg.getSenderId().equals(currentUid)) {
            return TYPE_SENT;
        }
        return TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);

        if (holder instanceof SentViewHolder) {
            SentViewHolder sentHolder = (SentViewHolder) holder;
            String displayText = msg.getText();
            if (Message.TYPE_VOICE.equals(msg.getType())) {
                displayText = "🎤 הודעה קולית";
            } else if (Message.TYPE_IMAGE.equals(msg.getType())) {
                displayText = "📷 תמונה";
            }
            sentHolder.messageText.setText(displayText != null ? displayText : "");
            if (msg.getTimestamp() != null) {
                sentHolder.timeText.setText(formatTime(msg.getTimestamp()));
            }
            sentHolder.readIndicator.setText(msg.isRead() ? "✓✓" : "✓");
            sentHolder.readIndicator.setAlpha(msg.isRead() ? 1.0f : 0.7f);
        } else {
            ReceivedViewHolder recvHolder = (ReceivedViewHolder) holder;
            String displayText = msg.getText();
            if (Message.TYPE_VOICE.equals(msg.getType())) {
                displayText = "🎤 הודעה קולית";
            } else if (Message.TYPE_IMAGE.equals(msg.getType())) {
                displayText = "📷 תמונה";
            }
            recvHolder.messageText.setText(displayText != null ? displayText : "");
            recvHolder.senderName.setText(msg.getSenderName() != null ? msg.getSenderName() : "");
            if (msg.getTimestamp() != null) {
                recvHolder.timeText.setText(formatTime(msg.getTimestamp()));
            }

            // Avatar initial
            String name = msg.getSenderName();
            if (name != null && !name.isEmpty()) {
                recvHolder.avatarText.setText(name.substring(0, 1).toUpperCase());
            }
        }
    }

    private String formatTime(Date date) {
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTime(date);
        Calendar todayCal = Calendar.getInstance();

        if (msgCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                msgCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
            return timeFormat.format(date);
        } else {
            return dateFormat.format(date);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, readIndicator;

        SentViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
            readIndicator = itemView.findViewById(R.id.readIndicator);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, senderName, avatarText;

        ReceivedViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
            senderName = itemView.findViewById(R.id.senderName);
            avatarText = itemView.findViewById(R.id.avatarText);
        }
    }
}

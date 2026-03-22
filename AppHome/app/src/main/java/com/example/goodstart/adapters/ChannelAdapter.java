package com.example.goodstart.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goodstart.R;
import com.example.goodstart.models.Channel;

import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {

    public interface OnChannelClickListener {
        void onChannelClick(Channel channel);
    }

    private List<Channel> channels;
    private OnChannelClickListener listener;

    public ChannelAdapter(List<Channel> channels, OnChannelClickListener listener) {
        this.channels = channels;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Channel channel = channels.get(position);
        holder.channelName.setText("# " + channel.getName());
        holder.channelDesc.setText(channel.getDescription() != null ? channel.getDescription() : "");
        holder.membersCount.setText(channel.getMembersCount() + " משתתפים");
        holder.avatarText.setText(channel.getName().substring(0, 1).toUpperCase());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onChannelClick(channel);
        });
    }

    @Override
    public int getItemCount() { return channels.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView channelName, channelDesc, membersCount, avatarText;

        ViewHolder(View itemView) {
            super(itemView);
            channelName = itemView.findViewById(R.id.channelName);
            channelDesc = itemView.findViewById(R.id.channelDesc);
            membersCount = itemView.findViewById(R.id.membersCount);
            avatarText = itemView.findViewById(R.id.avatarText);
        }
    }
}

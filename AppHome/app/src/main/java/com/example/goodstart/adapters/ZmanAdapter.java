package com.example.goodstart.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goodstart.R;
import com.example.goodstart.models.ZmanItem;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ZmanAdapter extends RecyclerView.Adapter<ZmanAdapter.ViewHolder> {

    public interface OnZmanActionListener {
        void onAlarmToggle(ZmanItem item);
    }

    private List<ZmanItem> items;
    private OnZmanActionListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ZmanAdapter(List<ZmanItem> items, OnZmanActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_zman, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ZmanItem item = items.get(position);

        holder.zmanIcon.setText(item.getIcon());
        holder.zmanLabel.setText(item.getLabel());
        holder.zmanSublabel.setText(item.getSublabel());
        holder.zmanTime.setText(timeFormat.format(item.getTime()));

        // Update status/countdown
        long diff = item.getTime().getTime() - System.currentTimeMillis();
        if (diff < 0) {
            holder.zmanTime.setTextColor(Color.GRAY);
            holder.zmanCountdown.setText("עבר");
            holder.zmanCountdown.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.zmanTime.setTextColor(holder.itemView.getContext().getColor(R.color.primary));
            long minutes = diff / (1000 * 60);
            if (minutes < 60) {
                holder.zmanCountdown.setText("בעוד " + minutes + " דק'");
            } else {
                holder.zmanCountdown.setText("בעוד " + (minutes / 60) + " שעות");
            }
            holder.zmanCountdown.setVisibility(View.VISIBLE);
            holder.zmanCountdown.setTextColor(holder.itemView.getContext().getColor(R.color.system_orange));
            holder.itemView.setAlpha(1.0f);
        }

        holder.alarmToggle.setImageResource(item.isAlarmEnabled() ? 
                R.drawable.ic_notifications_active : R.drawable.ic_notifications_none);
        holder.alarmToggle.setColorFilter(item.isAlarmEnabled() ? 
                holder.itemView.getContext().getColor(R.color.primary) : 
                holder.itemView.getContext().getColor(R.color.text_tertiary));

        holder.alarmToggle.setOnClickListener(v -> {
            item.setAlarmEnabled(!item.isAlarmEnabled());
            notifyItemChanged(position);
            if (listener != null) listener.onAlarmToggle(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView zmanIcon, zmanLabel, zmanSublabel, zmanTime, zmanCountdown;
        ImageView alarmToggle;

        ViewHolder(View itemView) {
            super(itemView);
            zmanIcon = itemView.findViewById(R.id.zmanIcon);
            zmanLabel = itemView.findViewById(R.id.zmanLabel);
            zmanSublabel = itemView.findViewById(R.id.zmanSublabel);
            zmanTime = itemView.findViewById(R.id.zmanTime);
            zmanCountdown = itemView.findViewById(R.id.zmanCountdown);
            alarmToggle = itemView.findViewById(R.id.alarmToggle);
        }
    }
}

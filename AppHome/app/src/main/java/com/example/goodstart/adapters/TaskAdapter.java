package com.example.goodstart.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goodstart.R;
import com.example.goodstart.models.HouseholdTask;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    public interface OnTaskActionListener {
        void onToggleCompleted(HouseholdTask task, boolean completed);
        void onDelete(HouseholdTask task);
    }

    private List<HouseholdTask> tasks;
    private String currentUid;
    private OnTaskActionListener listener;

    public TaskAdapter(List<HouseholdTask> tasks, String currentUid, OnTaskActionListener listener) {
        this.tasks = tasks;
        this.currentUid = currentUid;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HouseholdTask task = tasks.get(position);

        holder.taskTitle.setText(task.getTitle());
        
        // Time/Status text
        String statusText = task.isCompleted() ? "בוצע" : "פתוח";
        holder.taskTime.setText(statusText);

        // Priority indicator
        if (holder.priorityIndicator != null) {
            holder.priorityIndicator.setVisibility(task.isUrgent() ? View.VISIBLE : View.GONE);
        }

        // Checkbox
        holder.taskCheckbox.setOnCheckedChangeListener(null);
        holder.taskCheckbox.setChecked(task.isCompleted());

        // Style based on completion
        if (task.isCompleted()) {
            holder.taskTitle.setPaintFlags(
                    holder.taskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.taskTitle.setPaintFlags(
                    holder.taskTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.itemView.setAlpha(1.0f);
        }

        holder.taskCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onToggleCompleted(task, isChecked);
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onDelete(task);
            return true;
        });
    }

    @Override
    public int getItemCount() { return tasks.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox taskCheckbox;
        TextView taskTitle, taskTime;
        View priorityIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            taskCheckbox = itemView.findViewById(R.id.taskCheckbox);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskTime = itemView.findViewById(R.id.taskTime);
            priorityIndicator = itemView.findViewById(R.id.priorityIndicator);
        }
    }
}

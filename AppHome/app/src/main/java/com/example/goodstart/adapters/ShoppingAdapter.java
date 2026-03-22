package com.example.goodstart.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goodstart.R;
import com.example.goodstart.models.ShoppingItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingAdapter extends RecyclerView.Adapter<ShoppingAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onCheckedChanged(ShoppingItem item, boolean checked);
        void onDelete(ShoppingItem item);
    }

    private static final Map<String, String> CATEGORY_EMOJI = new HashMap<>();
    private static final Map<String, Integer> CATEGORY_COLOR = new HashMap<>();

    static {
        CATEGORY_EMOJI.put(ShoppingItem.CAT_SUPERMARKET, "🛒");
        CATEGORY_EMOJI.put(ShoppingItem.CAT_ELECTRONICS, "⚡");
        CATEGORY_EMOJI.put(ShoppingItem.CAT_PHARMACY, "💊");
        CATEGORY_EMOJI.put(ShoppingItem.CAT_CLOTHING, "👕");
        CATEGORY_EMOJI.put(ShoppingItem.CAT_HOME, "🏠");
        CATEGORY_EMOJI.put(ShoppingItem.CAT_OTHER, "📦");

        CATEGORY_COLOR.put(ShoppingItem.CAT_SUPERMARKET, R.color.cat_supermarket);
        CATEGORY_COLOR.put(ShoppingItem.CAT_ELECTRONICS, R.color.cat_electronics);
        CATEGORY_COLOR.put(ShoppingItem.CAT_PHARMACY, R.color.cat_pharmacy);
        CATEGORY_COLOR.put(ShoppingItem.CAT_CLOTHING, R.color.cat_clothing);
        CATEGORY_COLOR.put(ShoppingItem.CAT_HOME, R.color.cat_home);
        CATEGORY_COLOR.put(ShoppingItem.CAT_OTHER, R.color.cat_other);
    }

    private List<ShoppingItem> items;
    private OnItemActionListener listener;

    public ShoppingAdapter(List<ShoppingItem> items, OnItemActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shopping, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingItem item = items.get(position);

        holder.itemName.setText(item.getName());
        holder.addedBy.setText(item.getAddedByName());

        // Category emoji
        String emoji = CATEGORY_EMOJI.get(item.getCategory());
        if (holder.categoryEmoji != null) {
            holder.categoryEmoji.setText(emoji != null ? emoji : "📦");
        }

        // Category color indicator
        if (holder.categoryIndicator != null) {
            Integer colorRes = CATEGORY_COLOR.get(item.getCategory());
            if (colorRes != null) {
                holder.categoryIndicator.setBackgroundColor(
                        ContextCompat.getColor(holder.itemView.getContext(), colorRes));
            }
        }

        holder.itemCheckbox.setOnCheckedChangeListener(null);
        holder.itemCheckbox.setChecked(item.isChecked());

        if (item.isChecked()) {
            holder.itemName.setPaintFlags(
                    holder.itemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.itemName.setAlpha(0.5f);
        } else {
            holder.itemName.setPaintFlags(
                    holder.itemName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.itemName.setAlpha(1.0f);
        }

        holder.itemCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onCheckedChanged(item, isChecked);
        });

        holder.deleteBtn.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox itemCheckbox;
        TextView itemName, addedBy, categoryEmoji;
        ImageView deleteBtn, categoryIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            itemCheckbox = itemView.findViewById(R.id.itemCheckbox);
            itemName = itemView.findViewById(R.id.itemName);
            addedBy = itemView.findViewById(R.id.addedBy);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
            categoryEmoji = itemView.findViewById(R.id.categoryEmoji);
            categoryIndicator = itemView.findViewById(R.id.categoryIndicator);
        }
    }
}

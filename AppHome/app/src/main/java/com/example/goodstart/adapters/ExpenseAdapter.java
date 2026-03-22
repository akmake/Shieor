package com.example.goodstart.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goodstart.R;
import com.example.goodstart.models.Expense;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    private List<Expense> expenses;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

    private static final Map<String, Integer> CATEGORY_COLORS = new HashMap<>();
    private static final Map<String, String> CATEGORY_NAMES_HE = new HashMap<>();

    static {
        CATEGORY_COLORS.put("food", R.color.expense_food);
        CATEGORY_COLORS.put("transport", R.color.expense_transport);
        CATEGORY_COLORS.put("entertainment", R.color.expense_entertainment);
        CATEGORY_COLORS.put("bills", R.color.expense_bills);
        CATEGORY_COLORS.put("shopping", R.color.expense_shopping);
        CATEGORY_COLORS.put("health", R.color.expense_health);
        CATEGORY_COLORS.put("education", R.color.expense_education);
        CATEGORY_COLORS.put("other", R.color.expense_other);

        CATEGORY_NAMES_HE.put("food", "🍎 מזון");
        CATEGORY_NAMES_HE.put("transport", "🚗 תחבורה");
        CATEGORY_NAMES_HE.put("entertainment", "🎬 בילויים");
        CATEGORY_NAMES_HE.put("bills", "📄 חשבונות");
        CATEGORY_NAMES_HE.put("shopping", "🛍️ קניות");
        CATEGORY_NAMES_HE.put("health", "💊 בריאות");
        CATEGORY_NAMES_HE.put("education", "📚 חינוך");
        CATEGORY_NAMES_HE.put("other", "📌 אחר");
    }

    public ExpenseAdapter(List<Expense> expenses, Context context) {
        this.expenses = expenses;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenses.get(position);

        holder.expenseTitle.setText(expense.getTitle());
        holder.expenseAmount.setText("₪" + String.format("%.0f", expense.getAmount()));

        // Category
        String catName = CATEGORY_NAMES_HE.get(expense.getCategory());
        holder.expenseCategory.setText(catName != null ? catName : expense.getCategory());

        // Category color
        Integer colorRes = CATEGORY_COLORS.get(expense.getCategory());
        if (colorRes != null) {
            holder.categoryColor.setBackgroundColor(
                    context.getResources().getColor(colorRes));
        }

        // Date
        if (expense.getTimestamp() != null) {
            holder.expenseDate.setText(dateFormat.format(expense.getTimestamp()));
        }
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View categoryColor;
        TextView expenseTitle, expenseCategory, expenseDate, expenseAmount;

        ViewHolder(View itemView) {
            super(itemView);
            categoryColor = itemView.findViewById(R.id.categoryColor);
            expenseTitle = itemView.findViewById(R.id.expenseTitle);
            expenseCategory = itemView.findViewById(R.id.expenseCategory);
            expenseDate = itemView.findViewById(R.id.expenseDate);
            expenseAmount = itemView.findViewById(R.id.expenseAmount);
        }
    }
}

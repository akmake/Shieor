package com.example.goodstart.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goodstart.R;
import com.example.goodstart.api.RetrofitClient;
import com.example.goodstart.api.SefariaService;
import android.os.Build;
import android.text.Layout;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RambamPagerAdapter extends RecyclerView.Adapter<RambamPagerAdapter.ViewHolder> {

    private static final int TOTAL_DAYS = 334;

    // Hebrew ordinals 1-30
    private static final String[] HEBREW_ORDINALS = {
        "", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט", "י",
        "יא", "יב", "יג", "יד", "טו", "טז", "יז", "יח", "יט", "כ",
        "כא", "כב", "כג", "כד", "כה", "כו", "כז", "כח", "כט", "ל"
    };

    private final int todayPosition;
    private boolean isAutoScrolling = false;
    private final Handler scrollHandler = new Handler(Looper.getMainLooper());
    private RecyclerView attachedRecyclerView;

    public RambamPagerAdapter() {
        Calendar cycleStart = Calendar.getInstance();
        cycleStart.set(2024, Calendar.JULY, 14, 0, 0, 0);
        cycleStart.set(Calendar.MILLISECOND, 0);

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        long diffMs = today.getTimeInMillis() - cycleStart.getTimeInMillis();
        int daysSinceStart = (int) (diffMs / (1000L * 60 * 60 * 24));
        this.todayPosition = Math.max(0, daysSinceStart % TOTAL_DAYS);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.attachedRecyclerView = recyclerView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rambam_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(position, todayPosition);
    }

    @Override
    public int getItemCount() {
        return TOTAL_DAYS;
    }

    public int getTodayPosition() {
        return todayPosition;
    }

    public void setAutoScroll(boolean active) {
        this.isAutoScrolling = active;
        if (active) runAutoScroll();
        else scrollHandler.removeCallbacksAndMessages(null);
    }

    private void runAutoScroll() {
        if (!isAutoScrolling || attachedRecyclerView == null) return;
        View currentView = attachedRecyclerView.getChildAt(0);
        if (currentView != null) {
            NestedScrollView sv = currentView.findViewById(R.id.rambamScrollView);
            if (sv != null) {
                int speed = currentView.getContext()
                        .getSharedPreferences("RambamPrefs", Context.MODE_PRIVATE)
                        .getInt("auto_scroll_speed", 3) + 1;
                sv.smoothScrollBy(0, speed);
            }
        }
        scrollHandler.postDelayed(this::runAutoScroll, 50);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Flatten a 1-D or 2-D Sefaria JSON text array into a list of chapters.
     * Filters out empty strings so the UI never shows blank halakhot.
     */
    static List<List<String>> extractChapters(JsonElement heRaw) {
        List<List<String>> result = new ArrayList<>();
        if (heRaw == null || !heRaw.isJsonArray()) return result;
        JsonArray top = heRaw.getAsJsonArray();
        if (top.size() == 0) return result;

        if (top.get(0).isJsonArray()) {
            // 2-D: [[halacha…], [halacha…], …]
            for (JsonElement chElem : top) {
                List<String> ch = new ArrayList<>();
                if (chElem.isJsonArray()) {
                    for (JsonElement h : chElem.getAsJsonArray()) {
                        if (h.isJsonPrimitive()) {
                            String s = stripHtml(h.getAsString());
                            if (!s.isEmpty()) ch.add(s);
                        }
                    }
                }
                if (!ch.isEmpty()) result.add(ch);
            }
        } else {
            // 1-D: [halacha, halacha, …]
            List<String> ch = new ArrayList<>();
            for (JsonElement h : top) {
                if (h.isJsonPrimitive()) {
                    String s = stripHtml(h.getAsString());
                    if (!s.isEmpty()) ch.add(s);
                }
            }
            if (!ch.isEmpty()) result.add(ch);
        }
        return result;
    }

    private static String stripHtml(String s) {
        if (s == null) return "";
        return Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString().trim();
    }

    // ─── ViewHolder ─────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, chaptersText, errorText;
        NestedScrollView scrollView;
        ProgressBar loader;
        View errorLayout, retryBtn;
        LinearLayout contentContainer;

        Call<JsonObject> calendarCall;
        Call<SefariaService.SefariaResponse> textCall;

        ViewHolder(View itemView) {
            super(itemView);
            titleText    = itemView.findViewById(R.id.rambamTitle);
            chaptersText = itemView.findViewById(R.id.rambamChapters);
            scrollView   = itemView.findViewById(R.id.rambamScrollView);
            loader       = itemView.findViewById(R.id.rambamLoader);
            errorLayout  = itemView.findViewById(R.id.errorLayout);
            errorText    = itemView.findViewById(R.id.errorText);
            retryBtn     = itemView.findViewById(R.id.retryBtn);
            contentContainer = itemView.findViewById(R.id.rambamContentContainer);
        }

        void bind(int position, int todayPos) {
            if (calendarCall != null) { calendarCall.cancel(); calendarCall = null; }
            if (textCall     != null) { textCall.cancel();     textCall     = null; }

            Context ctx = itemView.getContext();
            SharedPreferences prefs = ctx.getSharedPreferences("RambamPrefs", Context.MODE_PRIVATE);
            String scrollKey = "scroll_pos_day_" + position;

            // Reset UI
            loader.setVisibility(View.VISIBLE);
            errorLayout.setVisibility(View.GONE);
            contentContainer.removeAllViews();
            titleText.setText("טוען הלכות...");
            chaptersText.setText("יום " + (position + 1));
            scrollView.scrollTo(0, 0);

            retryBtn.setOnClickListener(v -> bind(position, todayPos));

            // Compute the Gregorian date for this ViewPager page
            int dayOffset = position - todayPos;
            Calendar targetCal = Calendar.getInstance();
            targetCal.add(Calendar.DAY_OF_YEAR, dayOffset);
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .format(targetCal.getTime());

            calendarCall = RetrofitClient.getSefariaService().getCalendars(dateStr);
            calendarCall.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (call.isCanceled()) return;
                    try {
                        if (!response.isSuccessful() || response.body() == null) {
                            showError("שגיאה בטעינת הלוח"); return;
                        }
                        JsonArray items = response.body().getAsJsonArray("calendar_items");
                        if (items == null || items.size() == 0) {
                            showError("לא נמצא שיעור להיום"); return;
                        }

                        // Two-pass selection: prefer the 3-chapter Rambam track (ג' פרקים).
                        // Pass 0 – must contain "3" (or "three") in the English title.
                        // Pass 1 – fall back to any item labelled "rambam".
                        String foundUrl = null, foundDisplay = "";
                        outer:
                        for (int pass = 0; pass < 2; pass++) {
                            for (JsonElement el : items) {
                                if (!el.isJsonObject()) continue;
                                JsonObject item = el.getAsJsonObject();

                                String titleEn = getNestedStr(item, "title", "en");
                                if (titleEn == null) titleEn = getJsonStr(item, "title");
                                if (titleEn == null) continue;
                                String low = titleEn.toLowerCase(Locale.US);

                                if (!low.contains("rambam")) continue;
                                if (pass == 0 && !low.contains("3") && !low.contains("three")) continue;

                                String url = getJsonStr(item, "url");
                                if (url == null) url = getJsonStr(item, "ref");
                                if (url == null || url.isEmpty()) continue;
                                if (url.startsWith("/")) url = url.substring(1);

                                String heDisplay = getNestedStr(item, "displayValue", "he");
                                foundUrl     = url;
                                foundDisplay = heDisplay != null ? heDisplay : "";
                                break outer;
                            }
                        }

                        if (foundUrl != null) {
                            fetchText(foundUrl, foundDisplay, position, prefs, scrollKey);
                        } else {
                            showError("לא נמצא שיעור להיום");
                        }
                    } catch (Exception e) {
                        if (!call.isCanceled()) showError("שגיאה בפענוח הנתונים");
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    if (!call.isCanceled()) showError("אין חיבור לאינטרנט");
                }
            });
        }

        private void fetchText(String ref, String heDisplayTitle, int position,
                               SharedPreferences prefs, String scrollKey) {
            textCall = RetrofitClient.getSefariaService().getRambamChapter(ref);
            textCall.enqueue(new Callback<SefariaService.SefariaResponse>() {
                @Override
                public void onResponse(Call<SefariaService.SefariaResponse> call,
                                       Response<SefariaService.SefariaResponse> response) {
                    if (call.isCanceled()) return;
                    loader.setVisibility(View.GONE);
                    if (!response.isSuccessful() || response.body() == null) {
                        showError("שגיאה בקבלת התוכן"); return;
                    }

                    String heTitle = response.body().heTitle;
                    String title = (heTitle != null && !heTitle.isEmpty())
                            ? heTitle.replace("משנה תורה, ", "")
                            : (!heDisplayTitle.isEmpty() ? heDisplayTitle : "רמב\"ם");
                    titleText.setText(title);
                    chaptersText.setText(!heDisplayTitle.isEmpty()
                            ? heDisplayTitle : ("יום " + (position + 1)));

                    List<List<String>> chapters = extractChapters(response.body().heRaw);
                    if (chapters.isEmpty()) {
                        showError("לא נמצא תוכן להיום"); return;
                    }

                    Context ctx = itemView.getContext();
                    int textSizeSp = ctx.getSharedPreferences("RambamPrefs", Context.MODE_PRIVATE)
                            .getInt("text_size_sp", 20);
                    buildContent(ctx, chapters, textSizeSp);

                    int savedY = prefs.getInt(scrollKey, 0);
                    scrollView.post(() -> scrollView.scrollTo(0, savedY));
                    scrollView.setOnScrollChangeListener(
                            (NestedScrollView.OnScrollChangeListener)
                                    (v, sX, sY, oX, oY) ->
                                            prefs.edit().putInt(scrollKey, sY).apply());
                }

                @Override
                public void onFailure(Call<SefariaService.SefariaResponse> call, Throwable t) {
                    if (!call.isCanceled()) showError("שגיאה בחיבור לשרת");
                }
            });
        }

        private void showError(String msg) {
            loader.setVisibility(View.GONE);
            errorLayout.setVisibility(View.VISIBLE);
            errorText.setText(msg);
            titleText.setText("שגיאה");
        }

        // ── Content builder ─────────────────────────────────────────────────────

        private void buildContent(Context ctx, List<List<String>> chapters, int textSizeSp) {
            contentContainer.removeAllViews();
            int teal = ctx.getColor(R.color.rambam_primary);
            Typeface font = loadFont(ctx);

            for (int ci = 0; ci < chapters.size(); ci++) {
                // Chapter header – shown only when multiple chapters are present
                if (chapters.size() > 1) {
                    if (ci > 0) {
                        View sep = new View(ctx);
                        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 1));
                        sp.setMargins(0, dp(ctx, 28), 0, dp(ctx, 20));
                        sep.setLayoutParams(sp);
                        sep.setBackgroundColor(ctx.getColor(R.color.rambam_divider));
                        contentContainer.addView(sep);
                    }
                    TextView chTitle = new TextView(ctx);
                    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    cp.setMargins(0, 0, 0, dp(ctx, 16));
                    chTitle.setLayoutParams(cp);
                    String ordinal = ci < HEBREW_ORDINALS.length - 1
                            ? HEBREW_ORDINALS[ci + 1] : String.valueOf(ci + 1);
                    chTitle.setText("פרק " + ordinal);
                    chTitle.setTextColor(teal);
                    chTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                    chTitle.setTypeface(font, Typeface.BOLD);
                    chTitle.setGravity(Gravity.CENTER);
                    contentContainer.addView(chTitle);
                }

                List<String> halakhot = chapters.get(ci);
                for (int hi = 0; hi < halakhot.size(); hi++) {
                    addHalakha(ctx, hi + 1, halakhot.get(hi), teal, font,
                            ci == 0 && hi == 0, textSizeSp);
                }
            }
        }

        /**
         * Newspaper / drop-number style halacha paragraph.
         *
         * The ordinal letter (e.g. "ב") is embedded inline at the START of the text.
         * In RTL that means it appears at the far RIGHT of the first line, followed by
         * the halacha text flowing leftward.  Every subsequent line wraps to FULL WIDTH
         * (no column indentation) – exactly like a numbered entry in a printed sefer.
         */
        private void addHalakha(Context ctx, int num, String text, int tealColor,
                                Typeface font, boolean isFirst, int textSizeSp) {
            if (text == null || text.isEmpty()) return;

            String ordinal = num < HEBREW_ORDINALS.length
                    ? HEBREW_ORDINALS[num] : String.valueOf(num);

            // Spannable: [large bold teal ordinal]  [normal halacha text]
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(ordinal);
            int ordEnd = ordinal.length();
            ssb.setSpan(new ForegroundColorSpan(tealColor), 0, ordEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new RelativeSizeSpan(1.7f), 0, ordEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, ordEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append("  ");   // gap between number and text body
            ssb.append(text);

            TextView tv = new TextView(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, isFirst ? 0 : dp(ctx, 24), 0, 0);
            tv.setLayoutParams(lp);
            tv.setText(ssb);
            tv.setTextColor(Color.parseColor("#1C1C1E"));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
            tv.setTypeface(font);
            tv.setLineSpacing(dp(ctx, 4), 1f);
            tv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            tv.setTextDirection(View.TEXT_DIRECTION_RTL);
            tv.setGravity(Gravity.START | Gravity.TOP); // START in RTL = right edge
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tv.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
            }

            contentContainer.addView(tv);
        }

        // ─── JSON helpers ────────────────────────────────────────────────────────

        private static String getJsonStr(JsonObject obj, String key) {
            if (obj != null && obj.has(key) && obj.get(key).isJsonPrimitive())
                return obj.get(key).getAsString();
            return null;
        }

        private static String getNestedStr(JsonObject obj, String key, String nestedKey) {
            if (obj != null && obj.has(key) && obj.get(key).isJsonObject())
                return getJsonStr(obj.get(key).getAsJsonObject(), nestedKey);
            return null;
        }

        private Typeface loadFont(Context ctx) {
            try {
                // Using SBL Hebrew as requested
                Typeface tf = ResourcesCompat.getFont(ctx, R.font.sbl_hbrw);
                if (tf != null) return tf;
                // Fallback if file missing
                return ResourcesCompat.getFont(ctx, R.font.frank_ruhl_libre);
            } catch (Exception e) {
                return Typeface.SERIF;
            }
        }

        private static int dp(Context ctx, int dp) {
            return Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, dp,
                    ctx.getResources().getDisplayMetrics()));
        }
    }
}

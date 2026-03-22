package com.example.goodstart.api;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SefariaService {
    // encoded=true prevents double-encoding of the ref (which may contain commas/underscores)
    @GET("api/texts/{ref}?context=0")
    Call<SefariaResponse> getRambamChapter(@Path(value = "ref", encoded = true) String ref);

    @GET("api/calendars")
    Call<com.google.gson.JsonObject> getCalendars(@Query("date") String date);

    @GET("api/index/{book}")
    Call<com.google.gson.JsonObject> getBookIndex(@Path("book") String book);

    class SefariaResponse {
        @SerializedName("he")
        public JsonElement heRaw; // may be String[] or String[][] depending on chapter range
        public String ref;
        public String title;
        @SerializedName("heTitle")
        public String heTitle;
    }

    class CalendarResponse {
        public List<CalendarItem> calendar_items;
    }

    class CalendarItem {
        // title is {"en": "...", "he": "..."} in the Sefaria API — NOT a plain string
        public CalendarTitle title;
        @SerializedName("displayValue")
        public CalendarTitle displayValue;
        public String url; // URL-safe ref e.g. "Mishneh_Torah,_Foundations_of_the_Torah.1-3"
        public String ref; // full ref e.g. "Mishneh Torah, Foundations of the Torah 1-3"

        public static class CalendarTitle {
            @SerializedName("en")
            public String en;
            @SerializedName("he")
            public String he;
        }
    }
}

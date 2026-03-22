package com.example.goodstart.api;

import com.example.goodstart.models.StudyDay;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface StudyService {
    @GET("api/study/day")
    Call<StudyDay> getDailyStudy(@Query("date") String date);
}

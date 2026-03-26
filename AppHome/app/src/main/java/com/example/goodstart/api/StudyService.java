package com.example.goodstart.api;

import com.example.goodstart.model.ArticleDto;
import com.example.goodstart.models.StudyDay;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StudyService {
    @GET("api/study/day")
    Call<StudyDay> getDailyStudy(@Query("date") String date);

    @GET("api/articles")
    Call<List<ArticleDto>> getArticles();

    @GET("api/articles/{id}")
    Call<ArticleDto> getArticleById(@Path("id") String id);
}

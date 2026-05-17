package com.example.smartwatchhapticsystem.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface n8nApis {

    @POST("usecase-routing")
    Call<ResponseBody> postUseCaseRouting(@Body JsonObject body);

    @GET("get-usecases")
    Call<JsonArray> getUseCases();

}

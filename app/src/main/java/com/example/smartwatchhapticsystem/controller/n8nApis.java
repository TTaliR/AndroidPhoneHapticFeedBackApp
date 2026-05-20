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


    //http://localhost:5678/webhook/get-sensor-types
    @GET("get-sensor-types")
    Call<JsonArray> getUseCases();

}

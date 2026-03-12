package com.example.smartwatchhapticsystem.controller;

import com.example.smartwatchhapticsystem.model.LocationData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface n8nApis {


    @POST("sun-data")
    Call<ResponseBody> sendSunLocation(@Body LocationData locationData);

    @POST("moon-data")
    Call<ResponseBody> sendMoonLocation(@Body LocationData locationData);

    @POST("pollution-data")
    Call<ResponseBody> sendPollutionLocation(@Body LocationData locationData);

    @POST("temperature-data")
    Call<ResponseBody> sendTemperatureLocation(@Body LocationData locationData);

    @GET("get-usecases")
    Call<JsonArray> getUseCases();


}

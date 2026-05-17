package com.example.quizzapp_security.network;

import com.example.quizzapp_security.models.CheatLog;
import com.example.quizzapp_security.models.SessionLog;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseApi {

    @POST("rest/v1/cheat_logs")
    Call<Void> insertLog(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearerToken,
        @Header("Content-Type") String contentType,
        @Header("Prefer") String prefer,
        @Body CheatLog log
    );

    @POST("rest/v1/sessions")
    Call<Void> startSession(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearerToken,
        @Header("Content-Type") String contentType,
        @Header("Prefer") String prefer,
        @Body SessionLog session
    );

    @PATCH("rest/v1/sessions")
    Call<Void> endSession(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearerToken,
        @Header("Content-Type") String contentType,
        @Query("user_id") String userIdFilter,
        @Query("ended_at") String endedAtFilter,
        @Body SessionLog session
    );
}
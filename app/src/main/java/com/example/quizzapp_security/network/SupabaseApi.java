package com.example.quizzapp_security.network;

import com.example.quizzapp_security.models.CheatLog;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface SupabaseApi {
    @POST("rest/v1/cheat_logs")
    Call<Void> insertLog(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearerToken,
        @Header("Content-Type") String contentType,
        @Header("Prefer") String prefer,
        @Body CheatLog log
    );
}
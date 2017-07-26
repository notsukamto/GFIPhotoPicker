package com.github.potatodealer.gfiphotopicker.rest;


import com.github.potatodealer.gfiphotopicker.GFIPhotoPicker;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class InstagramApiClient {

    private static Retrofit retrofit = null;

    public static Retrofit getClient(String baseUrl) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

}

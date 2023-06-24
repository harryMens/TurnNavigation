package com.google.android.turnnavigation.network;

import static com.google.android.turnnavigation.network.Static.OPEN_DIRECTION;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class DirectionGenerator {
    private static Retrofit retrofit =
            new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(OPEN_DIRECTION)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();

    public static Network request =
            retrofit.create(Network.class);

    public Network getRequest(){
        return request;
    }
}
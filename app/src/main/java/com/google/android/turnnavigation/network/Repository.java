package com.google.android.turnnavigation.network;


import static com.google.android.turnnavigation.network.Static.GEOCODE_API_KEY;
import static com.google.android.turnnavigation.network.Static.OPEN_DIRECTION_KEY;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;

import com.google.android.turnnavigation.network.directions.Directions;
import com.google.android.turnnavigation.utility.GO;
import com.google.android.turnnavigation.utility.Geocode;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Repository {
    private static final String TAG = "Repository";

    private static Repository instance;
    Geocode geocode;
    GO going;
    private String language;
    Directions directions;
    private String lang;

    public MutableLiveData<Directions> liveDirections =  new MutableLiveData<>();
    public MutableLiveData<Boolean> loading = new MutableLiveData<>();

    public static Repository getInstance(){
        if (instance == null){
            instance = new Repository();
        }
        return instance;
    }
    private void gettingLanguage(){
        switch (Locale.getDefault().getLanguage()){
            case "de":{
                language = "de";
                lang = "de-DE";
            }
            break;
            case "es":{
                language = "es";
                lang = "es-ES";
            }
            break;
            case "ar":{
                language = "ar";
                lang = "ar";
            }
            break;
            case "zh":{
                language = "zh";
                lang = "zh-CN";
            }
            break;
            case "pt":{
                language = "pt";
                lang = "pt-BR";
            }
            break;
            case "fr":{
                language = "fr";
                lang = "fr-FR";
            }
            break;
            case "tr":{
                language = "tr";
                lang = "tr-TR";
            }
            break;
            case "uk":{
                language = "uk";
                lang = "uk-UA";
            }
            break;
            default:{
                language = "en";
                lang = "en-US";
            }
            break;
        }
    }
    public Repository() {
        gettingLanguage();
        geocode = new Geocode();
        going = new GO();
        directions = new Directions();
    }

    public Call<GO> getGeocoder(String address){
        return GeocoderGenerator.request.getGeocoder(address+".json", GEOCODE_API_KEY,lang);
    }
    public Call<GO> getReverseGeocoder(String address){
        return GeocoderGenerator.request.getReverseGeocoder(address+".json",GEOCODE_API_KEY,lang);
    }
    public void
    getOpenDirections(String mode, String start, String end, Context context){
        DirectionGenerator.request.getOpenDirection(mode,OPEN_DIRECTION_KEY,start,end)
                .enqueue(new Callback<Directions>() {
                    @Override
                    public void onResponse(Call<Directions> call, Response<Directions> response) {
                        if (response.isSuccessful()){
                            if (response.body() != null){
                                liveDirections.postValue(null);
                                liveDirections.postValue(response.body());
                                //loading.setValue(false);
                            }
                            else{
                                try{
                                    loading.setValue(false);
                                    Toast.makeText(context, "route not found", Toast.LENGTH_SHORT).show();
                                }
                                catch (Exception e){
                                    Log.d(TAG, "onResponse: open Toast problem "+e.getMessage());
                                }                            }
                        }
                        else{
                            loading.setValue(false);
                            try{
                                Toast.makeText(context, "no route found", Toast.LENGTH_SHORT).show();
                            }
                            catch (Exception e){
                                Log.d(TAG, "onResponse: open Toast problem "+e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Directions> call, Throwable t) {
                       loading.setValue(false);
                        try{
                            Toast.makeText(context, "check network connection", Toast.LENGTH_SHORT).show();
                        }
                        catch (Exception e){
                            Log.d(TAG, "onResponse: open Toast problem "+e.getMessage());
                        }
                        // Log.d(TAG, "onFailure: there was a problem with open direction "+t.getMessage());
                    }
                });
    }
}
















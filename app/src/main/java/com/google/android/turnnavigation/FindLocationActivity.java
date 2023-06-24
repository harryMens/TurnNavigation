package com.google.android.turnnavigation;


import static com.google.android.turnnavigation.network.Static.FIND_LOCATION;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.turnnavigation.adapter.ListAdapter;
import com.google.android.turnnavigation.network.Repository;
import com.google.android.turnnavigation.utility.GO;
import com.google.android.turnnavigation.utility.Geocode;
import com.google.android.turnnavigation.viewmodel.MyViewModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FindLocationActivity extends AppCompatActivity implements ListAdapter.ListViewHolder.ListListener {
    private static final String TAG = "FindLocationActivity";

    List<String> locationData = new ArrayList<>();
    EditText searchView;
    RecyclerView recyclerView;
    LinearLayout linearLayout;
    ProgressBar progressBar;
    ImageView networkAvailability;
    TextView offline;
    ListAdapter listAdapter;
    boolean change = true;
    MyViewModel myViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_location);

        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.search_recycler);
        linearLayout = findViewById(R.id.layout_loading);
        progressBar = findViewById(R.id.search_progress_bar);
        networkAvailability = findViewById(R.id.network_error);
        offline = findViewById(R.id.loading);

        myViewModel = new ViewModelProvider(this).get(MyViewModel.class);
        initialiseRecyclerView();
        search();


    }
    void search(){

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().length() > 2 && change) {
                    offline.setText("loading");
                    linearLayout.setVisibility(View.VISIBLE);
                    networkAvailability.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.VISIBLE);
                    Repository.getInstance().getGeocoder(s.toString()).enqueue(new Callback<GO>() {
                        @Override
                        public void onResponse(Call<GO> call, Response<GO> response) {

                            if (response.isSuccessful()) {
                                if (response.body() != null){
                                    if (response.body().getGeocode() != null) {
                                        initialiseRecyclerView();
                                        linearLayout.setVisibility(View.GONE);
                                        networkAvailability.setVisibility(View.VISIBLE);
                                        listAdapter.setPrediction(response.body().getGeocode());
                                        listAdapter.notifyDataSetChanged();
                                        recyclerView.setVisibility(View.VISIBLE);
                                    }
                                }
                                else{
                                    Log.d(TAG, "onResponse: body was null ");
                                }
                            }
                            else{
                                recyclerView.setVisibility(View.GONE);
                                offline.setText("offline");
                                progressBar.setVisibility(View.GONE);
                                networkAvailability.setVisibility(View.VISIBLE);
                                Log.d(TAG, "onResponse: there was error "+response.errorBody());
                            }
                        }

                        @Override
                        public void onFailure(Call<GO> call, Throwable t) {

                        }
                    });
                }
                else{
                    change = true;
                }
            }
        });

    }

    void initialiseRecyclerView(){
        listAdapter = new ListAdapter(this,this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //  recyclerView.addItemDecoration(new Decoration(1));
        recyclerView.setAdapter(listAdapter);
    }

    @Override
    public void getListPosition(Geocode prediction) {
        change = false;
        locationData.clear();
        String county = prediction.getAddress().getCounty();
        String latitude = String.valueOf(prediction.getPosition().getLatitude());
        String longitude = String.valueOf(prediction.getPosition().getLongitude());
        String label = prediction.getAddress().getLabel()+","+prediction.getAddress().getCountry();

        locationData.add(county);
        locationData.add(latitude);
        locationData.add(longitude);
        locationData.add(label);
        searchView.setText(label);
        listAdapter.clear();
        hideSoftKey();
        if (locationData.size() > 0){
            myViewModel.repository.liveDirections.setValue(null);
            Intent intent = new Intent(FindLocationActivity.this, MainActivity.class);
            intent.putExtra(FIND_LOCATION, (Serializable) locationData);
            startActivity(intent);
            finish();
        }
        else{
            Toast.makeText(FindLocationActivity.this, "Please no Address is provided", Toast.LENGTH_SHORT).show();
        }
    }
    void hideSoftKey(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}
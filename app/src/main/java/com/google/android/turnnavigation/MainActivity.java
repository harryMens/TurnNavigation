package com.google.android.turnnavigation;

import static com.google.android.turnnavigation.network.Static.CYCLING;
import static com.google.android.turnnavigation.network.Static.FIND_LOCATION;
import static com.google.android.turnnavigation.network.Static.SCREEN_PAGE;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.turnnavigation.boundary.Boundary;
import com.google.android.turnnavigation.boundary.Geometry;
import com.google.android.turnnavigation.boundary.JsonUtils;
import com.google.android.turnnavigation.network.directions.Directions;
import com.google.android.turnnavigation.network.directions.Segments;
import com.google.android.turnnavigation.network.directions.Steps;
import com.google.android.turnnavigation.viewmodel.MyViewModel;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, View.OnTouchListener, TextToSpeech.OnInitListener {
    private static final String TAG = "MainActivity";
    private static final int LOCATION_SETTINGS_REQUEST = 100;

    SupportMapFragment mapFragment;
    View mapFragmentView;
    List<LatLng> boundaryList = new ArrayList<>();
    private ActivityResultLauncher<String> filePickerLauncher;

    GoogleMap mMap;
    MyViewModel viewModel;
    List<Integer> wayPoints = new ArrayList<>();
    boolean ready = false;
    boolean firstTime = true;
    List<Double> stepsTime = new ArrayList<>();
    List<Double> stepsDistance = new ArrayList<>();
    List<LatLng> latLngList = new ArrayList<>();

    FusedLocationProviderClient fusedLocationClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    int locationValue = 0;
    int nextStep = 0;
    int gpxTarget= 0;
    int remainder = 0;
    int times = 0;

    ImageButton navigate;
    TextView instructionMessage;
    String message = "";
    List<String> locationData;
    RelativeLayout container;

    double latitude, longitude;
    double navigateLatitude, navigateLongitude;
    Location myCurrentLocation;
    Drawable moveImage;
    Drawable navigateImage;
    Drawable satellite;
    boolean audio = true;
    double timeShowed = 0.0;
    boolean allow = false;

    TextView locality, time, distance;
    ImageView speakerOnView, speakerOffView;
    List<Steps> stepsList = new ArrayList<>();
    List<Steps> stepsList1 = new ArrayList<>();
    List<String> stepsInstructions = new ArrayList<>();
    double previousTime;
    boolean speakNow = true;


    PolylineOptions polylineOptions;
    ProgressBar progressBar;
    ProgressDialog progress;
    private TextToSpeech textToSpeech;
    ArrayList<LatLng> pointsGpx = new ArrayList<>();
    ArrayList<String> stepsLocality = new ArrayList<>();
    int multi = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri result) {
                        if (result != null) {
                            loadAndDisplayGpxTrack(result);
                        }
                    }
                });

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.category_map);
        mapFragment.getMapAsync(this);

        navigate = findViewById(R.id.hand_btn);
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);

        locality = findViewById(R.id.sub_local);
        time = findViewById(R.id.time_taken);
        distance = findViewById(R.id.distance_taken);
        speakerOnView = findViewById(R.id.speaker_on);
        speakerOffView = findViewById(R.id.speaker_off);
        progressBar = findViewById(R.id.progress_bar);
        instructionMessage = findViewById(R.id.instructions);
        container = findViewById(R.id.container);

        moveImage = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_move, null);
        navigateImage = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_navigate, null);
        satellite = ResourcesCompat.getDrawable(getResources(), R.drawable.satelite, null);
        textToSpeech = new TextToSpeech(this, this);

        if (getIntent().hasExtra(SCREEN_PAGE)){
            navigate.setImageDrawable(navigateImage);

        }
        else {
            navigate.setImageDrawable(moveImage);
            navigate.setPadding(4,4,4,4);
            locationData = (List<String>) getIntent().getSerializableExtra(FIND_LOCATION);
            navigateLatitude = Double.parseDouble(locationData.get(1));
            navigateLongitude = Double.parseDouble(locationData.get(2));

        }
        init();
       // viewModel.repository.loading.setValue(true);
       }

       private void init(){


           requestPermissions();
           promptUser();
           getLastKnownLocation();
           gettingLastKnownLocation();
           setLocationCallback();
           startLocationUpdates();
           getLiveDirection();
           manageLoading();
           setTopLayout();
           setFollow();

           navigate.setOnClickListener(view -> {
               if (viewModel.repository.loading.getValue() != null && viewModel.repository.loading.getValue()) {
                   Toast.makeText(MainActivity.this, "Please Wait...", Toast.LENGTH_SHORT).show();

               } else if (navigate.getDrawable() == moveImage) {
                   viewModel.repository.loading.setValue(true);
                           if (latitude != 0.0 && longitude != 0.0) {
                               String destination = navigateLongitude + "," + navigateLatitude;
                               String begin = longitude + "," + latitude;
                               viewModel.getOpenDirections(CYCLING, begin, destination);
                           }


               }
               else if (navigate.getDrawable() == navigateImage) {
                   viewModel.follow.setValue(true);
                   navigate.setPadding(4,4,4,4);


                       LatLng loc = new LatLng(latitude, longitude);
                       CameraUpdate cameraUpdate1 = CameraUpdateFactory.newCameraPosition(
                               new CameraPosition(loc, 18.0f, 0.0f, 0.0f));
                       mMap.animateCamera(cameraUpdate1);

                       new Handler().postDelayed(new Runnable() {
                           @Override
                           public void run() {
                               viewModel.follow.setValue(false);
                               navigate.setImageDrawable(satellite);
                           }
                       }, 1000);
               }
               else if (navigate.getDrawable() == satellite) {
                   if (getIntent().hasExtra(SCREEN_PAGE)) {
                       LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                       for (LatLng point : pointsGpx) {
                           boundsBuilder.include(point);
                       }
                       LatLngBounds bounds = boundsBuilder.build();
                       mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                       navigate.setImageDrawable(navigateImage);
                       navigate.setPadding(0, 0, 5, 0);
                   } else {
                       LatLngBounds.Builder builder = new LatLngBounds.Builder();
                       builder.include(new LatLng(myCurrentLocation.getLatitude(), myCurrentLocation.getLongitude()));
                       builder.include(new LatLng(navigateLatitude, navigateLongitude));
                       LatLngBounds bounds = builder.build();

                       // Set a padding to create space around the bounding box (optional)
                       int padding = 100; // in pixels

                       // Animate the camera to the bounding box with padding
                       CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                       mMap.animateCamera(cameraUpdate);
                       navigate.setImageDrawable(navigateImage);
                       navigate.setPadding(0, 0, 5, 0);
                   }
               }
           });

           findViewById(R.id.bottom_layout)
                   .setOnClickListener(v -> {
                       if (viewModel.repository.loading.getValue() != null && viewModel.repository.loading.getValue()) {
                           Toast.makeText(MainActivity.this, "Please Wait...", Toast.LENGTH_SHORT).show();

                       } else if (audio) {
                           Toast.makeText(this, "audio disabled", Toast.LENGTH_SHORT).show();
                           audio = false;
                           speakerOnView.setVisibility(View.GONE);
                           speakerOffView.setVisibility(View.VISIBLE);
                       } else {
                           Toast.makeText(this, "audio enabled", Toast.LENGTH_SHORT).show();
                           audio = true;
                           speakerOffView.setVisibility(View.GONE);
                           speakerOnView.setVisibility(View.VISIBLE);
                       }
                   });
           getBoundary();

       }
    private void getBoundary() {
        String json = JsonUtils.getJsonFromAsset(this, "bounder.json");
        Gson gson = new Gson();
        Boundary myObject = gson.fromJson(json, Boundary.class);
        Geometry geometry = myObject.getFeatures().get(0).getGeometry();
        for (int i = 0; i < geometry.getCoordinates().size(); i++) {
            for (int j = 0; j < geometry.getCoordinates().get(i).size(); j++) {
                for (int k = 0; k < geometry.getCoordinates().get(i).get(j).size(); k++) {
                    for (int l = 0; l < geometry.getCoordinates().get(i).get(j).get(k).size(); l++) {
                        for (int m = 0; m < geometry.getCoordinates().get(i).get(j).get(k).get(l).size(); m++) {
                            if (geometry.getCoordinates().get(i).get(j).get(k).get(l).get(m) > 1) {
                                LatLng latLng = new LatLng(geometry.getCoordinates().get(i).get(j).get(k).get(l).get(1),
                                        geometry.getCoordinates().get(i).get(j).get(k).get(l).get(0));
                                boundaryList.add(latLng);
                            }
                        }
                    }
                }
            }
        }
        Log.e(TAG, "getBoundary: checking list " + boundaryList);

    }

    void promptUser() {

        int locationInterval = 5000;
        int locationFastestInterval = 1000;
        int locationMaxWaitTime = 1000;
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, locationInterval)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(locationFastestInterval)
                .setMaxUpdateDelayMillis(locationMaxWaitTime)
                .build();


        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {


            @Override
            public void onComplete(Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.

                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        MainActivity.this,
                                        LOCATION_SETTINGS_REQUEST);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            }
        });


    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void checkingLocationsEnabled(Boolean location, String locationName) {
        if (location != null && location) {
            Log.d(TAG, "MainActivity: camera enabled");
        } else {
            requestPermissions(new String[]{locationName}, LOCATION_SETTINGS_REQUEST);
        }
    }

    void requestPermissions() {
        ActivityResultLauncher<String[]> someActivityResultLauncher1 =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            Boolean fineLocation = result.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                            Boolean courseLocation = result.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false);
                            Boolean backgroundLocation = result.getOrDefault(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION, false);

                            checkingLocationsEnabled(fineLocation, android.Manifest.permission.ACCESS_FINE_LOCATION);
                            checkingLocationsEnabled(courseLocation, android.Manifest.permission.ACCESS_COARSE_LOCATION);
                            //   checkingLocationsEnabled(backgroundLocation, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                        }
                    }
                });
        ActivityResultLauncher<Intent> someActivityResultLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                            }
                        });

        someActivityResultLauncher1.launch(new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                //     Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private Location getLastKnownLocation() {
        LocationManager mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return null;
            }
            Location l = mLocationManager.getLastKnownLocation(provider);
            Log.d(TAG, "last known location, provider: %s, location: %s" + l +
                    l);

            if (l == null) {
                continue;
            }
            if (bestLocation == null
                    || l.getAccuracy() < bestLocation.getAccuracy()) {
                Log.d(TAG, "found best last known location: %s" + l);
                bestLocation = l;
            }
        }
        if (bestLocation == null) {
            return null;
        }
        return bestLocation;
    }

    void setLocationCallback() {
        // Log.d(TAG, "setLocationCallback: 365456454545654");
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    //  Log.d(TAG, "onLocationResult: location results was null");
                    return;
                }
                //  Log.d(TAG, "onLocationResult: location results was not null");
                for (Location location : locationResult.getLocations()) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    myCurrentLocation = location;


                    if (container.getVisibility() == View.VISIBLE &&
                            timeShowed != 0.0) {
                        double currentTime = SystemClock.elapsedRealtime();
                        double timeElapsed = currentTime - timeShowed;
                        if (timeElapsed < 0) {
                            timeElapsed *= -1;
                        }
                        if (timeElapsed > 120000) {
                            container.setVisibility(View.GONE);
                        }
                    }

                    if (getIntent().hasExtra(SCREEN_PAGE) && pointsGpx.isEmpty()){
                        return;
                    }
                    if (mMap != null && viewModel.follow.getValue() != null && viewModel.follow.getValue()
                            && navigate.getDrawable() == satellite) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
                    }
                    if (mMap != null && navigate.getDrawable() == satellite) {

                        if (viewModel.follow.getValue() != null && viewModel.follow.getValue()){
                             mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
                            Log.e(TAG, "onLocationResult: following");
                        }
                        else{
                            Log.e(TAG, "onLocationResult: not following" );

                        }

                        int sizing;
                        if (getIntent().hasExtra(SCREEN_PAGE)){
                            sizing = wayPoints.size();
                        }
                        else{
                            sizing = latLngList.size();
                        }
                        Log.d(TAG, "checking sizing: int "+sizing);
                        int i = nextStep;
                        boolean found = false;

                        do {
                            Location targetLocation = new Location("");
                            if (getIntent().hasExtra(SCREEN_PAGE)) {
                                targetLocation.setLatitude(pointsGpx.get(wayPoints.get(i)).latitude);
                                targetLocation.setLongitude(pointsGpx.get(wayPoints.get(i)).longitude);
                            }
                            else{
                                targetLocation.setLatitude(latLngList.get(i).latitude);
                                targetLocation.setLongitude(latLngList.get(i).longitude);
                            }
//                            if (getIntent().hasExtra(SCREEN_PAGE)) {
//                                targetLocation.setLatitude(pointsGpx.get(stepsList.get(i).getWay_points().get(0)).latitude);
//                                targetLocation.setLongitude(pointsGpx.get(stepsList.get(i).getWay_points().get(0)).longitude);
//                            }
//                            else {
////                                targetLocation.setLatitude(polylineOptions.getPoints().get(stepsList.get(i).getWay_points().get(0)).latitude);
////                                targetLocation.setLongitude(polylineOptions.getPoints().get(stepsList.get(i).getWay_points().get(0)).longitude);
//
//                                targetLocation.setLatitude(polylineOptions.getPoints().get(wayPoints.get(0)).latitude);
//                                targetLocation.setLongitude(polylineOptions.getPoints().get(wayPoints.get(0)).longitude);
//                            }
                            Location currentLocation = new Location("");
                            currentLocation.setLatitude(latitude);
                            currentLocation.setLongitude(longitude);
                            float distance1 = +targetLocation.distanceTo(currentLocation);

                            if (targetLocation.distanceTo(currentLocation) < 25) {

                                if (!speakNow) {
                                    double currentView = SystemClock.elapsedRealtime();
                                    double interval = currentView - previousTime;
                                    if (interval < 0) {
                                        interval *= -1;
                                    }
                                    Log.d(TAG, "onLocationResult: interval "+interval);
                                    if (interval >10000){
                                        speakNow = true;
                                    }
                                }
                                if (getIntent().hasExtra(SCREEN_PAGE)){

                                    instructionMessage.setText(stepsInstructions.get(i));
                                    locality.setText(stepsLocality.get(i));
                                    if (audio && speakNow) {
                                        previousTime = SystemClock.elapsedRealtime();
                                        Log.d(TAG, "onLocationResult: allowed to speak");
                                        speakNow = false;
                                        speakText(stepsInstructions.get(i));
                                    }
                                    else{
                                        Log.d(TAG, "onLocationResult: denied to speak");
                                    }

                                    message = stepsInstructions.get(i);

                                }
                                else{
                                    locality.setText(stepsList.get(i).getName());

                                    instructionMessage.setText(stepsList.get(i).getInstruction());
                                    if (audio && speakNow) {
                                        previousTime = SystemClock.elapsedRealtime();
                                        Log.d(TAG, "onLocationResult: allowed speaker");
                                        speakNow = false;
                                        speakText(stepsList.get(i).getInstruction());
                                    }
                                    else{
                                        Log.d(TAG, "onLocationResult: denied speaker");
                                    }
                                    message = stepsList.get(i).getInstruction();
                                }
                                findViewById(R.id.bottom_layout).setVisibility(View.VISIBLE);
                                viewModel.showTopLayout.setValue(true);
                                distance.setText(getDistance(stepsDistance.get(i)));
                                double currentClick = SystemClock.elapsedRealtime();
                                time.setText(getTime(stepsTime.get(i)));

                                found = true;
                                nextStep = i;


                            }
                            i++;
                        }
                        while (i < sizing && !found);
                    }
                }
            }
        };

    }

    private void setTopLayout() {
        viewModel.showTopLayout.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean != null) {
                    if (aBoolean) {
                        Log.d(TAG, "onChanged: please show");
                        container.setVisibility(View.VISIBLE);
                        timeShowed = SystemClock.elapsedRealtime();


                    }
                }
            }
        });
    }

    private void speakText(String text) {
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageId");
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    void pointToLocation(int num) {
        Log.e(TAG, "pointToLocation: location was called" );
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (num == locationValue) {
                    viewModel.follow.setValue(true);
                }
            }
        }, 5000);
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    void gettingLastKnownLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            // Got last known location. In some rare situations this can be null.
            if (location != null) {

//
                myCurrentLocation = location;

                Log.d(TAG, "gettingLastKnownLocation: location " + location);
            } else {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                Log.d(TAG, "gettingLastKnownLocation: location was null");
            }
        });


    }

    String getTime(Double time) {
        int hours = (int) (time / 3600); // Number of whole hours (0 in this case)
        int remainingSeconds = (int) (time % 3600); // Remaining seconds after extracting hours

        int minutes = remainingSeconds / 60; // Number of whole minutes
        int finalSeconds = remainingSeconds % 60; // Remaining seconds after extracting minutes

        if (hours == 0) {
            return minutes + " min " + finalSeconds + " sec";
        } else {
            return hours + " Hrs " + minutes + " min";
        }

    }

    String getDistance(Double m) {
        int km = (int) (m / 1000); // Number of whole hours (0 in this case)
        int remainingM = (int) (m % 1000); // Remaining seconds after extracting hours

        if (km == 0) {
            return "(" + remainingM + " m)";
        } else {
            return "(" + km + " Km " + remainingM + " m)";
        }

    }

    private void handle(){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ready){
                    ready = false;
                    Log.d(TAG, "onChanged: waypoint size"+wayPoints.size());
                   // Toast.makeText(MainActivity.this, "entered", Toast.LENGTH_SHORT).show();


                    multi++;
                    int low = multi-1;
                    int bin = low*250;


                    if (multi <= times) {
                        int dest = multi * 250;
                        Log.d(TAG, "run: multi "+multi);
                        Log.d(TAG, "run: low "+low);
                        Log.d(TAG, "run: times "+dest);
                        for (int i = 0; i < stepsList1.size(); i++) {
                            wayPoints.add(stepsList1.get(i).getWay_points().get(0));
                            stepsInstructions.add(stepsList1.get(i).getInstruction());
                            stepsDistance.add(stepsList1.get(i).getDistance());
                            stepsTime.add(stepsList1.get(i).getDuration());
                            stepsLocality.add(stepsList1.get(i).getName());
                        }
                        stepsList1.clear();

                        ready = false;
                        String destination = pointsGpx.get(dest - 1).longitude + "," + pointsGpx.get(dest - 1).latitude;
                        String begin = pointsGpx.get(bin).longitude + "," + pointsGpx.get(bin).latitude;
                        viewModel.getOpenDirections(CYCLING, begin, destination);
                        handler.postDelayed(this, 3000);

                    }
                    else{
                        Log.d(TAG, "run: reminder "+multi);
                        if (remainder > 0){
                            for (int i = 0; i< stepsList1.size(); i++) {
                                wayPoints.add(stepsList1.get(i).getWay_points().get(0)+remainder);
                                stepsInstructions.add(stepsList1.get(i).getInstruction());
                                stepsDistance.add(stepsList1.get(i).getDistance());
                                stepsTime.add(stepsList1.get(i).getDuration());
                                stepsLocality.add(stepsList1.get(i).getName());
                            }
                            stepsList1.clear();

                            int dest = multi * 250;
                            dest -= 250;
                            dest += remainder;
                            String destination = pointsGpx.get(dest - 1).longitude + "," + pointsGpx.get(dest - 1).latitude;
                            String begin = pointsGpx.get(bin).longitude + "," + pointsGpx.get(bin).latitude;
                            remainder = 0;
                            viewModel.getOpenDirections(CYCLING, begin, destination);
                            handler.postDelayed(this, 3000);


                        } else{

                            List<Double> time = new ArrayList<>(stepsTime);
                            List<Double> dis = new ArrayList<>(stepsDistance);
                            stepsTime.clear();
                            stepsDistance.clear();
                            double sumTime = 0.0, sumDist = 0.0 ;
                            for (int i =0; i < time.size(); i++){
                                sumTime += time.get(i);
                                sumDist += dis.get(i);
                            }
                            for (int i =0; i < time.size(); i++){
                                stepsDistance.add(sumDist);
                                stepsTime.add(sumTime);
                                sumTime -= time.get(i);
                                sumDist -= dis.get(0);
                            }
                            Log.d(TAG, "run: finalise");
                            for (int i = 0; i< wayPoints.size(); i++) {
                                mMap.addMarker(new MarkerOptions().position(
                                        new LatLng(pointsGpx.get(wayPoints.get(i)).latitude,
                                                pointsGpx.get(wayPoints.get(i)).longitude)));
                            }
                            Log.d(TAG, "onChanged: we are done");
                            viewModel.repository.loading.setValue(false);

                            Toast.makeText(MainActivity.this, "this is waypoint "+wayPoints.size(), Toast.LENGTH_SHORT).show();

                        }
                    }
                }
                else{
                    handler.postDelayed(this,3000);
                }
            }
        },3000);
    }
    private void getLiveDirection() {
        viewModel.getDirectionsLive().observe(this, new Observer<Directions>() {
            @Override
            public void onChanged(Directions directions) {
                if (directions != null) {

                    List<List<Double>> coordinates = directions.getFeatures().get(0).getGeometry().getCoordinates();
                    Segments segments = directions.getFeatures().get(0).getProperties().getSegments().get(0);
                    List<Steps> steps = segments.getSteps();

                    if (!getIntent().hasExtra(SCREEN_PAGE)){
                        viewModel.repository.loading.setValue(false);
                        stepsList.clear();
                        stepsList.addAll(steps);


                        double sumTime = 0.0, sumDist = 0.0 ;
                        for (int i =0; i < stepsList.size(); i++){
                            sumTime += stepsList.get(i).getDistance();
                            sumDist += stepsList.get(i).getDistance();
                        }
                        for (int i =0; i < stepsList.size(); i++) {
                            stepsDistance.add(sumDist);
                            stepsTime.add(sumTime);
                            sumTime -= stepsList.get(i).getDuration();
                            sumDist -= stepsList.get(0).getDistance();

                        }
                        polylineOptions = new PolylineOptions();

                        locality.setText(locationData.get(0));
                        for (int j = 0; j < coordinates.size(); j++) {
                            polylineOptions.add(new LatLng(coordinates.get(j).get(1), coordinates.get(j).get(0)));
                        }

                        distance.setText(getDistance(segments.getDistance()));
                        time.setText(getTime(segments.getDuration()));
                        // Add the polyline to the map
                        if (mMap != null) {
                            mMap.addPolyline(polylineOptions);
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
                            }
                            mMap.setMyLocationEnabled(true);
                            mMap.isMyLocationEnabled();
                        }

                        for (int i = 0; i< stepsList.size(); i++) {
                            latLngList.add(new LatLng(polylineOptions.getPoints().get(stepsList.get(i).getWay_points().get(0)).latitude,
                                    polylineOptions.getPoints().get(stepsList.get(i).getWay_points().get(0)).longitude));
                            if (mMap != null) {
                                mMap.addMarker(new MarkerOptions().position(
                                        new LatLng(polylineOptions.getPoints().get(stepsList.get(i).getWay_points().get(0)).latitude,
                                                polylineOptions.getPoints().get(stepsList.get(i).getWay_points().get(0)).longitude)));
                            }
                        }

                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(new LatLng(myCurrentLocation.getLatitude(), myCurrentLocation.getLongitude()));
                        builder.include(polylineOptions.getPoints().get(polylineOptions.getPoints().size() - 1));
                        LatLngBounds bounds = builder.build();

                        // Set a padding to create space around the bounding box (optional)
                        int padding = 100; // in pixels

                        // Animate the camera to the bounding box with padding
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                        mMap.animateCamera(cameraUpdate);


                        findViewById(R.id.bottom_layout).setVisibility(View.VISIBLE);
                        navigate.setImageDrawable(navigateImage);

                        navigate.setPadding(0,0,5,0);
                    }
                    else{
                        stepsList1.clear();
                        stepsList1.addAll(steps);
                        ready = true;
                    }



//                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
//                    String jsonResponse = gson.toJson(steps.get(0).g);
//                    // Print the JSON response to log in a vertical format
//                    JsonParser jsonParser = new JsonParser();
//                    String formattedResponse = gson.toJson(jsonParser.parse(jsonResponse));
//                    Log.e("RetrofitResponse", formattedResponse);

                }
            }
        });
    }

    private void showProgressBar() {
        progress = new ProgressDialog(this);
        progress.setMessage("Loading");
        progress.setCanceledOnTouchOutside(false);
        progress.setCancelable(false);
        progress.show();

    }
    void manageLoading() {
        viewModel.repository.loading.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean != null) {
                    if (aBoolean) {
                        showProgressBar();
                    } else {
                        if (progress != null) {
                            progress.dismiss();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mapFragmentView = mapFragment.getView();
        mapFragmentView.setOnTouchListener(MainActivity.this);
//        Location location = new Location("");
//        location.setLatitude(0.0);
//        location.setLongitude(0.0);
//        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(
//                new CameraPosition(new LatLng(navigateLatitude, navigateLongitude), 12.0f, 0.0f, location.getBearing()));
//        mMap.animateCamera(cameraUpdate);
//        locality.setText(locationData.get(0));

        if (getIntent().hasExtra(SCREEN_PAGE)) {
            gpxDisplayCode();
        }
        mMap.setMinZoomPreference(6);
        mMap.setMaxZoomPreference(20);
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

// Iterate through the list and include each LatLng coordinate in the bounds
            for (LatLng latLng : boundaryList) {
                builder.include(latLng);
            }

            LatLngBounds bounds = builder.build();
            addWaterOverlay();

            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.isMyLocationEnabled();
    }
    private void addWaterOverlay() {
        // Load the water overlay image
        Bitmap waterOverlay = BitmapFactory.decodeResource(getResources(), R.drawable.layout_bg);

        // Calculate the bounds for the water overlay (excluding the desired country)
        LatLngBounds waterOverlayBounds = getWaterOverlayBounds();

        // Create an ImageView to display the water overlay
        ImageView waterOverlayImageView = new ImageView(this);
        waterOverlayImageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        waterOverlayImageView.setImageBitmap(waterOverlay);

        // Add the ImageView to the layout
        ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(waterOverlayImageView);

        // Adjust the size and position of the ImageView to match the water overlay bounds
        adjustWaterOverlayImageView(waterOverlayImageView, waterOverlayBounds);
    }

    private LatLngBounds getWaterOverlayBounds() {
        // Bounds for the entire world
        LatLngBounds.Builder worldBoundsBuilder = new LatLngBounds.Builder();
        LatLng southwest = new LatLng(-90, -180);
        LatLng northeast = new LatLng(90, 180);
        worldBoundsBuilder.include(southwest);
        worldBoundsBuilder.include(northeast);
        LatLngBounds worldBounds = worldBoundsBuilder.build();

        // Subtract the bounds of the desired country from the world bounds
        LatLngBounds.Builder waterOverlayBoundsBuilder = new LatLngBounds.Builder();
        for (LatLng coordinate : boundaryList) {
            waterOverlayBoundsBuilder.include(coordinate);

        }
        LatLngBounds waterOverlayBounds = waterOverlayBoundsBuilder.build();

        return waterOverlayBounds;
    }



    private void adjustWaterOverlayImageView(ImageView imageView, LatLngBounds bounds) {
        mMap.setLatLngBoundsForCameraTarget(bounds);

        mMap.setOnCameraMoveListener(() ->  {
            LatLngBounds currentBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
            if (!bounds.contains(currentBounds.southwest) || !bounds.contains(currentBounds.northeast)) {
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.GONE);
            }
        });
    }

    void setFollow(){
        viewModel.follow.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean != null){
                    if (!aBoolean){
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                    viewModel.follow.setValue(true);
                            }
                        }, 5000);
                    }
                }
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if ( viewModel.repository.loading.getValue() != null && viewModel.repository.loading.getValue()){
            Toast.makeText(this, "Please Wait...", Toast.LENGTH_SHORT).show();

        }
        else {
            viewModel.follow.setValue(false);
            locationValue++;
            if (navigate.getDrawable() == satellite) {
                pointToLocation(locationValue);
            }
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if ( viewModel.repository.loading.getValue() != null && viewModel.repository.loading.getValue()){
            Toast.makeText(this, "Please Wait...", Toast.LENGTH_SHORT).show();

        }
        else if (mapFragmentView != null && navigate.getDrawable() == satellite) {
            mapFragmentView.dispatchTouchEvent(ev);
        } else {
            Log.d(TAG, "dispatchTouchEvent: view was null");
        }
        return super.dispatchTouchEvent(ev);
    }
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Set the language for text-to-speech
            int result = textToSpeech.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language data is missing or the language is not supported
                // Handle the error condition
            }
        } else {
            // Text-to-speech initialization failed
            // Handle the error condition
        }
    }

    @Override
    public void onBackPressed() {
        if ( viewModel.repository.loading.getValue() != null && viewModel.repository.loading.getValue()){
            Toast.makeText(this, "Please Wait...", Toast.LENGTH_SHORT).show();

        }
        else{
            finish();
            stopLocationUpdates();
            viewModel.getDirectionsLive().setValue(null);
        }

    }



    void gpxDisplayCode(){

        // Trigger file selection
        openFilePicker();
    }
    private void openFilePicker() {
        filePickerLauncher.launch("application/gpx+xml");
    }

    private void loadAndDisplayGpxTrack(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            InputSource inputSource = new InputSource(inputStream);
            org.w3c.dom.Document document = documentBuilder.parse(inputSource);


            org.w3c.dom.Element rootElement = document.getDocumentElement();
            org.w3c.dom.NodeList nodeList = rootElement.getElementsByTagName("trkpt");

            for (int i = 0; i < nodeList.getLength(); i++) {
                org.w3c.dom.Node node = nodeList.item(i);
                org.w3c.dom.Element element = (org.w3c.dom.Element) node;
                double lat = Double.parseDouble(element.getAttribute("lat"));
                double lon = Double.parseDouble(element.getAttribute("lon"));

                LatLng latLng = new LatLng(lat, lon);
                pointsGpx.add(latLng);
            }

            if (!pointsGpx.isEmpty()) {
                viewModel.repository.loading.setValue(true);
                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(pointsGpx)
                        .width(15)
                        .color(Color.GRAY);

                Polyline polyline = mMap.addPolyline(polylineOptions);

                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                for (LatLng point : pointsGpx) {
                    boundsBuilder.include(point);
                }
                LatLngBounds bounds = boundsBuilder.build();

                mMap.addMarker(new MarkerOptions().position(pointsGpx.get(pointsGpx.size()-1)).title("destination"));

                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                ready = true;
                times = pointsGpx.size()/250;
                remainder = pointsGpx.size()/250;
                handle();
            }
        } catch (Exception e) {
            viewModel.repository.loading.setValue(false);

            Log.e("GPX Load", "Error loading GPX file", e);
            Toast.makeText(this, "error loading GPX file", Toast.LENGTH_SHORT).show();
            finish();
        }
    }



}
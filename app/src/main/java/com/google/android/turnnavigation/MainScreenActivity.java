package com.google.android.turnnavigation;

import static com.google.android.turnnavigation.network.Static.SCREEN_PAGE;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
        Button findLocation = findViewById(R.id.map_location);
        Button drawRoute = findViewById(R.id.two_locations);

        findLocation.setOnClickListener(view -> {
            Intent intent = new Intent(MainScreenActivity.this, MainActivity.class);
            intent.putExtra(SCREEN_PAGE,0);
            startActivity(intent);

        });
        drawRoute.setOnClickListener(view -> {
            Intent intent = new Intent(MainScreenActivity.this, FindLocationActivity.class);
            startActivity(intent);
        });
    }
}
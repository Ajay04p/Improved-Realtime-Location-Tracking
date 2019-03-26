package com.umbc.mc.assignment2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.umbc.mc.conversionLib.DegreeCoordinate;
import com.umbc.mc.conversionLib.EarthCalc;
import com.umbc.mc.conversionLib.Point;

import java.security.Provider;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, SensorEventListener {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private Button getCurrentLocationButton, secondButton;
    private double currentLat;
    private double currentLng;
    private String displayString;
    private TextView t,t2;
    private boolean startAccMagFlag = true;
    private LatLng preLoc;
    private float preAcrcy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        getCurrentLocationButton = findViewById(R.id.FirstButton);
        secondButton = findViewById(R.id.SecondButton);
        t = findViewById(R.id.t);
        t2 = findViewById(R.id.t2);
        //Request for user Permission for location data


        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        getPermission();
        getCurrentLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLocationService();
            }
        });

        secondButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(startAccMagFlag){
                   registerSensorListener();
                   secondButton.setText("Stop ACC-MAG");
                   startAccMagFlag = false;
               }else{
                   unregisterSensorListener();
                   secondButton.setText("Start ACC-MAG");
                   startAccMagFlag = true;
               }
            }
        });

        this.mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        acc = this.mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mag = this.mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

    }

    public void registerSensorListener(){
        mSensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void unregisterSensorListener(){
        mSensorManager.unregisterListener(this);
    }

    public void getPermission(){
        try {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int hasReadContactPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
                if (hasReadContactPermission != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            101);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    private BitmapDescriptor markerDescriptor;
    private int accuracyStrokeColor = Color.argb(255, 130, 182, 228);
    private int accuracyFillColor = Color.argb(100, 130, 182, 228);

    private Marker positionMarker;
    private Circle accuracyCircle;


    public void startLocationService(){

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10 , this);
    }

    public void stopLocationService(){
        locationManager.removeUpdates(this);
    }
    @Override
    public void onLocationChanged(Location location) {
        unregisterSensorListener();
        startAccMagFlag = true;
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        float accuracy = location.getAccuracy();
        preLoc = new LatLng(latitude, longitude);
        preAcrcy = accuracy;
        displayLocation(latitude,longitude,accuracy);
        if(startAccMagFlag) {
            registerSensorListener();
            startAccMagFlag = false;
        }
    }

    public void displayLocation(double latitude, double longitude, double accuracy){
        if (positionMarker != null) {
            positionMarker.remove();
        }
        final MarkerOptions positionMarkerOptions = new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .icon(markerDescriptor)
                .anchor(0.5f, 0.5f);
        positionMarker = mMap.addMarker(positionMarkerOptions);

        if (accuracyCircle != null) {
            accuracyCircle.remove();
        }
        final CircleOptions accuracyCircleOptions = new CircleOptions()
                .center(new LatLng(latitude, longitude))
                .radius(accuracy)
                .fillColor(accuracyFillColor)
                .strokeColor(accuracyStrokeColor)
                .strokeWidth(2.0f);
        accuracyCircle = mMap.addCircle(accuracyCircleOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude),20));
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(20), 5000, null);

    }

    Sensor mag, acc;
    SensorManager mSensorManager;

    float NS2S = 1000000000;
    float displacementX = 0;
    float prevVelocityX = 0;
    float currVelocityX = 0;
    float displacementY = 0;
    float prevVelocityY = 0;
    float currVelocityY = 0;
    SensorData prevAccReadingX = new SensorData(0, 0l);
    SensorData currAccReadingX = new SensorData(0, 0l);
    SensorData prevAccReadingY = new SensorData(0, 0l);
    SensorData currAccReadingY = new SensorData(0, 0l);

    float count = 0;
    int samples = 100;
    int zeroSampleCount = 2;
    int zeroCountX = 0;
    int zeroCountY = 0;
    float heading = 0;
    int headingSamples = 0;
    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ORIENTATION:
                //mag_data = event.values.clone();
                heading = Math.round(event.values[0]);

                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                currAccReadingX.value += event.values[0];
                currAccReadingY.value += event.values[1];
                currAccReadingX.timestamp = event.timestamp;
                count += 1;
                break;
        }
        if(count == samples) {

            currAccReadingX.value = formatNumber(currAccReadingX.value / samples);
            currAccReadingY.value = formatNumber(currAccReadingY.value / samples);
            headingSamples = 0;

            if (currAccReadingX.value <= 0.1f && currAccReadingX.value >= -0.1f) {
                currAccReadingX.value = 0;
            }
            if (currAccReadingY.value <= 0.1f && currAccReadingY.value >= -0.1f) {
                currAccReadingY.value = 0;
            }

            if (prevAccReadingX.timestamp == 0) {
                prevAccReadingX.timestamp = currAccReadingX.timestamp;
                count = 0;


            } else {

                float dt = currAccReadingX.timestamp - prevAccReadingX.timestamp;
                currVelocityX = prevVelocityX + prevAccReadingX.value +
                        (currAccReadingX.value - prevAccReadingX.value) * 0.5f
                                * (dt / NS2S);
                currVelocityY = prevVelocityY + prevAccReadingY.value +
                        (currAccReadingY.value - prevAccReadingY.value) * 0.5f
                                * (dt / NS2S);

                //displacementX = displacementX + prevVelocityX + (currVelocityX - prevVelocityX) * 0.5f * (dt / NS2S);
                //displacementY = displacementY + prevVelocityY + (currVelocityY - prevVelocityY) * 0.5f * (dt / NS2S);

                displacementX = prevVelocityX + (currVelocityX - prevVelocityX) * 0.5f * (dt / NS2S);
                displacementY = prevVelocityY + (currVelocityY - prevVelocityY) * 0.5f * (dt / NS2S);

                displayString = "X -" + currAccReadingX.value + "\nVelocity- " + formatNumber(currVelocityX) + "\nDisplacement" + formatNumber(displacementX)
                        + "\nY -" + currAccReadingY.value + "\nVelocity- " + formatNumber(currVelocityY) + "\nDisplacement" + formatNumber(displacementY)+ "\n Heading- "+heading;

                prevAccReadingX.value = currAccReadingX.value;
                prevAccReadingY.value = currAccReadingY.value;

                prevAccReadingX.timestamp = currAccReadingX.timestamp;
                prevAccReadingY.timestamp = currAccReadingY.timestamp;

                prevVelocityX = currVelocityX;
                prevVelocityY = currVelocityY;

                if (currAccReadingX.value == 0) {
                    zeroCountX++;
                } else {
                    zeroCountX = 0;
                }
                if (zeroCountX == zeroSampleCount) {
                    prevVelocityX = 0;
                }

                if (currAccReadingY.value == 0) {
                    zeroCountY++;
                } else {
                    zeroCountY = 0;
                }
                if (zeroCountY == zeroSampleCount) {
                    prevVelocityY = 0;
                }
                count = 0;


                LatLng finalLoc = convertLinearToSpherical(preLoc, heading, displacementX);
                preLoc = convertLinearToSpherical(preLoc, heading, displacementY);
                displayLocation(preLoc.latitude, preLoc.longitude, preAcrcy);
            }

        }

    }

    public float formatNumber(float x){
        //DecimalFormat df = new DecimalFormat();
        DecimalFormat df = new DecimalFormat("###.##");
        String z= df.format(x);
        float v = Float.parseFloat(z);
        return v;
    }
    public LatLng convertLinearToSpherical(LatLng curr, float bearing, float distance){
        EarthCalc ec = new EarthCalc();
        DegreeCoordinate latCoordinate = DegreeCoordinate.fromDegrees(curr.latitude);
        DegreeCoordinate lngCoordinate = DegreeCoordinate.fromDegrees(curr.longitude);
        Point point = Point.at(latCoordinate, lngCoordinate);

        Point newPoint  = ec.pointAt(point,bearing ,distance );
        return new LatLng(newPoint.latitude, newPoint.longitude);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}

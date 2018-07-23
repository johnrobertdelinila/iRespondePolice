package com.example.oteptudlong.irespondepolice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import dmax.dialog.SpotsDialog;

public class RespondActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GeoQueryEventListener {

    // Permission Request
    private static final int PERMISSION_REQUEST_CODE = 1996;
    private static final int GOOGLE_PLAY_SERVICES_REQUEST_CODE = 1997;
    private static final int CALL_REQUEST_CODE = 1998;

    // Data
    public Report report;
    private static final String TAG = "RespondActivity";
    private boolean isBottomSheetCollapsed = true, isBottomSheetHidden = false;
    private String policeName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";
    private String citizenPhoneStr = null;

    // Location
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location mLastLocation;
    private LatLng destination;

    // Widgets
    public GoogleMap mMap;
    private Marker markerOrigin;
    private Polyline polyline;
    public LinearLayout bottomSheetLayout;
    public BottomSheetBehavior bottomSheetBehavior;
    public Button btn_send_report, btn_fake_report, btn_respond;
    private SpotsDialog loadingDialog;
    private RelativeLayout relativeLayout;
    private FloatingActionButton fab;
    private ImageView imageCar;
    private Handler handler;
    private TextView citizenName, citizenPhone, incidentName, incidentStatus, incidentDesc, distanceKm;
    private LinearLayout actionContainer;
    private LinearLayout imageContainer;
    private HorizontalScrollView horizontalScroll;
    private LinearLayout linearSMS;
    private LinearLayout callCitizen;

    // Firebase
    private DatabaseReference mCitizenReport = FirebaseDatabase.getInstance().getReference().child("Citizen Reports");
    private DatabaseReference mCitizen = FirebaseDatabase.getInstance().getReference().child("Citizens");
    private DatabaseReference mPoliceReport = FirebaseDatabase.getInstance().getReference().child("Police Reports");
    private DatabaseReference mPoliceLocation = FirebaseDatabase.getInstance().getReference().child("Police Locations");
    private GeoFire geoPoliceLocation;
    private String policeUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respond);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        init();
        setUpLocation();

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        imageCar.animate().scaleX(0).scaleY(0).setDuration(0).start();
        handler = new Handler();
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    if (isBottomSheetHidden) {
                        fab.animate().scaleX(0).scaleY(0).setDuration(150).start();
                    }
                    handler.postDelayed(() -> imageCar.animate().scaleX(1).scaleY(1).setDuration(300).start(), 150);
                    isBottomSheetCollapsed = false;
                    isBottomSheetHidden = false;
                }else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    isBottomSheetCollapsed = true;
                    isBottomSheetHidden = false;
                    imageCar.animate().scaleX(0).scaleY(0).setDuration(300).start();
                }else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    isBottomSheetHidden = true;
                    fab.animate().scaleX(1).scaleY(1).setDuration(300).start();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (!isBottomSheetHidden) {
                    fab.animate().scaleX(1 - slideOffset).scaleY(1 - slideOffset).setDuration(0).start();
                }
                if (!isBottomSheetCollapsed) {
                    imageCar.animate().scaleX(0 + slideOffset).scaleY(0 + slideOffset).setDuration(0).start();
                }
            }
        });
        fab.setOnClickListener(v -> toggleBottomSheet());

        btn_send_report.setOnClickListener(v -> checkPoliceReportForSending());
        btn_fake_report.setOnClickListener(v -> checkPoliceReportForFake());
        btn_respond.setOnClickListener(v -> policeRespond("on the way"));
        relativeLayout.setOnClickListener(v -> toggleBottomSheet());
        linearSMS.setOnClickListener(v -> sendSMStoCitizen());
        callCitizen.setOnClickListener(view -> {
            if (citizenPhoneStr != null) {
                // make a phone call
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.CALL_PHONE
                    }, CALL_REQUEST_CODE);
                }
                makePhoneCall();
            }else {
                Toast.makeText(this, "Citizen doesn't have Phone Number.", Toast.LENGTH_LONG).show();
            }
        });

        getCitizenDetails();

        // Listeners
        policeRespondentListener();
        checkPoliceRespond();
        checkPoliceReportForButton();

        if (report.getImages() != null) {
            Log.e("IMAGES", "May mga nakitang image");
            horizontalScroll.setVisibility(HorizontalScrollView.VISIBLE);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            layoutParams.setMargins(10, 10, 10, 0);
            for (String key_image: report.getImages().keySet()) {
                final String imageUrl = report.getImages().get(key_image);
                Log.e("IMAGES", imageUrl);

                final SquareImageView squareImageView = new SquareImageView(RespondActivity.this);
                squareImageView.setLayoutParams(layoutParams);

//                squareImageView.setImageDrawable(getResources().getDrawable(R.drawable.no_image));
                if (imageUrl.startsWith("gs://")) {
                    StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                    storageReference.getDownloadUrl().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String downloadUrl = task.getResult().toString();
                            Picasso.get().load(downloadUrl).placeholder(R.drawable.loading_image).into(squareImageView);
                        }else {
                            Toast.makeText(RespondActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }else {
                    Picasso.get().load(imageUrl).placeholder(R.drawable.loading_image).into(squareImageView);
                }

                imageContainer.addView(squareImageView);
            }
        }else {
            Log.e("IMAGES", "No images found");
        }

    }

    private void makePhoneCall() {
        if (citizenPhoneStr != null) {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + citizenPhoneStr));
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Toast.makeText(this, "Please allow the permission in settings to make a phone call.", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, "Making a phone call to Citizen.", Toast.LENGTH_SHORT).show();
            startActivity(intent);
        }else {
            Toast.makeText(this, "Citizen doesn't have Phone Number.", Toast.LENGTH_LONG).show();
        }
    }

    private void sendSMStoCitizen() {
        String message = "Police text message.";
        if (citizenPhoneStr != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // At least KitKat
                Intent smsMsgAppVar = new Intent(Intent.ACTION_VIEW);
                smsMsgAppVar.setData(Uri.parse("sms:" + citizenPhoneStr));
                smsMsgAppVar.putExtra("sms_body", message);
                startActivity(smsMsgAppVar);

            } else {
                // For early versions, do what worked for you before.
                Intent smsIntent = new Intent(android.content.Intent.ACTION_VIEW);
                smsIntent.setType("vnd.android-dir/mms-sms");
                smsIntent.putExtra("address", citizenPhoneStr);
                smsIntent.putExtra("sms_body", message);
                startActivity(smsIntent);
            }
        }
    }

    private void getCitizenDetails() {
        mCitizen.child(report.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Citizen citizen = dataSnapshot.getValue(Citizen.class);
                if (citizen != null) {
                    citizenName.setText(citizen.getDisplayName());
                    citizenPhoneStr = citizen.getPhoneNumber();
                    citizenPhone.setText(citizenPhoneStr);
                    incidentName.setText(report.getIncident());
                    String status;
                    int color;
                    if (report.getStatus().equals("faked")) {
                        status = "Faked";
                        color = getResources().getColor(R.color.cardStatusPending);
                    }else if (report.getStatus().equals("resolved")) {
                        status = "Resolved";
                        color = getResources().getColor(R.color.cardStatusResolved);
                    }else {
                        status = "Unresolve";
                        color = getResources().getColor(R.color.cardStatusPending);
                    }
                    incidentStatus.setText(status);
                    incidentStatus.setTextColor(color);
                    if (report.getDescription() != null) {
                        incidentDesc.setText(report.getDescription());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(RespondActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleBottomSheet() {

        if (isBottomSheetCollapsed) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

    }

    private void policeRespondentListener() {
        GeoLocation geoLocation = new GeoLocation(report.getLocation_latlng().get("latitude"), report.getLocation_latlng().get("longtitude"));
        GeoQuery geoQuery = geoPoliceLocation.queryAtLocation(geoLocation, 0.6);
        geoQuery.addGeoQueryEventListener(this);

        Log.e("Location of Incident", String.valueOf(geoLocation));
    }

    private void init() {
        report = (Report) getIntent().getSerializableExtra("report");
        bottomSheetLayout = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        btn_send_report = findViewById(R.id.btn_send_report);
        btn_fake_report = findViewById(R.id.btn_fake_report);
        loadingDialog = new SpotsDialog(this);
        btn_respond = findViewById(R.id.btn_respond);
        geoPoliceLocation = new GeoFire(mPoliceLocation.child(policeUid));
        destination = new LatLng(report.getLocation_latlng().get("latitude"), report.getLocation_latlng().get("longtitude"));
        relativeLayout = findViewById(R.id.relativeLayout);
        fab = findViewById(R.id.fab);
        imageCar = findViewById(R.id.imageCar);
        citizenName = findViewById(R.id.text_citizen_name);
        citizenPhone = findViewById(R.id.text_citizen_phone);
        incidentName = findViewById(R.id.text_incident_name);
        incidentStatus = findViewById(R.id.text_incident_status);
        incidentDesc = findViewById(R.id.text_incident_desc);
        distanceKm = findViewById(R.id.text_km);
        actionContainer = findViewById(R.id.action_container);
        imageContainer = findViewById(R.id.imageContainer);
        horizontalScroll = findViewById(R.id.horizontalScroll);
        linearSMS = findViewById(R.id.linearSMS);
        callCitizen = findViewById(R.id.layout_call_citizen);
    }

    private void checkPoliceReportForSending() {
        mCitizenReport.child(report.getKey()).child("policeReports").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChild(policeUid)) {
                    for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                        if (childSnapshot.getKey().equals(policeUid)) {
                            String policeReportID = childSnapshot.getValue(String.class);
                            fetchPoliceReport(policeReportID);
                            break;
                        }
                    }
                }else {
                    Intent intent = new Intent(RespondActivity.this, PoliceReportActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("key", report.getKey());
                    bundle.putDouble("latitude", (Double) report.getLocation_latlng().get("latitude"));
                    bundle.putDouble("longtitude", (Double) report.getLocation_latlng().get("longtitude"));
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(RespondActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPoliceReportForFake() {
        mCitizenReport.child(report.getKey()).child("policeReports").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(policeUid)) {
                    // Already sended a report
                    // Ask first to remove the current police report
                    // TODO: Alert Dialog the confirmation to remove the report
                    showDeletePoliceReport(dataSnapshot);
                }else {
                    updateReportStatus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showDeletePoliceReport(DataSnapshot dataSnapshot) {
        for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
            if (childSnapshot.getKey().equals(policeUid)) {
                String policeReportID = childSnapshot.getValue(String.class);
                removePoliceReport(policeReportID);
                break;
            }
        }
    }

    private void updateReportStatus() {
        HashMap<String, Object> status = new HashMap<>();
        status.put("status", "faked");
        mCitizenReport.child(report.getKey()).updateChildren(status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RespondActivity.this, "Considered as fake report.", Toast.LENGTH_SHORT).show();
                    // Add fake report to citizen
//                        mCitizen.child(report.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(DataSnapshot dataSnapshot) {
//                                if (dataSnapshot.hasChild("fakeReports")) {
//
//                                }
//                            }
//
//                            @Override
//                            public void onCancelled(DatabaseError databaseError) {
//
//                            }
//                        });
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private void removePoliceReport(String policeReportID) {
        mPoliceReport.child(policeReportID).removeValue()
                .addOnSuccessListener(aVoid -> mCitizenReport.child(report.getKey()).child("policeReports").child(policeUid).removeValue()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                updateReportStatus();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        }))
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(RespondActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchPoliceReport(String policeReportID) {
        mPoliceReport.child(policeReportID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                PoliceReport policeReport = dataSnapshot.getValue(PoliceReport.class);
                if (policeReport != null) {
                    Intent intent = new Intent(RespondActivity.this, ShowReportActivity.class);
                    intent.putExtra("policeReport", policeReport);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void checkPoliceReportForButton() {
        mCitizenReport.child(report.getKey()).child("policeReports").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(policeUid)) {
                    btn_send_report.setText("VIEW REPORT");
                }else {
                    btn_send_report.setText("WRITE REPORT");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void checkPoliceRespond() {
        mCitizenReport.child(report.getKey()).child("policeRespondee").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(policeUid)) {
                    for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                        // Check if your already on the way
                        if (childSnapshot.getKey().equals(policeUid)) {
                            // Police is on the way
                            String policeRespond = childSnapshot.child("police_respond").getValue(String.class);
                            btn_respond.setText("RESPONDING...");
                            btn_respond.animate().scaleX(0).scaleY(0).setDuration(150).start();
                            actionContainer.animate().scaleX(1).scaleY(1).setDuration(150).start();
                            actionContainer.bringToFront();
                            Log.e("YOUR RESPOND", policeRespond);
                            if (policeRespond != null && policeRespond.equals("arrived")) {
                                // TODO: Hide the Respond Button and Show the Write and Fake Button
                                btn_send_report.setEnabled(true);
                                btn_fake_report.setEnabled(true);
                            }
                            break;
                        }
                    }
                }else {
                    btn_respond.setText("RESPOND");
                    // TODO: Hide the Show and Write Button and Show the Respond Button
                    actionContainer.animate().scaleX(0).scaleY(0).setDuration(150).start();
                    btn_send_report.setEnabled(false);
                    btn_fake_report.setEnabled(false);
                    btn_respond.animate().scaleX(1).scaleY(1).setDuration(150).start();
                    btn_respond.bringToFront();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(RespondActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RespondActivity.this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        } else {
            if (isGooglePlayServices()) {
                buildGoogleApiClient();
                setUpLocationRequest();
            }
        }
    }

    private void setUpLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(500);
    }

    private boolean isGooglePlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(RespondActivity.this);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(RespondActivity.this, status, GOOGLE_PLAY_SERVICES_REQUEST_CODE).show();
            } else {
                Toast.makeText(this, "Device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        } else {
            return true;
        }
    }

    private void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(RespondActivity.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add Marker
        MarkerOptions markerOptionsDest = new MarkerOptions();
        markerOptionsDest.position(destination);
        markerOptionsDest.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        markerOptionsDest.title(report.getIncident());
        mMap.addMarker(markerOptionsDest);

        // Add Radius
        double radiusInMeter = 300; // 0.6 km
        int strokeColor = 0xffff0000;
        int shadeColor = 0x44ff0000;
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(destination);
        circleOptions.radius(radiusInMeter);
        circleOptions.fillColor(shadeColor);
        circleOptions.strokeColor(strokeColor);
        circleOptions.strokeWidth(4);
        mMap.addCircle(circleOptions);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        showLocation();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private void showLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (mLastLocation != null) {

            Location reportLocation = new Location("");
            reportLocation.setLatitude(report.getLocation_latlng().get("latitude"));
            reportLocation.setLongitude(report.getLocation_latlng().get("longtitude"));

            float distance = (float) (mLastLocation.distanceTo(reportLocation) * 0.001);
            DecimalFormat f = new DecimalFormat("##.00");

            String distanceInKilometer;
            if (distance == 0) {
                distanceInKilometer = "0km";
            }else {
                distanceInKilometer = f.format(distance) + "km";
            }
            distanceKm.setText(distanceInKilometer);

            final LatLng origin = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

            geoPoliceLocation.setLocation("Sakurako Oharo", new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {

                    if (error == null) {

                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(origin);
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                        markerOptions.title("Your location");

                        if (markerOrigin != null) {
                            markerOrigin.remove();
                        }
                        markerOrigin = mMap.addMarker(markerOptions);

                        String url = getUrl(origin, destination);
                        FetchUrl FetchUrl = new FetchUrl();
                        FetchUrl.execute(url);

                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(origin);
                        builder.include(destination);
                        LatLngBounds bounds = builder.build();

                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 240));

                    }else {
                        Toast.makeText(RespondActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }
            });

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        showLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isGooglePlayServices()) {
                    buildGoogleApiClient();
                    setUpLocationRequest();
                    showLocation();
                }
            }else {
                Toast.makeText(this, "Please allow the permission request in settings.", Toast.LENGTH_SHORT).show();
            }
        }else if (requestCode == CALL_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makePhoneCall();
            }else {
                Toast.makeText(this, "You can allow the permission request in the settings.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        String key = "key=AIzaSyC1_6q8cmLER1jrwvhb5dz3L0wXelRXXbA";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        Log.e("ENTERED", "ENTERED");
        arriveRespond();
    }

    @Override
    public void onKeyExited(String key) {
        Log.e("EXITED", "EXITED");
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        Log.e("MOVED", "MOVED");
        arriveRespond();
    }

    @Override
    public void onGeoQueryReady() {
//        Toast.makeText(this, "GeoQueryReady!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Log.e("GEO ERROR", error.getMessage());
    }

    private void arriveRespond() {
        // Check first if the police is already responding
        mCitizenReport.child(report.getKey()).child("policeRespondee")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(policeUid)) {
                            // Police is on the way
                            // Update it in arrived
                            policeRespond("arrived");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void sendRespond(final String status) {
        PoliceRespondee policeRespondee = new PoliceRespondee(policeName, status);
        mCitizenReport.child(report.getKey()).child("policeRespondee").child(policeUid).setValue(policeRespondee)
                .addOnSuccessListener(aVoid -> {
                    if (status.equals("on the way")) {
                        Toast.makeText(RespondActivity.this, "Your respond is on the way", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(RespondActivity.this, "You have arrived in the destination of incident", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(RespondActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void policeRespond(final String status) {
        mCitizenReport.child(report.getKey()).child("policeRespondee").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(policeUid)) {
                    boolean notResponded = true;
                    for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                        if (childSnapshot.getKey().equals(policeUid) && childSnapshot.child("police_respond").getValue(String.class).equals(status)) {
                            notResponded = false;
                            break;
                        }
                    }
                    if (notResponded) {
                        sendRespond(status);
                    }
                }else {
                    sendRespond(status);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(RespondActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask",jsonData[0].toString());
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask","Executing routes");
                Log.d("ParserTask",routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask",e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.RED);

                Log.d("onPostExecute","onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {
                if (polyline != null) {
                    polyline.remove();
                }
                polyline = mMap.addPolyline(lineOptions);
            }
            else {
                Log.d("onPostExecute","without Polylines drawn");
            }
        }
    }

    public class DataParser {

        /** Receives a JSONObject and returns a list of lists containing latitude and longitude */
        public List<List<HashMap<String,String>>> parse(JSONObject jObject){

            List<List<HashMap<String, String>>> routes = new ArrayList<>() ;
            JSONArray jRoutes;
            JSONArray jLegs;
            JSONArray jSteps;

            try {

                jRoutes = jObject.getJSONArray("routes");

                /** Traversing all routes */
                for(int i=0;i<jRoutes.length();i++){
                    jLegs = ( (JSONObject)jRoutes.get(i)).getJSONArray("legs");
                    List path = new ArrayList<>();

                    /** Traversing all legs */
                    for(int j=0;j<jLegs.length();j++){
                        jSteps = ( (JSONObject)jLegs.get(j)).getJSONArray("steps");

                        /** Traversing all steps */
                        for(int k=0;k<jSteps.length();k++){
                            String polyline = "";
                            polyline = (String)((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);

                            /** Traversing all points */
                            for(int l=0;l<list.size();l++){
                                HashMap<String, String> hm = new HashMap<>();
                                hm.put("lat", Double.toString((list.get(l)).latitude) );
                                hm.put("lng", Double.toString((list.get(l)).longitude) );
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }catch (Exception e){
            }


            return routes;
        }


        /**
         * Method to decode polyline points
         * Courtesy : https://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
         * */
        private List<LatLng> decodePoly(String encoded) {

            List<LatLng> poly = new ArrayList<>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((((double) lat / 1E5)),
                        (((double) lng / 1E5)));
                poly.add(p);
            }

            return poly;
        }
    }

}

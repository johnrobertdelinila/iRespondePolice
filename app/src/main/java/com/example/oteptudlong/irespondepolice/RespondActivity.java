package com.example.oteptudlong.irespondepolice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import dmax.dialog.SpotsDialog;

public class RespondActivity
        extends AppCompatActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, GeoQueryEventListener {

    public GoogleMap mMap;
    public Report report;
    private static final int PERMISSION_REQUEST_CODE = 1996;
    private static final int GOOGLE_PLAY_SERVICES_REQUEST_CODE = 1997;
    private static final String TAG = "RespondActivity";

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location mLastLocation;
    private LatLng destination;

    private Marker markerOrigin, markerDest;
    private Polyline polyline;

    public LinearLayout bottomSheetLayout;
    public BottomSheetBehavior bottomSheetBehavior;

    public Button btn_send_report, btn_fake_report, btn_respond;
    private DatabaseReference mCitizenReport = FirebaseDatabase.getInstance().getReference().child("Citizen Reports");
    private DatabaseReference mCitizen = FirebaseDatabase.getInstance().getReference().child("Citizens");
    private DatabaseReference mPoliceReport = FirebaseDatabase.getInstance().getReference().child("Police Reports");
    private DatabaseReference mPoliceLocation = FirebaseDatabase.getInstance().getReference().child("Police Locations");
    private GeoFire geoPoliceLocation;
    private SpotsDialog loadingDialog;
    private String policeUid = "police_id";

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

        btn_send_report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPoliceReportForSending();
            }
        });
        btn_fake_report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPoliceReportForFake();
            }
        });
        btn_respond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                policeRespond("on the way");
            }
        });

        // Listeners
        policeRespondentListener();
        checkPoliceRespond();
        checkPoliceReportForButton();

        // TODO: Fake report button and Write report button is hide, Respond button is show

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
    }

    private void checkPoliceReportForSending() {
        mCitizenReport.child(report.getKey()).child("policeReports").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

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
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(RespondActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPoliceReportForFake() {
        mCitizenReport.child(report.getKey()).child("policeReports").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
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
            public void onCancelled(DatabaseError databaseError) {

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
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
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
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private void removePoliceReport(String policeReportID) {
        mPoliceReport.child(policeReportID).removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mCitizenReport.child(report.getKey()).child("policeReports").child(policeUid).removeValue()
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
                                });
                    }
                })
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
                        // TODO: Must get your own UID
                        if (childSnapshot.getKey().equals(policeUid)) {
                            // Police is on the way
                            String policeRespond = childSnapshot.child("police_respond").getValue(String.class);
                            btn_respond.setText("RESPONDING...");
                            Log.e("YOUR RESPOND", policeRespond);
                            if (policeRespond.equals("arrived")) {
                                // TODO: Hide the Respond Button and Show the Write and Fake Button
                            }
                            break;
                        }
                    }
                }else {
                    btn_respond.setText("RESPOND");
                    // TODO: Hide the Show and Write Button and Show the Respond Button
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

        MarkerOptions markerOptionsDest = new MarkerOptions();
        markerOptionsDest.position(destination);
        markerOptionsDest.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        markerOptionsDest.title(report.getIncident());
        markerDest = mMap.addMarker(markerOptionsDest);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        setLocation();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private void setLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (mLastLocation != null) {

            final LatLng origin = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

            geoPoliceLocation.setLocation("Sakurako Oharo", new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {

                    if (error == null) {

                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(origin);
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        markerOptions.title("Your location");

                        if (markerOrigin != null) {
                            markerOrigin.remove();
                        }
                        markerOrigin = mMap.addMarker(markerOptions);

                        String url = getUrl(origin, destination);
                        FetchUrl FetchUrl = new FetchUrl();
                        FetchUrl.execute(url);

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, 17.0f));

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
        setLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isGooglePlayServices()) {
                    buildGoogleApiClient();
                    setUpLocationRequest();
                    setLocation();
                }
            }else {
                Toast.makeText(this, "Please allow the permission request in settings.", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "GeoQueryReady!", Toast.LENGTH_SHORT).show();
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
        // TODO: Get the police name
        PoliceRespondee policeRespondee = new PoliceRespondee("John Delinila", status);
        mCitizenReport.child(report.getKey()).child("policeRespondee").child(policeUid).setValue(policeRespondee)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        if (status.equals("on the way")) {
                            Toast.makeText(RespondActivity.this, "Your respond is on the way", Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(RespondActivity.this, "You have arrived in the destination of incident", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(RespondActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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

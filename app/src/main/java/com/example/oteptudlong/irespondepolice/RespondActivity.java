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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
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

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location mLastLocation;

    private Marker markerOrigin, markerDest;
    private Polyline polyline;

    public LinearLayout bottomSheetLayout;
    public BottomSheetBehavior bottomSheetBehavior;

    public Button btn_send_report, btn_fake_report, btn_respond;
    private DatabaseReference mCitizenReport = FirebaseDatabase.getInstance().getReference().child("Citizen Report");
    private DatabaseReference mCitizen = FirebaseDatabase.getInstance().getReference().child("Citizen");
    private DatabaseReference mReportLocation = FirebaseDatabase.getInstance().getReference().child("Report Location");
    private SpotsDialog loadingDialog;
    private static final String randomStringSeparator = "eISN3K053y";
    private String policeUid = "police_id";
    private Boolean isOnTheWay = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respond);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        init();

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        btn_send_report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RespondActivity.this, PoliceReportActivity.class);
                intent.putExtra("key", report.getKey());
                intent.putExtra("latitude", report.getLatitude());
                intent.putExtra("longtitude", report.getLongtitude());
                startActivity(intent);
            }
        });

        btn_fake_report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                showFakeConfirmationDialog();
                addFakeReport();
            }
        });

        btn_respond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                willRespond();
            }
        });

        Log.e("CITIZEN ID", report.getUid());

        setUpLocation();
//        buttonListener();
        respondentListener();
        otwListener();

    }

    private void showFakeConfirmationDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
    }

    private void respondentListener() {
        GeoFire geoFire = new GeoFire(mReportLocation.child("-LBeUnsrKqaT8Oyp53c_"));
        GeoLocation geoLocation = new GeoLocation(report.getLatitude(), report.getLongtitude());
        GeoQuery geoQuery = geoFire.queryAtLocation(geoLocation, 0.6);
        geoQuery.addGeoQueryEventListener(this);

        Log.e("LOCATION OF INCIDENT", String.valueOf(geoLocation));
    }

    private void addFakeReport() {
        loadingDialog.show();
        mCitizen.child(report.getUid()).child("fake_reports")
                .runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        int fake_reports = 0;
                        if (mutableData.getValue() != null) {
                            fake_reports = mutableData.getValue(int.class);
                        }
                        fake_reports++;
                        mutableData.setValue(fake_reports);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean isComplete, DataSnapshot dataSnapshot) {
                        if (databaseError != null) {
                            loadingDialog.dismiss();
                            Toast.makeText(RespondActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("FAKE REPORT ERR", databaseError.getMessage());
                        }else {
                            loadingDialog.dismiss();
                            Toast.makeText(RespondActivity.this, "Considered as fake news", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void init() {
        report = (Report) getIntent().getSerializableExtra("report");
        bottomSheetLayout = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        btn_send_report = findViewById(R.id.btn_send_report);
        btn_fake_report = findViewById(R.id.btn_fake_report);
        loadingDialog = new SpotsDialog(this);
        btn_respond = findViewById(R.id.btn_respond);
    }

    private void otwListener() {
        mCitizenReport.child(report.getKey()).child("otwIds").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String otw_ids = dataSnapshot.getValue(String.class);
                if (otw_ids != null) {
                    checkOtwIDs(otw_ids);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("OTW LISTENER err", databaseError.getMessage());
            }
        });
    }

    private void checkOtwIDs(String otwIDs) {
        List<String> ids = Arrays.asList(otwIDs.split(randomStringSeparator));
        if (ids.contains(policeUid)) {
            Log.e("ON THE WAY", "YES");
            btn_respond.setText("RESPONDING...");
            isOnTheWay = true;
        }else {
            Log.e("ON THE WAY", "NO");
            btn_respond.setText("RESPOND");
            isOnTheWay = false;
        }
    }

    private void buttonListener() {
        mCitizenReport.child(report.getKey()).child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String status = dataSnapshot.getValue(String.class);
                if (status != null) {
                    if (status.equals("resolved")) {
                        btn_send_report.setText("VIEW REPORT");
                    }else if (status.equals("pending")) {
                        btn_send_report.setText("WRITE REPORT");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("BTN_LISTENER ERR", databaseError.getMessage());
            }
        });
    }

    private void willRespond() {
        loadingDialog.show();
        mCitizenReport.child(report.getKey()).child("otwIds")
                .runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        String otwIds = "null";
                        if (mutableData.getValue() != null) {
                            otwIds = combinePoliceIDs(mutableData.getValue(String.class), policeUid);
                        }
                        mutableData.setValue(otwIds);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                        if (databaseError != null) {
                            loadingDialog.dismiss();
                            Log.e("ERRor", databaseError.getMessage());
                            Toast.makeText(RespondActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }else {
                            loadingDialog.dismiss();
                            Toast.makeText(RespondActivity.this, "Your are now currently responding to the incident", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private String combinePoliceIDs(String currentPoliceIDs, String newPoliceID){
        StringBuilder output = new StringBuilder();
        if (currentPoliceIDs.equals("null")) {
            output.append(newPoliceID);
        }else {
            output.append(currentPoliceIDs);
            output.append(randomStringSeparator);
            output.append(newPoliceID);
        }
        return output.toString();
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
            if (markerOrigin != null) {
                markerOrigin.remove();
            }

            LatLng origin = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(origin);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            markerOptions.title("Your location");
            markerOrigin = mMap.addMarker(markerOptions);

            if (markerDest != null) {
                markerDest.remove();
            }
            LatLng dest = new LatLng(report.getLatitude(), report.getLongtitude());
            MarkerOptions markerOptionsDest = new MarkerOptions();
            markerOptionsDest.position(dest);
            markerOptionsDest.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            markerOptionsDest.title("Your Destination");
            markerDest = mMap.addMarker(markerOptionsDest);

            String url = getUrl(origin, dest);
            FetchUrl FetchUrl = new FetchUrl();
            FetchUrl.execute(url);

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, 17.0f));

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
        // MAGPAPAKITA UNG WRITE REPORT AT FAKE REPORT BUTTON
        // MAWAWALA UNG REPOND BUTTON
        // PERO DAPAT NASA ON THE WAY SIANG ID MUNA
        haveArrived();
    }

    @Override
    public void onKeyExited(String key) {
        Log.e("EXITED", "EXITED");
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        Log.e("MOVED", "MOVED");
        // MAGPAPAKITA UNG WRITE REPORT AT FAKE REPORT BUTTON
        // MAWAWALA UNG REPOND BUTTON
        // PERO DAPAT NASA ON THE WAY SIANG ID MUNA
    }

    @Override
    public void onGeoQueryReady() {
        Toast.makeText(this, "OKAY!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Log.e("GEO ERROR", error.getMessage());
    }

    private void haveArrived() {
        mCitizenReport.child(report.getKey()).child("arrivedIds").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (isOnTheWay != null && isOnTheWay) {
                    // check pa kung meron na ung id niya sa arrived id para hindi maulit
                    mCitizenReport.child(report.getKey()).child("arrivedIds").runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            String arrived_ids = "null";
                            if (mutableData.getValue() != null) {
                                String current_ids = mutableData.getValue(String.class);
                                arrived_ids = combinePoliceIDs(current_ids, policeUid);
                            }
                            mutableData.setValue(arrived_ids);
                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                            if (databaseError != null) {
                                Log.e("ERROR", databaseError.getMessage());
                                Toast.makeText(RespondActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }else {
                                Toast.makeText(RespondActivity.this, "You have arrived on the incident", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

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

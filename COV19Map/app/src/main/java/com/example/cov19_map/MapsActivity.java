package com.example.cov19_map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.UrlTileProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_CODE =911;
    private GoogleMap mMap;
    Location myLocation;
    LatLng userLocation;
    List<AuthUI.IdpConfig> providers;
    private String jsonUrl = "https://disease.sh/v2/countries?yesterday=false";
    LocationManager locationManager;
    LocationListener locationListener;
    MenuItem userData;

    //On create
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //lista metod logowania
        providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(), //Email
                new AuthUI.IdpConfig.PhoneBuilder().build(), //Phone
                new AuthUI.IdpConfig.GoogleBuilder().build() //Google
        );

        showSignInOptions();
    }

    //Okno metody logowania
    private void showSignInOptions() {
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).setTheme(R.style.AppTheme).setLogo(R.drawable.cov19map).build(),
                REQUEST_CODE
        );
    }

    //Obługa responsa
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            IdpResponse response =IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK){
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                userData.setTitle(user.getDisplayName());
                if (mMap != null){
                    GetCountries getCountries = new GetCountries();
                    getCountries.execute(jsonUrl);
                }
            } else {
                Toast.makeText(this, ""+response.getError().getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    //Obsługa okna dialogowego
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //Yes button clicked
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        }
    };

    //On map ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        modifySnippet();

        if (ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this,new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }else{
            if(!mMap.isMyLocationEnabled())
                mMap.setMyLocationEnabled(true);

            LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            myLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (myLocation == null) {
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                String provider = lm.getBestProvider(criteria, true);
                myLocation = lm.getLastKnownLocation(provider);
            }

            if(myLocation!=null){
                userLocation = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 4), 1500, null);
            }
        }
    }

    //modyfikuje domyślny snippet map google
    private void modifySnippet () {
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                LinearLayout info = new LinearLayout(getApplicationContext());
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(getApplicationContext());
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(getApplicationContext());
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });
    }


    //Wywołanie menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        userData = menu.findItem(R.id.userName);
        return true;
    }

    //Obsługa menu
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logOut:
                Toast.makeText(this, "Log out", Toast.LENGTH_SHORT).show();
                AuthUI.getInstance().signOut(MapsActivity.this).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        showSignInOptions();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MapsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //pozwala na wyświetlanie obrazu bez pobierania

    //pobiera dane z Json'a i umieszcza na mapie
    public  class GetCountries extends AsyncTask<String, Void, String>{

        //pobiera dane z Jsona
        @Override
        protected String doInBackground(String... jsonUrls) {
            //Toast.makeText(this, "Trwa pobieranie infromacji", Toast.LENGTH_LONG).show();
            String jsonString="";
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(jsonUrls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                int data = inputStreamReader.read();
                while (data != -1){
                    char current = (char) data;
                    jsonString += current;
                    data = inputStreamReader.read();
                }
                System.out.println("haha");
                return jsonString;
            } catch (Exception e) {
                e.printStackTrace();
        }
            return null;
        }

        //po pobraniu danych umieszcza na mapie
        @Override
        protected void onPostExecute(String jsonString) {
            super.onPostExecute(jsonString);
            Toast.makeText(getApplicationContext(), "Trwa pobieranie obrazów, prosimy o cierpliwość", Toast.LENGTH_LONG).show();
            try {
                JSONArray jsonArray = new JSONArray(jsonString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject country = jsonArray.getJSONObject(i);
                    JSONObject countryInfo = country.getJSONObject("countryInfo");

                    // Koordynaty
                    LatLng currentLatLng = new LatLng(countryInfo.getDouble("lat"), countryInfo.getDouble("long"));

                    DownloadImage downloadImage = new DownloadImage();

                    //88        88  I8,        8        ,8I    db         ,ad8888ba,         db
                    //88        88  `8b       d8b       d8'   d88b       d8"'    `"8b       d88b
                    //88        88   "8,     ,8"8,     ,8"   d8'`8b     d8'                d8'`8b
                    //88        88    Y8     8P Y8     8P   d8'  `8b    88                d8'  `8b
                    //88        88    `8b   d8' `8b   d8'  d8YaaaaY8b   88      88888    d8YaaaaY8b
                    //88        88     `8a a8'   `8a a8'  d8""""""""8b  Y8,        88   d8""""""""8b
                    //Y8a.    .a8P      `8a8'     `8a8'  d8'        `8b  Y8a.    .a88  d8'        `8b
                    //`"Y8888Y"'        `8'       `8'  d8'          `8b  `"Y88888P"  d8'          `8b
                    //Jeżeli chcesz by dane ładowały się szybciej zakomentuj wskazane linie!!
                    Bitmap flaga; // <--
                    flaga = downloadImage.execute(countryInfo.getString("flag")).get(); // <--
                    mMap.addMarker(new MarkerOptions()
                            .position(currentLatLng)
                            .title(country.getString("country"))
                            .icon(BitmapDescriptorFactory.fromBitmap(flaga))// <--
                            .snippet("Przypadki ogółem: "+country.getString("cases")+"\n"+"Przypadki dziś: "+country.getString("todayCases")+"\n"+"Przypadki aktywne: "+country.getString("active")+"\n"+"Stan krytyczny: "+country.getString("critical")+"\n"+"Zgony ogółem: "+country.getString("deaths")+"\n"+"Zgodny dziś: "+country.getString("todayDeaths")+"\n"+"Wyleczono: "+country.getString("recovered"))
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //wyświetla informacje przed wykonaniem kodu
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(getApplicationContext(), "Trwa pobieranie infromacji oraz zdjęć, ten proces może trwać powyżej minuty, prosimy o cierpliwość", Toast.LENGTH_LONG).show();
        }

    }

    //pobiera obrazy
    private class DownloadImage extends AsyncTask <String, Void, Bitmap>{
        @Override
        protected Bitmap doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream inputStream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                Bitmap resized = Bitmap.createScaledBitmap(bitmap, 80,45, true);
                return resized;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }


}

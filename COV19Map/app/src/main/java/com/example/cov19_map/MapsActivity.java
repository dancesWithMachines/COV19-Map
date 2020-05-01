package com.example.cov19_map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.UrlTileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String jsonUrl = "https://disease.sh/v2/countries?yesterday=false";
    LocationManager locationManager;
    LocationListener locationListener;

    //On create
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    //On map ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        modifySnippet();

        GetCountries getCountries = new GetCountries();
        getCountries.execute(jsonUrl);
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
        return true;
    }

    //Obsługa menu
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.showMe:
                Toast.makeText(this, "Znajdź mnie", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.logOut:
                Toast.makeText(this, "Log out", Toast.LENGTH_SHORT).show();
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

                    //!UWAGA!
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

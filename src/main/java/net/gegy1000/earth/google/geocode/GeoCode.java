package net.gegy1000.earth.google.geocode;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class GeoCode {
    private final double latitude;
    private final double longitude;

    private GeoCode(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static GeoCode get(String place) throws IOException {
        String req = "address=" + place.replaceAll(" ", "+");
        URL url = new URL("https://maps.googleapis.com/maps/api/geocode/json?" + req);

        GeoCodeContainer.Location location = new Gson().fromJson(new InputStreamReader(url.openStream()), GeoCodeContainer.class).results[0].geometry.location;

        return new GeoCode(location.lat, location.lng);
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}

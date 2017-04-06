package ch.ethz.ikg.assignment1;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Observable;

/**
 * Created by chsch on 06.04.2017.
 */

public class LocationUpdates extends Observable implements LocationListener {

    // Create Location which stores last known location
    Location oldLoc = null;

    // Create LocationManager to access GPS measurements
    private LocationManager locationManager;

    private ArrayList<String> locationValues = new ArrayList<>();

    protected void onStart(AppCompatActivity activity) {
        for (int i = 0; i <= 5; i++) {
            locationValues.add("N/A");
        }
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            // We don't have the necessary permissions, so we have to request them.
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            // We have the necessary permissions, and can request the
            // location updates.
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 2, this);
        }
    }

    /**
     * This function checks if the app has necessary permissions to access the GPS sensor and
     * requests location updates if it has.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Here we have the permission, and can request the
                    // location updates.
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 2, this);
                }
            }
        }
    }

    protected void onStop() {
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        setChanged();
        locationValues.add(1, String.format("%.5f N, %.5f E", location.getLatitude(), location.getLongitude()));
        // location.getSpeed returns speed in m/s and therefore *3.6 to get speed in km/h
        locationValues.add(2, String.format("%.1f km/h", location.getSpeed() * 3.6));
        locationValues.add(3, String.format("%.2f m/s²", getAcceleration(location)));
        locationValues.add(4, String.format("%.1f m.a.s.l.", location.getAltitude()));
        locationValues.add(5, String.format("%d;%.7f;%.7f", location.getTime(), location.getLatitude(), location.getLongitude()));
        oldLoc = location;
        notifyObservers(locationValues);
    }

    /**
     * This function calculates the acceleration, based on the speed at the current and last location.
     * It denotes the speed difference between the two locations over time.
     *
     * @param location (current location)
     * @return acceleration in m/s^2
     */
    public double getAcceleration(Location location) {
        if (oldLoc != null) {
            double acceleration = ((location.getSpeed() - oldLoc.getSpeed()) / (location.getTime() - oldLoc.getTime()) * 1000);
            return acceleration;
        } else {
            return 0;
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
}

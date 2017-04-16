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

import java.util.Observable;

/**
 * This class implements the LocationListener which receives updates from the GPS sensor. The class
 * implements the Observable interface so that the main activity can observe location changes.
 * The arguments needed in the MainActivity are passed as an String[5] with the form:
 * {Location, Speed, Acceleration, Height, Log} whose values can be directly shown in the
 * interface. Log contains a timestamp, longitude and latitude which can be written to a log
 * file directly.
 */

public class LocationUpdates extends Observable implements LocationListener {

    // Create Location which stores last known location
    Location oldLoc = null;

    // Create LocationManager to access GPS measurements
    private LocationManager locationManager;

    // This Array will be passed to the observer as argument.
    private String[] locationValues = new String[6];

    /**
     * This method is to be called by the MainActivity in its onStart() method.
     * The method creates initializes the arguments that are provided to the observer, creates
     * the locationManager and checks permissions. To do so, the current activity is passed so
     * that the method has the context and necessary information for the permission check.
     *
     * @param activity
     */
    protected void onStart(AppCompatActivity activity) {
        //Initialize Array which is passed to the observer
        for (int i = 0; i < 5; i++) {
            locationValues[i] = "N/A";
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

    /**
     * This function prevents the app from further accessing the GPS sensors if it is stopped.
     */
    protected void onStop() {
        locationManager.removeUpdates(this);
    }

    /**
     * This function handles location changes incoming from the GPS sensor. The current location,
     * altitude and speed are directly displayed to the user. Furthermore, the current location
     * is stored after displaying it. It is used in a with the next location measurement to calculate
     * the acceleration.
     *
     * @param location (current location)
     */
    @Override
    public void onLocationChanged(Location location) {
        // as soon as the location changed, mark this object as changed
        setChanged();
        locationValues[0] = String.valueOf(location.getLatitude());
        locationValues[1] = String.valueOf(location.getLongitude());
        // location.getSpeed returns speed in m/s and therefore *3.6 to get speed in km/h
        locationValues[2] = String.format("%.1f km/h", location.getSpeed() * 3.6);
        locationValues[3] = String.format("%.2f m/s²", getAcceleration(location));
        locationValues[4] = String.format("%.1f m.a.s.l.", location.getAltitude());
        locationValues[5] = String.format("%d;%.7f;%.7f", location.getTime(), location.getLatitude(), location.getLongitude());
        oldLoc = location;
        // notify the observers that the location values have changed
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

    /**
     * Unused function
     *
     * @param provider
     * @param status
     * @param extras
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    /**
     * Unused function
     *
     * @param provider
     */
    @Override
    public void onProviderEnabled(String provider) {

    }

    /**
     * Unused function
     *
     * @param provider
     */
    @Override
    public void onProviderDisabled(String provider) {

    }
}

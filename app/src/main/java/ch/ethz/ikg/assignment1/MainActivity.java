/**
 * Copyright <2017> <ETH Zürich - C.Schoenenberger>
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ch.ethz.ikg.assignment1;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * This application was created in the context of the course Mobile GIS and Location Based Services
 * from the ETH Zurich. It is the first assignment of three and intents to ensure that the learning
 * objectives of the course are met.
 * The aim of the app is to provide bikers with following information:
 * - Heading
 * - Speed
 * - Height
 * - Acceleration
 * - Location
 * - Temperature
 * To obtain this information, GPS-, acceleration-, magnetic field- and temperature sensor are
 * accessed and processed. Furthermore, the recording of a GPS track with storage into a CSV file
 * is possible.
 *
 * A drawback of the application is, that the LocationListener and the SensorEventListener are
 * included in the main activity. For the sake of clarity and clean design, they should be
 * implemented in individual classes which can then be accessed by the main activity. This
 * should be implemented in a next step.
 *
 * @author Christoph Schönenberger
 * @version 1.1
 * @since 04.04.2017
 */

public class MainActivity extends AppCompatActivity implements LocationListener, SensorEventListener {

    // the ALPHA value is needed for the low-pass filter
    static final float ALPHA = 0.25f;
    // Message to be displayed if sensor is not available
    private final static String NOT_SUPPORTED = "Sensor not available";
    // Create arrays to store gravity and magnetic field sensor values that are used to calculate heading
    float[] gravity;
    float[] geomagnetic;
    // Create Location which stores last known location
    Location oldLoc = null;
    // create variable to store old bearing value, used for rotation
    double oldBearingDegree = 0;
    // Create LocationManager to access GPS measurements
    private LocationManager locationManager;
    // Create SensorManager to access sensor measurements
    private SensorManager sensorManager;
    // Create temperature, acceleration and magnetic field sensors
    private Sensor tempSensor;
    private Sensor accelerometer;
    private Sensor magnetometer;
    // Create TextViews that display information
    private TextView headingTxtView;
    private TextView headingDegreeTxtView;
    private TextView speedTxtView;
    private TextView heightTxtView;
    private TextView accelerationTxtView;
    private TextView locationTxtView;
    private TextView temperatureTxtView;
    private ImageView headingImageView;
    private ToggleButton toggle;
    private boolean record;

    /**
     * Method that is called when activity is initialized. All UI elements are loaded and referenced.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Display icon in actionbar
        android.support.v7.app.ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayShowHomeEnabled(true);
        supportActionBar.setIcon(R.mipmap.ic_launcher);

        // Reference UI elements
        headingTxtView = (TextView) findViewById(R.id.valueHeading);
        headingDegreeTxtView = (TextView) findViewById(R.id.headingDegree);
        speedTxtView = (TextView) findViewById(R.id.valueSpeed);
        heightTxtView = (TextView) findViewById(R.id.valueHeight);
        accelerationTxtView = (TextView) findViewById(R.id.valueAcceleration);
        locationTxtView = (TextView) findViewById(R.id.valueLocation);
        temperatureTxtView = (TextView) findViewById(R.id.valueTemperature);
        headingImageView = (ImageView) findViewById(R.id.imageViewHeading);

        // reference ToggleButton to record measurements
        toggle = (ToggleButton) findViewById(R.id.toggleButton);

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_SHORT).show();
                    // haptic feedback when button is activated
                    toggle.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    record = isChecked;
                } else {
                    Toast.makeText(getApplicationContext(), "Recording stopped", Toast.LENGTH_SHORT).show();
                    // haptic feedback when button is deactivated
                    toggle.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    record = isChecked;
                }
            }
        });
    }

    /**
     * This function sets the locationManager as well as sensorManager. Furthermore it checks, if
     * all necessary permissions are given to acquire GPS measurings and requests location updates.
     * Furthermore, it checks if a temperature sensor is available and displays a message
     * (NOT_SUPPORTED) if it is not.
     */
    @Override
    protected void onStart() {
        super.onStart();

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            // We don't have the necessary permissions, so we have to request them.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            // We have the necessary permissions, and can request the
            // location updates.
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 2, this);
        }

        // get temperature sensor, if it is supported by current version.
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        }
        // set temperature textview to NOT_SUPPORTED if no temperature sensor is available
        if (tempSensor == null) {
            temperatureTxtView.setText(NOT_SUPPORTED);
        }
        // get accelerometer and magnetometer
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    /**
     * This function checks if the app has necessary permissions to access the GPS sensor and
     * requests location updates if it has.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
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
     * This function prevents the app from further accessing any sensors if it is stopped.
     */
    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(this);
        sensorManager.unregisterListener(this);
    }

    /**
     * Registers Listeners for all sensors on resume with a delay of one second.
     */
    protected void onResume() {
        super.onResume();
        if (tempSensor != null) {
            sensorManager.registerListener(this, tempSensor, 1000000);
        }
        sensorManager.registerListener(this, accelerometer, 3000000);
        sensorManager.registerListener(this, magnetometer, 3000000);
    }

    /**
     * Unregisters all sensors on pause
     */
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    /**
     * This function handles location changes invoming from the GPS sensor. The current location,
     * altitude and speed are directly displayed to the user. Furthermore, the current location
     * is stored after displaying it. It is used in a with the next location measurement to calculate
     * the acceleration.
     *
     * @param location (current location)
     */
    @Override
    public void onLocationChanged(Location location) {
        locationTxtView.setText(String.format("%.5f N, %.5f E", location.getLatitude(), location.getLongitude()));
        // location.getSpeed returns speed in m/s and therefore *3.6 to get speed in km/h
        speedTxtView.setText(String.format("%.1f km/h", location.getSpeed() * 3.6));
        accelerationTxtView.setText(String.format("%.2f m/s²", getAcceleration(location)));
        heightTxtView.setText(String.format("%.1f m.a.s.l.", location.getAltitude()));
        if (record) {
            logPosition(location);
        }
        oldLoc = location;
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
     * This function translates bearing values into user-readable text. Values range between
     * -180° and 180°, whereas 0° corresponds to the mobile heading north.
     *
     * @param bearing calculated based on azimuth (based on accelerometer and magnetic field sensor)
     * @return string heading as user-readable text (N/NW/W/SW/S/SE/E/NE)
     */
    public String getHeading(double bearing) {
        String heading = "N/A";
        if ((bearing >= -22.5 && bearing <= 0) || (bearing >= 0 && bearing <= 22.5)) {
            heading = "N";
        } else if (bearing < -22.5 && bearing >= -67.5) {
            heading = "NE";
        } else if (bearing < -67.5 && bearing >= -112.5) {
            heading = "E";
        } else if (bearing < -112.5 && bearing >= -157.5) {
            heading = "SE";
        } else if (bearing < -157.5 || bearing > 157.5) {
            heading = "S";
        } else if (bearing > 112.5 && bearing <= 157.5) {
            heading = "SW";
        } else if (bearing > 67.5 && bearing <= 112.5) {
            heading = "W";
        } else if (bearing > 22.5 && bearing <= 67.5) {
            heading = "NW";
        }
        return heading;
    }

    /**
     * This file writes all locations in a csv file which is stored on the SD card. If no SD card
     * is available, the position cannot be logged.
     *
     * @param location the current location
     */
    public void logPosition(Location location) {
        // check if SD card is mounted
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                // create directory on SD card in public folder documents. This way, the user can
                // access the file using a computer or a file explorer.
                File directory = new File(Environment.getExternalStorageDirectory() + "/Assignment1_Output");
                directory.mkdir();
                // create new file
                File f = new File(directory, "GPSTrack.csv");
                FileOutputStream outputStream = new FileOutputStream(f, true);
                PrintWriter writer = new PrintWriter(outputStream);
                // buffered reader to check if header is already written
                BufferedReader br = new BufferedReader(new FileReader(f));

                //write header first if no logs have been written so far
                if (br.readLine() == null) { //if the first line is null
                    writer.print("TimeStamp"); //write header
                    writer.print(";");
                    writer.print("Latitude");
                    writer.print(";");
                    writer.println("Longitude");
                }
                //write data if location is not null, e.g. data is available
                if (location != null) {
                    writer.print(location.getTime()); //Time
                    writer.print(";");
                    writer.print(location.getLatitude());
                    writer.print(";");
                    writer.println(location.getLongitude());
                }
                // close writer and output stream
                writer.close();
                outputStream.close();
                // make file readable for system and force media scanner to index it
                f.setReadable(true);
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(f));
                sendBroadcast(intent);
            } catch (Exception e) { //Catch exception if any
                // error message if exception is given.
                System.err.println("Error: " + e.getMessage());
                Log.e(getClass().toString(), Log.getStackTraceString(e));
            }
        } else { // log an error if no SD card is available
            Log.e("FileLog", "SD card not mounted");
        }
    }

    /**
     * This function handles incoming sensor measurements. It handles temperature measurements as
     * well as accelerometer and magnetic field measurements which are used to calculate the heading.
     * The heading is directly displayed on the display as user readable text.
     *
     * @param event
     */
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        // Set temperature on display, if event is of type temperature
        if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            float temperature = event.values[0];
            temperatureTxtView.setText(String.format("%.1f° C", temperature));
            // Get values of accelerometer and store them
        } else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values;
            // Get magnetic field values and store them
        } else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values;
        }
        // Access accelerometer and magnetic field values
        if (gravity != null && geomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            // check if rotation matrix was calculated
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                // calculate azimuth, based on accelerometer and magnetic field sensors
                float orientation[] = new float[3];
                orientation = lowPass(SensorManager.getOrientation(R, orientation), orientation);
                float azimuth = orientation[0];
                // calculate bearing based on azimuth
                double bearing = Math.round(-azimuth * 360 / (2 * Math.PI));
                // display bearing on display as readable text using getHeading(bearing)
                headingTxtView.setText(getHeading(bearing));
                double bearingDegree;
                if (bearing < 0) {
                    bearingDegree = 360 + bearing;
                } else if (bearing > 0) {
                    bearingDegree = bearing;
                } else {
                    bearingDegree = 0;
                }
                headingDegreeTxtView.setText(String.format("%.0f°", bearingDegree));
                // Create rotate animation of imageViewHeading
                RotateAnimation ra;
                // the following conditional statement manages the turning direction of the rotation
                // such that the rotation is not too large
                if (oldBearingDegree - bearingDegree > 180 || oldBearingDegree - bearingDegree < -180) {
                    ra = new RotateAnimation((float) bearingDegree, (float) oldBearingDegree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                } else {
                    ra = new RotateAnimation((float) oldBearingDegree, (float) bearingDegree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                }
                // Rotate heading image so that arrow always points north
                ra.setDuration(210);
                ra.setFillAfter(true);
                headingImageView.startAnimation(ra);
                // set old bearing
                oldBearingDegree = bearingDegree;
            }
        } else {
            headingTxtView.setText("N/A");
        }
    }

    /**
     * The low-pass filter omits high frequencies in sensor measurements and allows for a smoother
     * measuring process.
     *
     * @param input  measurement values directly from the sensor
     * @param output variable, in which the filtered values are written
     * @return filtered values
     */
    protected float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    /**
     * Unused function.
     *
     * @param sensor
     * @param accuracy
     */
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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

package ch.ethz.ikg.assignment1;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Observable;

/**
 * Created by chsch on 06.04.2017.
 */

public class SensorUpdates extends Observable implements SensorEventListener {

    // the ALPHA value is needed for the low-pass filter
    static final float ALPHA = 0.25f;
    // Message to be displayed if sensor is not available
    private final static String NOT_SUPPORTED = "Sensor not available";
    // Create arrays to store gravity and magnetic field sensor values that are used to calculate heading
    float[] gravity;
    float[] geomagnetic;

    // Create SensorManager to access sensor measurements
    private SensorManager sensorManager;

    // Create temperature, acceleration and magnetic field sensors
    private Sensor tempSensor;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private ArrayList<String> sensorValues = new ArrayList<>();

    protected void onStart(AppCompatActivity activity) {
        for (int i = 0; i <= 4; i++) {
            sensorValues.add("N/A");
        }
        // get temperature sensor, if it is supported by current version.
        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        }
        // set temperature textview to NOT_SUPPORTED if no temperature sensor is available
        if (tempSensor == null) {
            sensorValues.add(4, NOT_SUPPORTED);
        }
        // get accelerometer and magnetometer
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    protected void onStop() {
        sensorManager.unregisterListener(this);
    }

    protected void onResume() {
        if (tempSensor != null) {
            sensorManager.registerListener(this, tempSensor, 1000000);
        }
        sensorManager.registerListener(this, accelerometer, 3000000);
        sensorManager.registerListener(this, magnetometer, 3000000);
    }

    protected void onPause() {
        sensorManager.unregisterListener(this);
    }

    /**
     * This function handles incoming sensor measurements. It handles temperature measurements as
     * well as accelerometer and magnetic field measurements which are used to calculate the heading.
     * The heading is directly displayed on the display as user readable text.
     *
     * @param event
     */
    public void onSensorChanged(SensorEvent event) {
        setChanged();
        Sensor sensor = event.sensor;
        // Set temperature on display, if event is of type temperature
        if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            float temperature = event.values[0];
            sensorValues.add(4, String.format("%.1f° C", temperature));
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
                sensorValues.add(1, getHeading(bearing));
                double bearingDegree;
                if (bearing < 0) {
                    bearingDegree = 360 + bearing;
                } else if (bearing > 0) {
                    bearingDegree = bearing;
                } else {
                    bearingDegree = 0;
                }
                sensorValues.add(2, String.format("%.0f°", bearingDegree));
                sensorValues.add(3, String.valueOf(bearingDegree));

                notifyObservers(sensorValues);
            }
        } else {
            sensorValues.add(1, "N/A");
            sensorValues.add(2, "N/A");
            sensorValues.add(3, "0");
            notifyObservers(sensorValues);
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


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

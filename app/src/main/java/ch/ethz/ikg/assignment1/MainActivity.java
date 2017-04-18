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

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Observable;
import java.util.Observer;

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
 * The values are read in two additional classes which extend the Observable class. This class
 * implements the Observer interface which allows it to observe the LocationUpdates and
 * SensorUpdates so that the UI can be updated according to their changes.
 *
 * @author Christoph Schönenberger
 * @version 1.2
 * @since 06.04.2017
 */

public class MainActivity extends AppCompatActivity implements Observer {

    // create variable to store old bearing value, used for rotation
    float oldBearingDegree = 0f;
    private LocationUpdates locationUpdates = new LocationUpdates();
    private SensorUpdates sensorUpdates = new SensorUpdates();
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

        locationUpdates.addObserver(this);
        sensorUpdates.addObserver(this);

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

        // Button to start map activity with routing.
        /*A future version should include a drawer menu with which between several activities
        * can be switched. However, due to time pressure this could not be implemented. */
        Button routingButton = (Button) findViewById(R.id.routingButton);
        routingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("geo:");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                startActivity(intent);
            }
        });
    }

    /**
     * This method calls the onStart methods of all observables.
     */
    @Override
    protected void onStart() {
        super.onStart();
        locationUpdates.onStart(this);
        sensorUpdates.onStart(this);
    }

    /**
     * This function prevents is called when the activity is stopped. The observables are
     * stopped as well.
     */
    @Override
    protected void onStop() {
        super.onStop();
        locationUpdates.onStop();
        locationUpdates.deleteObserver(this);
        sensorUpdates.onStop();
        sensorUpdates.deleteObserver(this);
    }

    /**
     * Pauses all the app and also the observables that can be paused.
     */
    protected void onPause() {
        super.onPause();
        locationUpdates.deleteObserver(this);
        sensorUpdates.onPause();
        sensorUpdates.deleteObserver(this);
    }

    /**
     * The method is called, when the activity is resumed. The onResume() Method of the Observables
     * is called as well.
     */
    protected void onResume() {
        super.onResume();
        locationUpdates.addObserver(this);
        sensorUpdates.addObserver(this);
        sensorUpdates.onResume();
    }

    /**
     * This file writes all locations in a csv file which is stored on the SD card. If no SD card
     * is available, the position cannot be logged.
     *
     * @param location the current location
     */
    public void logPosition(String location) {
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
                    writer.println(location);
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
     * This function updates the UI when the observables change. Two possible observables are
     * included: LocationUpdates & SensorUpdates. The method handles these two types accordingly.
     *
     * @param o
     * @param arg
     */
    @Override
    public void update(Observable o, Object arg) {
        try {
            if (o instanceof LocationUpdates) {
                String[] locationValues = (String[]) arg;
                locationTxtView.setText(String.format("%.5f N, %.5f E", new Float(locationValues[0]), new Float(locationValues[1])));
                speedTxtView.setText(locationValues[2]);
                accelerationTxtView.setText(locationValues[3]);
                heightTxtView.setText(locationValues[4]);
                if (record) {
                    logPosition(locationValues[5]);
                }
            } else if (o instanceof SensorUpdates) {
                String[] sensorValues = (String[]) arg;
                headingTxtView.setText(sensorValues[0]);
                headingDegreeTxtView.setText(sensorValues[1]);
                temperatureTxtView.setText(sensorValues[3]);
                Float bearingDegree = new Float(sensorValues[2]);
                // Create rotate animation of imageViewHeading
                RotateAnimation ra;
                // the following conditional statement manages the turning direction of the rotation
                // such that the rotation is not too large
                if (oldBearingDegree - bearingDegree > 180 || oldBearingDegree - bearingDegree < -180) {
                    ra = new RotateAnimation(bearingDegree, oldBearingDegree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                } else {
                    ra = new RotateAnimation(oldBearingDegree, bearingDegree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                }
                // Rotate heading image so that arrow always points north
                ra.setDuration(210);
                ra.setFillAfter(true);
                headingImageView.startAnimation(ra);
                // set old bearing
                oldBearingDegree = bearingDegree;
            } else {
                throw new Exception("Invalid observable");
            }
        } catch (Exception e) {//Catch exception if any
            // error message if exception is given.
            System.err.println("Error: " + e.getMessage());
            Log.e(getClass().toString(), Log.getStackTraceString(e));
        }
    }
}

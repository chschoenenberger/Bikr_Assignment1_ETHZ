package ch.ethz.ikg.assignment1;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.LineSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import ch.ethz.ikg.assignment1.Analysis.*;
import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;

/**
 * This activity shows a map with all GPX files in the assets folder. The individual tracks as well
 * as an average path are displayed. Furthermore, the activity allows a comparison between the user
 * and the shown GPS tracks considering speed and time of the user and on average at this position.
 */
public class Routing extends AppCompatActivity implements Observer {

    // initialize map and graphics
    private ArcGISMap map = null;
    private GraphicsOverlay graphicsOverlay = new GraphicsOverlay();

    // initialize basemaps
    MenuItem mStreetsMenuItem = null;
    MenuItem mTopoMenuItem = null;
    MenuItem mGrayMenuItem = null;

    // The GPX parser comes from the "io.ticofab.androidgpxparser:parser" library.
    GPXParser mParser;

    // initialize buttons and other UI elements
    ImageButton centerButton = null;
    ImageButton northButton = null;
    private TextView avTimeTxtView;
    private TextView avSpeedTxtView;
    private TextView youTimeTxtView;
    private TextView youSpeedTxtView;
    private ProgressBar progressBar;

    // initialize aggregated (average) path
    private List<Trackpoint> aggPath;

    // initialize starting time of app
    private long startTime;

    // initialize locationUpdates to access user-speed and get according measures of aggregated path
    private LocationUpdates locationUpdates = new LocationUpdates();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);

        // initialize MapView with Map including basemap and graphicsOverlay
        final MapView mapView = (MapView) findViewById(R.id.mapView2);

        // In a future version, an option menu could be displayed to select the basemaps
        map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 47.391629, 8.522529, 12);
        mapView.setMap(map);
        mapView.getGraphicsOverlays().add(graphicsOverlay);

        // display current location on map with autopan
        final LocationDisplay locationDisplay = mapView.getLocationDisplay();

        // as soon as user pans the AutoPanMode is set to OFF
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
        locationDisplay.startAsync();

        // add this activity as observer of LocationUpdates
        locationUpdates.addObserver(this);

        // Reference UI elements
        avTimeTxtView = (TextView) findViewById(R.id.avTime);
        avSpeedTxtView = (TextView) findViewById(R.id.avSpeed);
        youTimeTxtView = (TextView) findViewById(R.id.yourTime);
        youSpeedTxtView = (TextView) findViewById(R.id.yourSpeed);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        // Set starting time on creation
        startTime = DateTime.now().getMillis();

        // This is needed for the GPX parser.
        JodaTimeAndroid.init(this);
        mParser = new GPXParser();

        // Load GPX tracks
        List<Gpx> tracks = loadTracks();

        // Draw tracks on the map.
        clearMap();
        visualizeTracks(tracks);

        // initialize button to recenter
        centerButton = (ImageButton) findViewById(R.id.centerButton2);
        centerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                centerButton.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (locationDisplay.getAutoPanMode() == LocationDisplay.AutoPanMode.OFF) {
                    locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
                    Toast.makeText(getApplicationContext(), "Map centered", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Map already centered", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // initialize button to adjust view to north
        northButton = (ImageButton) findViewById(R.id.northButton2);
        northButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                northButton.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                // if user is currently centered on location just set north and continue navigation
                // without adjusting map angle to user heading
                if (locationDisplay.getAutoPanMode() == LocationDisplay.AutoPanMode.COMPASS_NAVIGATION) {
                    mapView.setViewpointRotationAsync(0);
                    locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.NAVIGATION);
                    // if user is not currently in navigation mode just rotate map north
                } else {
                    mapView.setViewpointRotationAsync(0);
                }
            }
        });

    }

    /**
     * Loads some tracks from the "assets" folder.
     *
     * @return A list of GPX tracks, namely all found in the "assets" folder (ending with
     * ".gpx").
     */
    private List<Gpx> loadTracks() {
        List<Gpx> tracks = new LinkedList<>();
        try {
            for (String file : getAssets().list("")) {
                if (file.endsWith(".gpx")) {
                    InputStream in = getAssets().open(file);
                    Log.d("tracks", "Loading: " + file);
                    tracks.add(mParser.parse(in));
                }
            }
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
        return tracks;
    }

    /**
     * Removes all overlays from the map.
     */
    private void clearMap() {
        if (graphicsOverlay.getGraphics().size() > 0) {
            graphicsOverlay.getGraphics().remove(0);
        }
    }

    /**
     * Visualizes a list of GPX tracks, by drawing all tracks, and an "average" track, computed using
     * the dynamic time warping algorithm.
     *
     * @param tracks The overall track list.
     */
    private void visualizeTracks(List<Gpx> tracks) {
        // creates a style for all paths
        SimpleLineSymbol allTracksSymbol = new
                SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.argb(100, 205, 55, 0), 3);
        // creates a style for the average path
        SimpleLineSymbol aggrTrackSymbol = new
                SimpleLineSymbol(SimpleLineSymbol.Style.DASH, Color.argb(255, 205, 55, 0), 5);

        List<List<Trackpoint>> tracksAsTrackpoints = new LinkedList<>();
        // This draws all tracks, and also stores all GPX tracks as Trackpoint tracks.
        for (Gpx track : tracks) {
            visualizeTrack(gpxToTrackpoint(track), allTracksSymbol);
            tracksAsTrackpoints.add(gpxToTrackpoint(track));
        }

        // Finding "average" path, by using dynamic time warping.
        while (tracksAsTrackpoints.size() > 1) {
            List<Trackpoint> t0 = tracksAsTrackpoints.remove(0);
            List<Trackpoint> t1 = tracksAsTrackpoints.remove(0);
            DynamicTimeWarp.DTWResult res = DynamicTimeWarp.compute(t0, t1);
            aggPath = new LinkedList<>();

            // Basically, for every trackpoint of t0, we look which trackpoints of t1
            // correspond to it, and take the average. We do this for all tracks in a cluster.
            for (int j = 0; j < res.getPath().size(); j++) {
                List<Integer> mapping = res.getPath().get(j);
                if (mapping.size() > 0) {
                    double lon = t0.get(j).getLongitude();
                    double lat = t0.get(j).getLatitude();
                    long time = t0.get(j).getTime();

                    for (int id : mapping) {
                        lon += t1.get(id).getLongitude();
                        lat += t1.get(id).getLatitude();
                        time += t1.get(id).getTime(); //sum up time in milliseconds
                    }

                    lon = lon / (mapping.size() + 1);
                    lat = lat / (mapping.size() + 1);
                    time = time / (mapping.size() + 1); // average time at point t0

                    aggPath.add(new Trackpoint(lon, lat, time));
                }
            }
            tracksAsTrackpoints.add(aggPath);
            progressBar.setMax(getMaxDist()); //set distance from start to end as maxValue for progress bar
        }

        visualizeTrack(tracksAsTrackpoints.get(0), aggrTrackSymbol);
    }

    /**
     * Visualizes a track with a given symbol.
     *
     * @param track  The track to draw, as a list of {@link Trackpoint}s.
     * @param symbol The symbol to draw the track with.
     */
    private void visualizeTrack(List<Trackpoint> track, LineSymbol symbol) {
        PointCollection pointColl = new PointCollection(SpatialReferences.getWgs84());

        for (Trackpoint t : track) {
            pointColl.add(new Point(t.getLongitude(), t.getLatitude()));
        }

        Polyline polyline = new Polyline(pointColl);
        Graphic routeGraphic = new Graphic(polyline, symbol);

        graphicsOverlay.getGraphics().add(routeGraphic);
    }

    /**
     * Converts a {@link Gpx} to a list of {@link Trackpoint}s, which are internally used.
     *
     * @param track The GPX track to convert.
     * @return A list of trackpoints.
     */
    private List<Trackpoint> gpxToTrackpoint(Gpx track) {
        List<Trackpoint> trackpoints = new LinkedList<>();

        for (Track t : track.getTracks()) {
            for (TrackSegment segment : t.getTrackSegments()) {
                for (TrackPoint routePoint : segment.getTrackPoints()) {
                    trackpoints.add(new Trackpoint(routePoint.getLongitude(), routePoint.getLatitude(),
                            routePoint.getTime().getMillis()));
                }
            }
        }

        return trackpoints;
    }

    /**
     * Calculates the time difference between two times in milliseconds and returns a nicely formatted
     * string containing hours, minutes and seconds
     * @param d1 time-value from
     * @param d2 time-value to
     * @return Formatted string
     */
    private static String getTimeDiff(long d1, long d2) {
        long timeDiff = Math.abs(d1 - d2);
        int seconds = (int) (timeDiff / 1000) % 60;
        int minutes = (int) (timeDiff / (60 * 1000)) % 60;
        int hours = (int) timeDiff / (60 * 60 * 1000);
        return (hours + "h " + minutes + "min " + seconds + "s");
    }

    /**
     * This method calls the onStart methods of the superclass. Location updates are started.
     */
    @Override
    protected void onStart() {
        super.onStart();
        locationUpdates.onStart(this);
    }

    /**
     * This function is called when the activity is stopped. Location updates are stopped as well.
     */
    @Override
    protected void onStop() {
        super.onStop();
        locationUpdates.onStop();
    }

    /**
     * Pauses the app and deletes this as observer.
     */
    protected void onPause() {
        super.onPause();
        locationUpdates.deleteObserver(this);
    }

    /**
     * The method is called, when the activity is resumed. Adds this as an observer.
     */
    protected void onResume() {
        super.onResume();
        locationUpdates.addObserver(this);
    }

    /**
     * Create menu to select basemap.
     *
     * @param menu which is to be loaded
     * @return always true
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        mTopoMenuItem = menu.getItem(0);
        mStreetsMenuItem = menu.getItem(1);
        mGrayMenuItem = menu.getItem(2);
        mTopoMenuItem.setChecked(true);
        return true;
    }

    /**
     * Change basemap when selection is done.
     *
     * @param item chosen basemap
     * @return true when basemap is changed
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.World_Topo:
                map.setBasemap(Basemap.createTopographic());
                mTopoMenuItem.setChecked(true);
                return true;
            case R.id.World_Street_Map:
                map.setBasemap(Basemap.createStreets());
                mStreetsMenuItem.setChecked(true);
                return true;
            case R.id.Gray:
                map.setBasemap(Basemap.createLightGrayCanvasVector());
                mGrayMenuItem.setChecked(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This function updates the UI elements every time some new values from the LocationUpdates come
     * in. Some values are directly received from the observable and some are calculated.
     * @param o observable that should be of type LocationUpdates
     * @param arg that are passed by observable on every update
     */
    @Override
    public void update(Observable o, Object arg) {
        try {
            if (o instanceof LocationUpdates) {
                String[] locationValues = (String[]) arg;
                youSpeedTxtView.setText(locationValues[2]);
                youTimeTxtView.setText(getTimeDiff(DateTime.now().getMillis(), startTime));
                // current location is stored as trackpoint to ease further calculations
                Trackpoint current = new Trackpoint(Double.valueOf(locationValues[1]), Double.valueOf(locationValues[0]), DateTime.now().getMillis());
                // min distance is initialized
                double min = Double.POSITIVE_INFINITY;
                Trackpoint closest = null;
                for (Trackpoint tp : aggPath) { //compares the distance between the current location
                    // and every trackpoint in the aggPath to get the closest trackpoit
                    if (current.distance(tp) < min) {
                        min = current.distance(tp);
                        closest = tp;
                    }
                }
                // Average time is extracted from closest trackpoint
                avTimeTxtView.setText(getTimeDiff(closest.getTime(), aggPath.get(0).getTime()));
                int closestIndex = aggPath.indexOf(closest);
                // Average speed at closest trackpoint is calculated
                if (aggPath.get(closestIndex - 1) != null) {
                    avSpeedTxtView.setText(String.format("%.2f km/h", closest.speed(aggPath.get(closestIndex - 1))));
                }
                // Current progress is calculated and updated.
                int progress = (int) (getMaxDist() - current.distance(aggPath.get(aggPath.size() - 1)));
                progressBar.setProgress(progress);
            }
        } catch (Exception e) {
            Log.e("Location Update", "Location update did not work.");
            Log.getStackTraceString(e);
        }
    }

    /**
     * This method calculates the distance between the first point of the aggregated path and the
     * destination
     *
     * @return Distance between start and end
     */
    public int getMaxDist() {
        return (int) aggPath.get(0).distance(aggPath.get(aggPath.size() - 1));
    }
}

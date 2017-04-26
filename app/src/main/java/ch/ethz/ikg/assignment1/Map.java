package ch.ethz.ikg.assignment1;

import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapRotationChangedEvent;
import com.esri.arcgisruntime.mapping.view.MapRotationChangedListener;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.security.Credential;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.symbology.MarkerSymbol;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;

/**
 * This activity contains the a Map to navigate. With a long press on the shown features, the user
 * can navigate to certain POIs.
 */

public class Map extends AppCompatActivity {

    private ArcGISMap map = null;
    private GraphicsOverlay graphicsOverlay = new GraphicsOverlay();
    // initialize symbols
    private PictureMarkerSymbol rentalSymbol = null;
    private PictureMarkerSymbol pumpSymbol = null;
    private PictureMarkerSymbol parkingSymbol = null;
    // initialize basemaps
    MenuItem mStreetsMenuItem = null;
    MenuItem mTopoMenuItem = null;
    MenuItem mGrayMenuItem = null;
    // initialize buttons
    ImageButton centerButton = null;
    ImageButton northButton = null;

    /**
     * Creates activity and initializes the necessary view and buttons.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);

        // initialize MapView with Map including basemap and graphicsOverlay
        final MapView mapView = (MapView) findViewById(R.id.mapView);

        // In a future version, an option menu could be displayed to select the basemaps
        map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 47.391629, 8.522529, 12);
        mapView.setMap(map);
        mapView.getGraphicsOverlays().add(graphicsOverlay);

        // add portal and layers
        Portal portal = this.addPortal();
        this.addLayers(portal, mapView);

        // display current location on map with autopan
        final LocationDisplay locationDisplay = mapView.getLocationDisplay();

        // as soon as user pans the AutoPanMode is set to OFF
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
        locationDisplay.startAsync();

        // initialize button to recenter
        centerButton = (ImageButton) findViewById(R.id.centerButton);
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
        northButton = (ImageButton) findViewById(R.id.northButton);
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

        // create onTouchlisteer which reacts to longpress to start navigating
        mapView.setOnTouchListener(new Routing(this, mapView));

        mapView.addMapRotationChangedListener(new MapRotationChangedListener() {
            @Override
            public void mapRotationChanged(MapRotationChangedEvent mapRotationChangedEvent) {
                if (rentalSymbol != null) {
                    rentalSymbol.setAngle((float) mapView.getMapRotation());
                }
                if (parkingSymbol != null) {
                    parkingSymbol.setAngle((float) mapView.getMapRotation());
                }
                if (pumpSymbol != null) {
                    pumpSymbol.setAngle((float) mapView.getMapRotation());
                }
            }
        });

    }

    /**
     * add the IKG portal with specified credentials
     *
     * @return
     */
    public Portal addPortal() {
        Portal portal = new Portal("http://www.arcgis.com");
        Credential creds = new UserCredential(getString(R.string.arcGIS_user_loc), getString(R.string.arcGIS_password_loc));
        portal.setCredential(creds);
        return portal;
    }

    /**
     * Add layers to created Portal containing rental, pumping stations and parking lots
     * In a future version, the symbols should be generalized so that not too many POIs are
     * displayed at the same time. Furthermore, features should always be angle alined to the
     * screen. However, the function to achieve this Symbol.setAngleAlignment(MarkerSymbol.AngleAlignment.SCREEN);
     * did not work.
     *
     * @param portal  which was created beforehand
     * @param mapView on which layers are to be displayes
     */
    public void addLayers(Portal portal, MapView mapView) {
        // get map on which layers are to be displayed
        ArcGISMap map = mapView.getMap();

        // add parking spot layer with adjusted symbology and render features on map
        PortalItem parkingSpot = new PortalItem(portal, "3e96e04f176347c9acd97cdebcf63ea0");
        FeatureLayer parkingSpotLayer = new FeatureLayer(parkingSpot, 0);
        BitmapDrawable parkingDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.mipmap.ic_parking);
        parkingSymbol = new PictureMarkerSymbol(parkingDrawable);
        parkingSymbol.setHeight(20);
        parkingSymbol.setWidth(20);
        SimpleRenderer rendererParking = new SimpleRenderer(parkingSymbol);
        parkingSpotLayer.setRenderer(rendererParking);
        map.getOperationalLayers().add(parkingSpotLayer);

        // add pumping station layer with adjusted symbology and render features on map
        PortalItem pumpingStation = new PortalItem(portal, "039bcfd424b946e9b799d8c6a81be9ff");
        FeatureLayer pumpingStationLayer = new FeatureLayer(pumpingStation, 0);
        BitmapDrawable pumpDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.mipmap.ic_pump);
        pumpSymbol = new PictureMarkerSymbol(pumpDrawable);
        pumpSymbol.setHeight(30);
        pumpSymbol.setWidth(30);
        SimpleRenderer rendererPump = new SimpleRenderer(pumpSymbol);
        pumpingStationLayer.setRenderer(rendererPump);
        map.getOperationalLayers().add(pumpingStationLayer);

        // add rental places layer with adjusted symbology and render features on map
        PortalItem rental = new PortalItem(portal, "e12ad29f628a4070b9cacd729a492f4d");
        FeatureLayer rentalLayer = new FeatureLayer(rental, 0);
        BitmapDrawable rentalDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.mipmap.ic_rental);
        rentalSymbol = new PictureMarkerSymbol(rentalDrawable);
        rentalSymbol.setHeight(30);
        rentalSymbol.setWidth(30);
        rentalSymbol.setAngleAlignment(MarkerSymbol.AngleAlignment.SCREEN);
        SimpleRenderer rendererRent = new SimpleRenderer(rentalSymbol);
        rentalLayer.setRenderer(rendererRent);
        map.getOperationalLayers().add(rentalLayer);
    }

    /**
     * This method calls the onStart methods of the superclass.
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * This function is called when the activity is stopped.
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * Pauses all the app.
     */
    protected void onPause() {
        super.onPause();
    }

    /**
     * The method is called, when the activity is resumed.
     */
    protected void onResume() {
        super.onResume();
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

}

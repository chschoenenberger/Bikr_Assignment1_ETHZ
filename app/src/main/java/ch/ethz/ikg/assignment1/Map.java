package ch.ethz.ikg.assignment1;

import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.security.Credential;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;


public class Map extends AppCompatActivity {

    GraphicsOverlay graphicsOverlay = new GraphicsOverlay();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);

        // initialize map
        final MapView mapView = (MapView) findViewById(R.id.mapView);
        ArcGISMap map = new ArcGISMap(Basemap.Type.LIGHT_GRAY_CANVAS_VECTOR, 47.408570, 8.506846, 16);
        mapView.setMap(map);

        // add portal and layers
        Portal portal = this.addPortal();
        this.addLayers(portal, mapView);

        // display current location on map with autopan
        final LocationDisplay locationDisplay = mapView.getLocationDisplay();
        // as soon as user pans the AutoPanMode is set to OFF
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
        locationDisplay.startAsync();

        // initialize button to recenter
        Button centerButton = (Button) findViewById(R.id.centerButton);
        centerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationDisplay.getAutoPanMode() == LocationDisplay.AutoPanMode.OFF) {
                    locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
                    Toast.makeText(getApplicationContext(), "Map centered", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Map already centered", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button northButton = (Button) findViewById(R.id.northButton);
        northButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.setViewpointRotationAsync(0);
            }
        });

        mapView.setOnTouchListener(new Routing(this, mapView));
    }

    public Portal addPortal() {
        // add portal
        Portal portal = new Portal("http://www.arcgis.com");
        Credential creds = new UserCredential(getString(R.string.arcGIS_user_loc), getString(R.string.arcGIS_password_loc));
        portal.setCredential(creds);
        return portal;
    }

    public void addLayers(Portal portal, MapView mapView) {
        ArcGISMap map = mapView.getMap();
        mapView.getGraphicsOverlays().add(graphicsOverlay);

        // add additional layers from portal
        PortalItem parkingSpot = new PortalItem(portal, "3e96e04f176347c9acd97cdebcf63ea0");
        FeatureLayer parkingSpotLayer = new FeatureLayer(parkingSpot, 0);
        BitmapDrawable parkingDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.mipmap.ic_parking);
        PictureMarkerSymbol parkingSymbol = new PictureMarkerSymbol(parkingDrawable);
        parkingSymbol.setHeight(20);
        parkingSymbol.setWidth(20);
        SimpleRenderer rendererParking = new SimpleRenderer(parkingSymbol);
        parkingSpotLayer.setRenderer(rendererParking);
        map.getOperationalLayers().add(parkingSpotLayer);

        PortalItem pumpingStation = new PortalItem(portal, "039bcfd424b946e9b799d8c6a81be9ff");
        FeatureLayer pumpingStationLayer = new FeatureLayer(pumpingStation, 0);
        BitmapDrawable pumpDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.mipmap.ic_pump);
        PictureMarkerSymbol pumpSymbol = new PictureMarkerSymbol(pumpDrawable);
        pumpSymbol.setHeight(30);
        pumpSymbol.setWidth(30);
        SimpleRenderer rendererPump = new SimpleRenderer(pumpSymbol);
        pumpingStationLayer.setRenderer(rendererPump);
        map.getOperationalLayers().add(pumpingStationLayer);

        PortalItem rental = new PortalItem(portal, "e12ad29f628a4070b9cacd729a492f4d");
        FeatureLayer rentalLayer = new FeatureLayer(rental, 0);
        BitmapDrawable rentalDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.mipmap.ic_rental);
        PictureMarkerSymbol rentalSymbol = new PictureMarkerSymbol(rentalDrawable);
        rentalSymbol.setHeight(30);
        rentalSymbol.setWidth(30);
        SimpleRenderer rendererRent = new SimpleRenderer(rentalSymbol);
        rentalLayer.setRenderer(rendererRent);
        map.getOperationalLayers().add(rentalLayer);
    }

    /**
     * This method calls the onStart methods of all observables.
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * This function prevents is called when the activity is stopped. The observables are
     * stopped as well.
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * Pauses all the app and also the observables that can be paused.
     */
    protected void onPause() {
        super.onPause();
    }

    /**
     * The method is called, when the activity is resumed. The onResume() Method of the Observables
     * is called as well.
     */
    protected void onResume() {
        super.onResume();
    }
}

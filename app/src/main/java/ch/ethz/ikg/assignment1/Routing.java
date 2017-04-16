package ch.ethz.ikg.assignment1;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;

import java.util.Observable;
import java.util.Observer;

public class Routing extends AppCompatActivity implements Observer {

    private LocationUpdates locationUpdates = new LocationUpdates();
    private MapView mapView;
    private Double longitude;
    private Double latitude;
    GraphicsOverlay graphicsOverlay = new GraphicsOverlay();
    SimpleMarkerSymbol positionSym = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.GREEN, 12);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);

        locationUpdates.addObserver(this);

        mapView = (MapView) findViewById(R.id.mapView);
        ArcGISMap map = new ArcGISMap(Basemap.Type.LIGHT_GRAY_CANVAS_VECTOR, 47.408570, 8.506846, 16);
        if (longitude != null && latitude != null) {
            map.setInitialViewpoint(new Viewpoint(latitude, longitude, 16));
        }
        mapView.setMap(map);
        mapView.getGraphicsOverlays().add(graphicsOverlay);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof LocationUpdates) {
            String[] locationValues = (String[]) arg;
            latitude = new Double(locationValues[0]);
            longitude = new Double(locationValues[1]);
            Point position = new Point(longitude, latitude, SpatialReferences.getWgs84());
            /*mapView.setViewpointAsync(new Viewpoint(position,14),1f);*/
            mapView.setViewpointCenterAsync(position);
            graphicsOverlay.getGraphics().clear();
            Graphic graphic = new Graphic(position, positionSym);
            graphicsOverlay.getGraphics().add(graphic);
        }
    }

    /**
     * This method calls the onStart methods of all observables.
     */
    @Override
    protected void onStart() {
        super.onStart();
        locationUpdates.onStart(this);
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
    }

    /**
     * Pauses all the app and also the observables that can be paused.
     */
    protected void onPause() {
        super.onPause();
        locationUpdates.deleteObserver(this);
    }

    /**
     * The method is called, when the activity is resumed. The onResume() Method of the Observables
     * is called as well.
     */
    protected void onResume() {
        super.onResume();
        locationUpdates.addObserver(this);
    }
}

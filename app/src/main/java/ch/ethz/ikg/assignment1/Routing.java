package ch.ethz.ikg.assignment1;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.Credential;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static android.content.ContentValues.TAG;

/**
 * Created by chsch on 17.04.2017.
 */

public class Routing extends DefaultMapViewOnTouchListener {

    private Context mContext = null;

    // provide a default constructor
    public Routing(Context context, MapView mapView) {
        super(context, mapView);
        mContext = context;
    }

    // override the onSingleTapConfirmed gesture to handle a single tap on the MapView
    @Override
    public void onLongPress(MotionEvent e) {
        // get the screen point where user tapped
        android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());
        final ListenableFuture<List<IdentifyLayerResult>> identifyFuture = mMapView.identifyLayersAsync(screenPoint, 20, false, 25);
        identifyFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // get the identify results from the future - returns when the operation is complete
                    List<IdentifyLayerResult> identifyLayersResults = identifyFuture.get();
                    // iterate all the layers in the identify result
                    for (IdentifyLayerResult identifyLayerResult : identifyLayersResults) {
                        // iterate each result in each identified layer, and check for Feature results
                        if (identifyLayerResult.getElements().size() > 0) {
                            GeoElement topmostElement = identifyLayerResult.getElements().get(0);
                            if (topmostElement instanceof Feature) {
                                Feature identifiedFeature = (Feature) topmostElement;
                                Point to = (Point) identifiedFeature.getGeometry();
                                askForRoute(to);
                            }
                        }
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    Log.e(TAG, "Exception: Wrong features retrieved", ex);
                    ex.printStackTrace();
                }
            }
        });
    }

    public void askForRoute(final Point to) {
        DialogInterface.OnClickListener dialogClickListener = new
                DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                route(mMapView, mMapView.getLocationDisplay().getMapLocation(), to);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                Toast.makeText(mContext, "Map canceled!", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                };
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        DecimalFormat df = new DecimalFormat("#.000");
        builder.setMessage("Do you want to route to (" + df.format(to.getX()) +
                ", " + df.format(to.getY()) + ")?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private void route(final MapView mapView, final Point from, final Point to) {

        // select routing service and enter credentials
        final RouteTask routeTask = new RouteTask("http://route.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World");
        Credential creds = new UserCredential(mContext.getString(R.string.arcGIS_user_loc), mContext.getString(R.string.arcGIS_password_loc));
        routeTask.setCredential(creds);

        final ListenableFuture<RouteParameters> routeParametersFuture = routeTask.createDefaultParametersAsync();
        routeParametersFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                RouteParameters routeParameters = null;
                try {
                    routeParameters = routeParametersFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                assert routeParameters != null;
                routeParameters.setReturnDirections(true);
                routeParameters.setReturnRoutes(true);
                routeParameters.setOutputSpatialReference(mapView.getSpatialReference());

                List<Stop> stops = new ArrayList<>();
                stops.add(new Stop(from));
                stops.add(new Stop(to));

                routeParameters.getStops().addAll(stops);
                final ListenableFuture<RouteResult> routeResultFuture = routeTask.solveRouteAsync(routeParameters);
                routeTask.addDoneLoadingListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            RouteResult routeResult = routeResultFuture.get();
                            Route route = routeResult.getRoutes().get(0);
                            SimpleLineSymbol routeSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.DASH, Color.BLUE, 4);
                            Graphic routeGraphic = new Graphic(route.getRouteGeometry(), routeSymbol);
                            GraphicsOverlay graphicsOverlay = mMapView.getGraphicsOverlays().get(0);
                            graphicsOverlay.getGraphics().add(routeGraphic);

                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
}


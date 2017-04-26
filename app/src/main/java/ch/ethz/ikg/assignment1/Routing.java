package ch.ethz.ikg.assignment1;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
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
import java.util.*;
import java.util.concurrent.ExecutionException;

import static android.content.ContentValues.TAG;

/**
 * This class handles all the routing. If a user presses on a feature which is contained in the
 * layers he can navigate to it.
 */

public class Routing extends DefaultMapViewOnTouchListener {

    // initialize needed variables
    private Context mContext = null;
    private Point destination = null;
    private Graphic routeGraphic = null;
    private GraphicsOverlay graphicsOverlay = mMapView.getGraphicsOverlays().get(0);
    private Vibrator vib = null;

    /**
     * Default constructor
     *
     * @param context from activity which calls the routing task
     * @param mapView from activity which calls the routing task
     */
    Routing(Context context, MapView mapView) {
        super(context, mapView);
        mContext = context;
    }

    /**
     * override the onSingleTapConfirmed gesture to handle a single tap on the MapView
     *
     * @param e MotionEvent which starts the routing
     */
    @Override
    public void onLongPress(MotionEvent e) {
        // if another routing task already exists, clear route
        /* in a future version, the user should be asked, if current destination should be
           included in the routing.*/
        if (destination != null) {
            stopRouting();
            Toast.makeText(mContext, "Previous routing task cancelled", Toast.LENGTH_SHORT).show();
        }
        vib = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        // get the screen point where user tapped
        android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());
        final ListenableFuture<List<IdentifyLayerResult>> identifyFuture = mMapView.identifyLayersAsync(screenPoint, 20, false, 25);
        identifyFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // get the identify results from the future - returns when the operation is complete
                    List<IdentifyLayerResult> identifyLayersResults = identifyFuture.get();
                    // select topmost layer
                    if (identifyLayersResults.size() > 0) {
                        IdentifyLayerResult identifyLayerResult = identifyLayersResults.get(0);
                        // select topmost element of topmost layer
                        if (identifyLayerResult.getElements().size() > 0) {
                            GeoElement topmostElement = identifyLayerResult.getElements().get(0);
                            if (topmostElement instanceof Feature) {
                                // get geometry of feature and ask for route to that feature
                                vib.vibrate(100);
                                Feature identifiedFeature = (Feature) topmostElement;
                                destination = (Point) identifiedFeature.getGeometry();
                                askForRoute(destination);
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

    /**
     * Ask user if he wants to route to a certain POI
     *
     * @param to point to which the route leads
     */
    private void askForRoute(final Point to) {
        DialogInterface.OnClickListener dialogClickListener = new
                DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                route(mMapView, mMapView.getLocationDisplay().getMapLocation(), to);
                                if (mMapView.getLocationDisplay().getAutoPanMode() != LocationDisplay.AutoPanMode.COMPASS_NAVIGATION) {
                                    mMapView.getLocationDisplay().setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
                                }
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                Toast.makeText(mContext, "Routing canceled!", Toast.LENGTH_SHORT).show();
                                // clear destination if routing task is cancelled
                                destination = null;
                                break;
                        }
                    }
                };
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        DecimalFormat df = new DecimalFormat("#.000");
        builder.setMessage("Do you want to route to " + df.format(to.getX()) +
                ", " + df.format(to.getY()) + "?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
        // add LocationListener which stops routing task when destination is reached
        addDestinationAwareness();
    }

    /**
     * Routing task itself which accesses the routing service and displays the route on the provided
     * MapView
     *
     * @param mapView on which the route is to be displayed
     * @param from    Origin = current location
     * @param to      destination of routing task
     */
    private void route(final MapView mapView, final Point from, final Point to) {
        // select routing service and enter credentials for service
        final RouteTask routeTask = new RouteTask("http://route.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World");
        Credential creds = new UserCredential(mContext.getString(R.string.arcGIS_user_loc), mContext.getString(R.string.arcGIS_password_loc));
        routeTask.setCredential(creds);

        // get route parameters
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
                            routeGraphic = new Graphic(route.getRouteGeometry(), routeSymbol);
                            graphicsOverlay.getGraphics().add(routeGraphic);
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    /**
     * This method adds a LocationListener for the current map location. The advantage of including
     * this method here is that we don't have to worry about the coordinate system of the destination
     * and the current location.
     */
    private void addDestinationAwareness() {
        mMapView.getLocationDisplay().addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
            @Override
            public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
                if (destination != null) {
                    // if distance between user location and destination is less than 150 map units
                    // the routing task is stopped and a notification is sent to the user
                    if (GeometryEngine.distanceBetween(destination, mMapView.getLocationDisplay().getMapLocation()) < 150) {
                        stopRouting();
                        sendNotification(mContext, "Destination reached");
                    }
                }
            }
        });
    }

    /**
     * This method deletes the displayed route from the map and sets the destination to null.
     */
    private void stopRouting() {
        graphicsOverlay.getGraphics().clear();
        destination = null;
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MapActivity.
     */
    private void sendNotification(Context context, String notificationDetails) {
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(context, Map.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // Define the notification settings.
        builder.setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle(notificationDetails)
                .setContentText(context.getString(R.string.proximity_text))
                .setContentIntent(notificationPendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }
}


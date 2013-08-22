package org.anhonesteffort.polygons;

import org.anhonesteffort.polygons.action.BroadcastActionLauncher;
import org.anhonesteffort.polygons.communication.SMSSender;
import org.anhonesteffort.polygons.geometry.TaggedPoint;
import org.anhonesteffort.polygons.geometry.TaggedPolygon;
import org.anhonesteffort.polygons.location.BetterLocationListener;
import org.anhonesteffort.polygons.location.BetterLocationManager;
import org.anhonesteffort.polygons.map.GoogleGeometryFactory;
import org.anhonesteffort.polygons.storage.DatabaseHelper;
import org.anhonesteffort.polygons.storage.GeometryChangeListener;
import org.anhonesteffort.polygons.storage.LocationSubscriberChangeListener;

import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class ZoneService extends Service implements 
  GeometryChangeListener, LocationSubscriberChangeListener, BetterLocationListener {
  private static final String TAG           = "org.anhonesteffort.polygons.PolygonListActivity";
  public static final String POLYGON_ENTER	= "org.anhonesteffort.polygons.POLYGON_ENTER";
  public static final String POLYGON_EXIT		= "org.anhonesteffort.polygons.POLYGON_EXIT";
  public static final String POLYGON_ID		  = "org.anhonesteffort.polygons.POLYGON_ID";
  public static final String POLYGON_LABEL	= "org.anhonesteffort.polygons.POLYGON_LABEL";
  public static final String PHONE_LOCATION	= "org.anhonesteffort.polygons.PHONE_LOCATION";
  
  private static final int MINIMUM_INTERVAL_MS         = 1500;
  private static final int REGULAR_INTERVAL_MS         = 120000;
  private static final int SUBSCRIBER_INTERVAL_MS      = 30000;
  private static final double AVG_WALKING_VELOCITY_MPS = 1.5;

  private final IBinder binder = new ZoneServiceBinder();
  private DatabaseHelper applicationStorage;
  private BroadcastActionLauncher actionLauncher;
  
  private BetterLocationManager locationManager;
  private Location bestLocation;

  public class ZoneServiceBinder extends Binder {
    public ZoneService getService() {
      return ZoneService.this;
    }
  }

  @Override
  public void onCreate() {
    Log.d(TAG, "onCreate()");
    super.onCreate();
    
    // Register for changes in polygon geometries and location subscribers.
    applicationStorage = DatabaseHelper.getInstance(this.getBaseContext());
    applicationStorage.polygons.addGeometryChangeListener(this);
    applicationStorage.actions.addLocationSubscriberChangeListener(this);
    
    // Use the BetterLocationManager for location updates.
    locationManager = new BetterLocationManager(this.getApplicationContext());
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    if(settings.getBoolean(PreferencesActivity.PREF_GEOFENCING, true))
      locationManager.requestLocationUpdates(MINIMUM_INTERVAL_MS, this);
    
    // Register the BroadcastActionLauncher for SCREEN_ON broadcasts, cannot be done via ApplicationManifest.
    actionLauncher = new BroadcastActionLauncher();
    this.registerReceiver(actionLauncher, new IntentFilter(Intent.ACTION_SCREEN_ON));
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.d(TAG, "onBind()");
    return binder;
  }
  
  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy()");
    super.onDestroy();
    
    // Unregister from changes in polygon geometries, location subscribers, location updates and SCREEN_ON broadcasts.
    applicationStorage.polygons.removeGeometryChangeListener(this);
    applicationStorage.actions.removeLocationSubscriberChangeListener(this);
    locationManager.removeLocationUpdates(this);
    this.unregisterReceiver(actionLauncher);
  }
  
  public void onGeofencingPreferenceChange(boolean enabled) {
    Log.d(TAG, "Geofencing preference set to: " + enabled);
    if(enabled)
      locationManager.requestLocationUpdates(MINIMUM_INTERVAL_MS, this);
    else
      locationManager.removeLocationUpdates(this);
  }

  public void sendPolygonBroadcasts() {
    Log.d(TAG, "sendPolygonBroadcasts()");
    
    // Polygons we occupied before, we occupy now.
    List<TaggedPolygon<TaggedPoint>> polygonsIOccupied = applicationStorage.polygons.getPolygonsOccupied();
    TaggedPoint bestPoint = new TaggedPoint(0, bestLocation.getLongitude(), bestLocation.getLatitude());
    List<TaggedPolygon<TaggedPoint>> polygonsIOccupy = applicationStorage.polygons.getPolygonsContainingPoint(bestPoint);

    // Figure out which polygons we have left.
    boolean have = true;
    for(TaggedPolygon<TaggedPoint> polygonOccupied : polygonsIOccupied) {
      have = false;
      for(TaggedPolygon<TaggedPoint> polygon : polygonsIOccupy) {
        if(polygonOccupied.getID() == polygon.getID())
          have = true;
      }
      if(have == false) {
        applicationStorage.polygons.setPolygonOccupancy(polygonOccupied.getID(), false);

        // Send the polygon exit broadcast.
        double[] phoneLocation = {bestLocation.getLatitude(), bestLocation.getLongitude()};
        Intent polygonEnterIntent = new Intent(POLYGON_EXIT);
        polygonEnterIntent.putExtra(POLYGON_ID, polygonOccupied.getID());
        polygonEnterIntent.putExtra(POLYGON_LABEL, polygonOccupied.getLabel());
        polygonEnterIntent.putExtra(PHONE_LOCATION, phoneLocation);
        sendBroadcast(polygonEnterIntent);
      }
    }

    // Figure out which polygons we have entered.
    for(TaggedPolygon<TaggedPoint> polygon : polygonsIOccupy) {
      have = false;
      for(TaggedPolygon<TaggedPoint> polygonOccupied : polygonsIOccupied) {
        if(polygon.getID() == polygonOccupied.getID())
          have = true;
      }
      if(have == false) {
        applicationStorage.polygons.setPolygonOccupancy(polygon.getID(), true);

        // Send the polygon enter broadcast.
        double[] phoneLocation = {bestLocation.getLatitude(), bestLocation.getLongitude()};
        Intent polygonEnterIntent = new Intent(POLYGON_ENTER);
        polygonEnterIntent.putExtra(POLYGON_ID, polygon.getID());
        polygonEnterIntent.putExtra(POLYGON_LABEL, polygon.getLabel());
        polygonEnterIntent.putExtra(PHONE_LOCATION, phoneLocation);
        sendBroadcast(polygonEnterIntent);
      }
    }
  }

  @Override
  public void onBetterLocationAvailable(Location location) {
    Log.d(TAG, "onBetterLocationAvailable()");
    bestLocation = location;
    
    // If we have any location update subscribers oblige them.
    List<String> locationSubscribers = applicationStorage.actions.getLocationSubscribers();
    if(locationSubscribers.size() > 0) {
      locationManager.requestLocationUpdates(SUBSCRIBER_INTERVAL_MS, this);
      for(String subscriber : locationSubscribers)
        SMSSender.sendTextMessage(subscriber, Double.toString(bestLocation.getLatitude()) + ", " + Double.toString(bestLocation.getLongitude()));
    }
    
    // Otherwise conserve power by only updating location when reasonable.
    else {
      TaggedPoint bestPoint = GoogleGeometryFactory.buildTaggedPoint(bestLocation);
      double device_velocity_mps = AVG_WALKING_VELOCITY_MPS;
      double time_to_polygon_ms = REGULAR_INTERVAL_MS * 2;
      double distance_to_polygon_m = applicationStorage.polygons.getDistanceToClosestPolygon(bestPoint);
      
      // Assume the user is walking if device velocity unknown.
      if(location.getSpeed() > 0)
        device_velocity_mps = location.getSpeed();
      
      // Estimate how many seconds it will take to reach the closest polygon.
      if(distance_to_polygon_m > 0)
        time_to_polygon_ms = (distance_to_polygon_m / device_velocity_mps) * 1000;
      
      Log.d(TAG, "Closest polygon is " + distance_to_polygon_m + "m & " + (long) time_to_polygon_ms + "ms away @" + device_velocity_mps + "m/s.");
      
      // Allow the GPS to sleep for at most 1/2 the time it would take to reach the closest polygon.
      if((long) (time_to_polygon_ms / 2) > REGULAR_INTERVAL_MS)
        locationManager.requestLocationUpdates(REGULAR_INTERVAL_MS, this);
      else if((long) (time_to_polygon_ms / 2) > MINIMUM_INTERVAL_MS)
        locationManager.requestLocationUpdates((long) (time_to_polygon_ms / 2), this);
      else
        locationManager.requestLocationUpdates(MINIMUM_INTERVAL_MS, this);
    }
    
    sendPolygonBroadcasts();
  }

  @Override
  public void onGeometryChange() {
    if(bestLocation != null)
      onBetterLocationAvailable(bestLocation);
  }

  @Override
  public void onSubscriberChange() {
    if(bestLocation != null)
      onBetterLocationAvailable(bestLocation);
  }

  @Override
  public void onLocationsDisabled(List<String> missingProviders) {
    // Nothing to do.
  }

  @Override
  public void onLocationsEnabled() {
    // Nothing to do.
  }
}

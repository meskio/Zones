package org.anhonesteffort.polygons.location;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class BetterLocationManager implements LocationListener {
  private static final String TAG = "org.anhonesteffort.zoneDb.location.BetterLocationManager";
  
  private Context context;
  private LocationManager locationManager;
  private List<LocationRequest> locationRequests;
  
  private class LocationRequest {
    public BetterLocationListener listener;
    public long minumum_interval_ms;
    public Location lastLocationUpdate;
    
    public LocationRequest(long minumum_interval_ms, BetterLocationListener listener) {
      this.minumum_interval_ms = minumum_interval_ms;
      this.listener = listener;
    }
  }
  
  public BetterLocationManager(Context applicationContext) {
    context = applicationContext;
    locationRequests = new ArrayList<LocationRequest>();
    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
  }
  
  private void addLocationRequest(long minumum_interval_ms, BetterLocationListener listener) {
    Log.d(TAG, "Added new location listener with " + minumum_interval_ms + "ms minimum interval.");
    LocationRequest request = new LocationRequest(minumum_interval_ms, listener); 
    locationRequests.add(request);
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, request.minumum_interval_ms, 0, this);
    if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
      request.listener.onLocationsEnabled();
  }
  
  private void updateLocationRequest(long new_minumum_interval_ms, LocationRequest existingRequest) {
    double high_bound = existingRequest.minumum_interval_ms + (existingRequest.minumum_interval_ms * 0.10);
    double low_bound = existingRequest.minumum_interval_ms - (existingRequest.minumum_interval_ms * 0.10);
    
    // Only update the minimum interval if it is +/- 10% different.
    if(new_minumum_interval_ms >= high_bound || new_minumum_interval_ms <= low_bound) {
      Log.d(TAG, "Updated location listener to " + new_minumum_interval_ms + "ms minimum interval.");
      existingRequest.minumum_interval_ms = new_minumum_interval_ms;
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, existingRequest.minumum_interval_ms, 0, this);
    }
  }
  
  private boolean newLocationBetter(Location newLocation, LocationRequest request) {
    double score = 0;
    Log.d(TAG, "old: " + request.lastLocationUpdate.getAccuracy() + ", " + request.lastLocationUpdate.getTime() + ", " + request.lastLocationUpdate.getSpeed());
    Log.d(TAG, "new: " + newLocation.getAccuracy() + ", " + newLocation.getTime() + ", " + newLocation.getSpeed());
    
    // The location gets points for accuracy.
    score = (request.lastLocationUpdate.getAccuracy() / newLocation.getAccuracy()) * 100.0;
    Log.d(TAG, "points for accuracy: " + score);
    
    // The location gets points for timeliness.
    score += ((double) (newLocation.getTime() - request.lastLocationUpdate.getTime()) / (double) request.minumum_interval_ms) * 100.0;
    Log.d(TAG, "points for accuracy & new time: " + score);
    
    // The location gets points if it has a speed.
    if(newLocation.getSpeed() != (float) 0.0 && request.lastLocationUpdate.getSpeed() == (float) 0.0)
      score += 20;
    
    Log.d(TAG, "points for accuracy, new time & speed: " + score + "/" + 100.0);
    
    if(score >= 100)
      return true;
    return false;
  }
  
  public void requestLocationUpdates(long minumum_interval_ms, BetterLocationListener listener) {
    Log.d(TAG, "requestLocationUpdates()");
    
    boolean new_listener = true;
    for(LocationRequest request : locationRequests) {
      if(request.listener == listener) {
        updateLocationRequest(minumum_interval_ms, request);
        new_listener = false;
        break;
      }
    }
    if(new_listener)
      addLocationRequest(minumum_interval_ms, listener);
  }
  
  public void removeLocationUpdates(BetterLocationListener listener) {
    Log.d(TAG, "removeLocationUpdates()");
    
    LocationRequest remove_request = null;
    for(LocationRequest request : locationRequests) {
      if(request.listener == listener) {
        remove_request = request;
        break;
      }
    }
    if(remove_request != null)
      locationRequests.remove(remove_request);
    if(locationRequests.isEmpty())
      locationManager.removeUpdates(this);
  }

  @Override
  public void onLocationChanged(Location newLocation) {
    for(LocationRequest request : locationRequests) {
      if(request.lastLocationUpdate == null) {
        request.listener.onBetterLocationAvailable(newLocation);
        request.lastLocationUpdate = newLocation;
      }
      else if(newLocationBetter(newLocation, request)) {
        request.listener.onBetterLocationAvailable(newLocation);
        request.lastLocationUpdate = newLocation;
      }
    }
  }

  @Override
  public void onProviderDisabled(String provider) {
    if(provider.equals(LocationManager.GPS_PROVIDER)) {
      Log.d(TAG, "GPS provider disabled, unable to provide location updates.");
      List<String> missingProviders = new ArrayList<String>();
      missingProviders.add(provider);
      
      for(LocationRequest request : locationRequests)
        request.listener.onLocationsDisabled(missingProviders);
    }
  }

  @Override
  public void onProviderEnabled(String provider) {
    if(provider.equals(LocationManager.GPS_PROVIDER)) {
      Log.d(TAG, "GPS provider enabled, can now provide location updates.");
      for(LocationRequest request : locationRequests)
        request.listener.onLocationsEnabled();
    }
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
    // Nothing to do, yet.
  }
}

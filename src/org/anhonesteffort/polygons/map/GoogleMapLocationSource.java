package org.anhonesteffort.polygons.map;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.google.android.gms.maps.LocationSource;

// For bypassing use of Google Play Services by Google Maps "my location" layer.
public class GoogleMapLocationSource implements LocationSource, LocationListener {
  private OnLocationChangedListener mapLocationListener;

  @Override
  public void activate(OnLocationChangedListener listener) {
    mapLocationListener = listener;
  }

  @Override
  public void deactivate() {
    mapLocationListener = null;
  }

  @Override
  public void onLocationChanged(Location location) {
    if(mapLocationListener != null)
      mapLocationListener.onLocationChanged(location);
  }

  @Override
  public void onProviderDisabled(String provider) {
    // Nothing to do.
  }

  @Override
  public void onProviderEnabled(String provider) {
    // Nothing to do.
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
    // Nothing to do.
  }
};
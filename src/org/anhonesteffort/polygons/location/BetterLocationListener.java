package org.anhonesteffort.polygons.location;

import android.location.Location;

import java.util.List;

public interface BetterLocationListener {

  // Called when a better location is available.
  void onBetterLocationAvailable(Location location);
  
  // Called when the state of the device has changed such that location updates cannot be provided.
  // missingProviders is a list of providers that must be enabled to receive location updates.
  void onLocationsDisabled(List<String> missingProviders);
  
  // Called when the state of the device has changed such that location updates can be provided.
  void onLocationsEnabled();
  
}

package org.anhonesteffort.polygons.map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.util.SparseArray;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.*;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.database.DatabaseHelper;
import org.anhonesteffort.polygons.database.model.PointRecord;
import org.anhonesteffort.polygons.database.model.ZoneRecord;
import org.anhonesteffort.polygons.map.geometry.MapPoint;
import org.anhonesteffort.polygons.map.geometry.MapZone;
import org.anhonesteffort.polygons.map.ZoneMapActivity.DrawState;

import java.util.ArrayList;
import java.util.List;

public class GoogleZoneMap
  implements OnMapClickListener, OnMapLongClickListener,
    OnMarkerDragListener, OnMarkerClickListener, OnCameraChangeListener {
  
  private static final String TAG = "org.anhonesteffort.polygons.map.GoogleZoneMap";

  private DatabaseHelper databaseHelper;
  private ZoneMapActivity mapActivity;
  
  private GoogleMap googleMap;
  private boolean map_loaded = false;
  private SparseArray<Polygon> mapPolygons = new SparseArray<Polygon>();
  private List<Marker> mapMarkers = new ArrayList<Marker>();

  public GoogleZoneMap(ZoneMapActivity mapActivity) {
    this.mapActivity = mapActivity;
    databaseHelper = DatabaseHelper.getInstance(mapActivity.getApplicationContext());
    initializeMap();
  }

  // Set up the Google Map and all listeners.
  private void initializeMap() {
    if(googleMap == null) {
      googleMap = ((SupportMapFragment) mapActivity.getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
      if(googleMap != null) {
        googleMap.setOnMapClickListener(this);
        googleMap.setOnMapLongClickListener(this);
        googleMap.setOnMarkerDragListener(this);
        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnCameraChangeListener(this);
        //googleMap.setMyLocationEnabled(true);
      }
      else {
        Log.e(TAG, "Map failed to load! Why?!");
        AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
        builder.setTitle(R.string.error_map_load);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mapActivity.startActivity(intent);
          }
        });
        builder.show();
      }
    }
  }

  public void addPoint(MapPoint point) {
    MarkerOptions marker = GoogleGeometryFactory.buildMarkerOptions(point);
    marker.draggable(true);
    marker.icon(BitmapDescriptorFactory.defaultMarker(point.getFillColor()));
    mapMarkers.add(googleMap.addMarker(marker));
  }

  public void removePoint(MapPoint point) {
    Marker removeMarker = null;

    for(Marker marker : mapMarkers) {
      if(point.getId() == Integer.parseInt(marker.getSnippet()))
        removeMarker = marker;
    }

    if(removeMarker != null) {
      removeMarker.remove();
      mapMarkers.remove(removeMarker);
    }
  }
  
  public void selectPoint(MapPoint point) {
    for(Marker marker : mapMarkers) {
      if(point.getId() == Integer.parseInt(marker.getSnippet()))
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(point.getSelectedFillColor()));
    }
  }
  
  public ArrayList<MapPoint> getPoints() {
    ArrayList<MapPoint> mapPoints = new ArrayList<MapPoint>();
    for(Marker marker : mapMarkers)
      mapPoints.add(new MapPoint(GoogleGeometryFactory.buildPointRecord(marker)));
    return mapPoints;
  }

  public void addZone(MapZone mapZone) {
    Log.d(TAG, "addZone(), zone_id: " + mapZone.getId());

    PolygonOptions mapPolygonOptions = GoogleGeometryFactory.buildPolygonOptions(mapZone);
    Polygon mapPolygon = googleMap.addPolygon(mapPolygonOptions);

    mapPolygons.append(mapZone.getId(), mapPolygon);
  }

  public void removePolygon(int polygon_id) {
    Log.d(TAG, "deleteZone(), polygon_id: " + polygon_id);
    if(mapPolygons.get(polygon_id, null) != null) {
      mapPolygons.get(polygon_id).remove();
      mapPolygons.remove(polygon_id);
    }
  }

  public int getViewType() {
    return googleMap.getMapType();
  }
  
  public void setViewType(int type) {
    googleMap.setMapType(type);
  }

  public int changeViewType() {
    switch(googleMap.getMapType()) {
      case GoogleMap.MAP_TYPE_NORMAL:
        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        return GoogleMap.MAP_TYPE_TERRAIN;

      case GoogleMap.MAP_TYPE_TERRAIN:
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        return GoogleMap.MAP_TYPE_HYBRID;

      case GoogleMap.MAP_TYPE_HYBRID:
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        return GoogleMap.MAP_TYPE_NORMAL;
    }
    return GoogleMap.MAP_TYPE_TERRAIN;
  }

  public void focusOnPoint(PointRecord point, double zoom) {
    googleMap.moveCamera(
        CameraUpdateFactory.newLatLngZoom(new LatLng(
            point.getY(),
            point.getX()),
            (float) zoom));
  }

  public void focusOnZone(ZoneRecord zoneRecord) {
    PointRecord[] zoneBounds = databaseHelper.zoneDb.getZoneBounds(zoneRecord.getId());
    if(zoneBounds[0].getX() == -1)
      return;

    LatLngBounds mapBounds = new LatLngBounds(
                                          GoogleGeometryFactory.buildLatLng(zoneBounds[0]),
                                          GoogleGeometryFactory.buildLatLng(zoneBounds[1]));

    CameraUpdate manualUpdate = CameraUpdateFactory.newLatLngBounds(mapBounds, 140);
    googleMap.moveCamera(manualUpdate);
  }

  @Override
  public void onCameraChange(CameraPosition position) {
    if(map_loaded == false) {
      map_loaded = true;
      mapActivity.onMapLoad();
    }

    PointRecord mapCenter = new PointRecord(0, position.target.longitude, position.target.latitude);
    addZonesWithinBounds(googleMap.getProjection().getVisibleRegion().latLngBounds);
    mapActivity.onMapViewChange(mapCenter, position.zoom);
  }

  @Override
  public void onMapClick(LatLng clickPoint) {
    mapActivity.onMapClick(GoogleGeometryFactory.buildPointRecord(clickPoint));
  }

  @Override
  public void onMapLongClick(LatLng clickPoint) {
    mapActivity.onMapLongClick(GoogleGeometryFactory.buildPointRecord(clickPoint));
  }

  @Override
  public boolean onMarkerClick(Marker marker) {
    mapActivity.onPointClick(GoogleGeometryFactory.buildPointRecord(marker));
    return false;
  }

  @Override
  public void onMarkerDragStart(Marker dragMarker) {
    mapActivity.onPointMoveStart(GoogleGeometryFactory.buildPointRecord(dragMarker));
    return;
  }

  @Override
  public void onMarkerDrag(Marker marker) {
    return;
  }

  @Override
  public void onMarkerDragEnd(Marker dragMarker) {
    mapActivity.onPointMoveStop(GoogleGeometryFactory.buildPointRecord(dragMarker));
    return;
  }

  // Clear the map and redraw all zoneDb within provided bounds.
  private void addZonesWithinBounds(LatLngBounds bounds) {
    ZoneRecord visibleZone;
    List<ZoneRecord> visibleZoneList;
    PolygonOptions mapPolygonOptions;
    Polygon mapPolygon;

    mapMarkers.clear();
    mapPolygons.clear();
    googleMap.clear();

    visibleZone = GoogleGeometryFactory.buildZoneRecord(bounds);
    visibleZoneList = databaseHelper.zoneDb.getZonesIntersecting(visibleZone);

    for(ZoneRecord zoneRecord : visibleZoneList) {
      if(mapActivity.getState() != DrawState.NEW_POINTS || zoneRecord.getId() != mapActivity.getSelectedZone().getId()) {
        mapPolygonOptions = GoogleGeometryFactory.buildPolygonOptions(new MapZone(zoneRecord));
        mapPolygon = googleMap.addPolygon(mapPolygonOptions);

        mapPolygons.put(zoneRecord.getId(), mapPolygon);
      }
    }
  }

  public void clearPoints() {
    for(Marker marker : mapMarkers)
      marker.remove();
    mapMarkers.clear();
  }
}

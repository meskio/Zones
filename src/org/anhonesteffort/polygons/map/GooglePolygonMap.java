package org.anhonesteffort.polygons.map;

import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.geometry.TaggedPoint;
import org.anhonesteffort.polygons.geometry.TaggedPolygon;
import org.anhonesteffort.polygons.map.PolygonMapActivity.DrawState;
import org.anhonesteffort.polygons.storage.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

public class GooglePolygonMap
  implements OnMapClickListener, OnMapLongClickListener,
    OnMarkerDragListener, OnMarkerClickListener, OnCameraChangeListener {
  
  private static final String TAG               = "org.anhonesteffort.polygons.map.GooglePolygonMap";
  private final static int POLYGON_STROKE_WIDTH = 3;
  private final static int POLYGON_FILL_COLOR   = 0x5F880607;

  private DatabaseHelper applicationStorage;
  private PolygonMapActivity mapActivity;
  
  private GoogleMap googleMap;
  private boolean map_loaded = false;
  private GoogleMapLocationSource locationSource;
  private SparseArray<Polygon> mapPolygons = new SparseArray<Polygon>();
  private List<Marker> mapMarkers = new ArrayList<Marker>();

  public GooglePolygonMap(PolygonMapActivity mapActivity) {
    this.mapActivity = mapActivity;
    applicationStorage = DatabaseHelper.getInstance(mapActivity.getApplicationContext());
    locationSource = new GoogleMapLocationSource();
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
        googleMap.setMyLocationEnabled(true);
        //googleMap.setLocationSource(locationSource);
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

  public void addPoint(TaggedPoint point) {
    MarkerOptions marker = GoogleGeometryFactory.buildMarkerOptions(point);
    marker.draggable(true);
    marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
    mapMarkers.add(googleMap.addMarker(marker));
  }

  public void removePoint(TaggedPoint point) {
    Marker removeMarker = null;
    for(Marker marker : mapMarkers) {
      if(point.getID() == Integer.parseInt(marker.getSnippet()))
        removeMarker = marker;
    }
    if(removeMarker != null) {
      removeMarker.remove();
      mapMarkers.remove(removeMarker);
    }
  }
  
  public void selectPoint(TaggedPoint point) {
    for(Marker marker : mapMarkers) {
      if(point.getID() == Integer.parseInt(marker.getSnippet()))
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
    }
  }
  
  public ArrayList<TaggedPoint> getPoints() {
    ArrayList<TaggedPoint> mapPoints = new ArrayList<TaggedPoint>();
    for(Marker marker : mapMarkers)
      mapPoints.add(GoogleGeometryFactory.buildTaggedPoint(marker));
    return mapPoints;
  }

  public void addPolygon(TaggedPolygon<TaggedPoint> polygon) {
    Log.d(TAG, "addPolygon(), polygon_id: " + polygon.getID());
    PolygonOptions mapPolygonOptions = GoogleGeometryFactory.buildPolygonOptions(polygon);
    mapPolygonOptions.fillColor(POLYGON_FILL_COLOR).strokeWidth(POLYGON_STROKE_WIDTH);
    Polygon mapPolygon = googleMap.addPolygon(mapPolygonOptions);
    mapPolygons.append(polygon.getID(), mapPolygon);
  }

  public void removePolygon(int polygon_id) {
    Log.d(TAG, "removePolygon(), polygon_id: " + polygon_id);
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

  public void focusOnPoint(TaggedPoint point, double zoom) {
    googleMap.moveCamera(
        CameraUpdateFactory.newLatLngZoom(new LatLng(
            point.getY(),
            point.getX()),
            (float) zoom));
  }

  public void focusOnPolygon(TaggedPolygon<TaggedPoint> polygon) {
    TaggedPoint[] polygonPointBounds = applicationStorage.polygons.getPolygonBounds(polygon.getID());
    if(polygonPointBounds[0].getX() == -1)
      return;

    LatLngBounds polygonMapBounds = new LatLngBounds(
        GoogleGeometryFactory.buildLatLng(polygonPointBounds[0]),
        GoogleGeometryFactory.buildLatLng(polygonPointBounds[1]));

    CameraUpdate manualUpdate = CameraUpdateFactory.newLatLngBounds(polygonMapBounds, 140);
    googleMap.moveCamera(manualUpdate);
  }
  
  public void onMyLocationChange(Location location) {
    locationSource.onLocationChanged(location);
  }

  @Override
  public void onCameraChange(CameraPosition position) {
    if(map_loaded == false) {
      map_loaded = true;
      mapActivity.onMapLoad();
    }

    TaggedPoint mapCenter = new TaggedPoint(0, position.target.longitude, position.target.latitude);
    addPolygonsWithinBounds(googleMap.getProjection().getVisibleRegion().latLngBounds);
    mapActivity.onMapViewChange(mapCenter, position.zoom);
  }

  @Override
  public void onMapClick(LatLng clickPoint) {
    mapActivity.onMapClick(GoogleGeometryFactory.buildTaggedPoint(clickPoint));
  }

  @Override
  public void onMapLongClick(LatLng clickPoint) {
    mapActivity.onMapLongClick(GoogleGeometryFactory.buildTaggedPoint(clickPoint));
  }

  @Override
  public boolean onMarkerClick(Marker marker) {
    mapActivity.onPointClick(GoogleGeometryFactory.buildTaggedPoint(marker));
    return false;
  }

  @Override
  public void onMarkerDragStart(Marker dragMarker) {
    mapActivity.onPointMoveStart(GoogleGeometryFactory.buildTaggedPoint(dragMarker));
    return;
  }

  @Override
  public void onMarkerDrag(Marker marker) {
    return;
  }

  @Override
  public void onMarkerDragEnd(Marker dragMarker) {
    mapActivity.onPointMoveStop(GoogleGeometryFactory.buildTaggedPoint(dragMarker));
    return;
  }

  // Clear the map and redraw all polygons within provided bounds.
  private void addPolygonsWithinBounds(LatLngBounds bounds) {
    TaggedPolygon<TaggedPoint> visiblePolygon;
    List<TaggedPolygon<TaggedPoint>> polygons;
    PolygonOptions mapPolygonOptions;
    Polygon mapPolygon;

    mapMarkers.clear();
    mapPolygons.clear();
    googleMap.clear();

    visiblePolygon = GoogleGeometryFactory.buildTaggedPolygon(bounds);
    polygons = applicationStorage.polygons.getPolygonsIntersectingPolygon(visiblePolygon);

    for(TaggedPolygon<TaggedPoint> polygon : polygons) {
      if(mapActivity.getState() != DrawState.NEW_POINTS || polygon.getID() != mapActivity.getSelectedPolygon().getID()) {
        mapPolygonOptions = GoogleGeometryFactory.buildPolygonOptions(polygon);
        mapPolygonOptions.fillColor(POLYGON_FILL_COLOR).strokeWidth(POLYGON_STROKE_WIDTH);
        mapPolygon = googleMap.addPolygon(mapPolygonOptions);
        mapPolygons.put(polygon.getID(), mapPolygon);
      }
    }
  }

  public void clearPoints() {
    for(Marker marker : mapMarkers)
      marker.remove();
    mapMarkers.clear();
  }
}

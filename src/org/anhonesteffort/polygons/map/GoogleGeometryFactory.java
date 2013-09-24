package org.anhonesteffort.polygons.map;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import org.anhonesteffort.polygons.database.model.PointRecord;
import org.anhonesteffort.polygons.database.model.ZoneRecord;
import org.anhonesteffort.polygons.map.geometry.MapZone;

public class GoogleGeometryFactory {

  public static MarkerOptions buildMarkerOptions(PointRecord point) {
    MarkerOptions marker = new MarkerOptions();
    LatLng position = new LatLng(point.getY(), point.getX());
    marker.position(position);
    marker.snippet(Integer.toString(point.getId()));
    return marker;
  }

  public static LatLng buildLatLng(PointRecord point) {
    return new LatLng(point.getY(), point.getX());
  }

  public static PolygonOptions buildPolygonOptions(ZoneRecord zoneRecord) {
    PolygonOptions polygonOptions = new PolygonOptions();
    for(PointRecord point : zoneRecord.getPoints())
      polygonOptions.add(buildLatLng(point));
    return polygonOptions;
  }

  public static PolygonOptions buildPolygonOptions(MapZone mapZone) {
    PolygonOptions polygonOptions = new PolygonOptions();

    for(PointRecord point : mapZone.getPoints())
      polygonOptions.add(buildLatLng(point));

    polygonOptions.fillColor(mapZone.getFillColor());
    polygonOptions.strokeColor(mapZone.getStrokeColor());
    polygonOptions.strokeWidth(mapZone.getStrokeWidth());

    return polygonOptions;
  }

  public static PointRecord buildPointRecord(LatLng point) {
    PointRecord newPoint =  new PointRecord(-1, -1, point.longitude, point.latitude);
    return newPoint;
  }

  public static PointRecord buildPointRecord(Marker marker) {
    PointRecord newPoint =  new PointRecord(Integer.parseInt(marker.getSnippet()), -1,
                                                             marker.getPosition().longitude,
                                                             marker.getPosition().latitude);
    return newPoint;
  }

  public static PointRecord buildPointRecord(MarkerOptions marker) {
    PointRecord newPoint =  new PointRecord(Integer.parseInt(marker.getSnippet()), -1,
                                                             marker.getPosition().longitude,
                                                             marker.getPosition().latitude);
    return newPoint;
  }
  
  public static PointRecord buildPointRecord(Location point) {
    PointRecord newPoint =  new PointRecord(-1, -1, point.getLongitude(), point.getLatitude());
    return newPoint;
  }

  public static ZoneRecord buildZoneRecord(PolygonOptions polygonOptions) {
    ZoneRecord zoneRecord = new ZoneRecord(-1, "");
    for(LatLng point : polygonOptions.getPoints()) {
      zoneRecord.getPoints().add(buildPointRecord(point));
    }
    return zoneRecord;
  }

  public static ZoneRecord buildZoneRecord(LatLngBounds bounds) {
    ZoneRecord zoneRecord = new ZoneRecord(-1, "");
    zoneRecord.getPoints().add(buildPointRecord(bounds.northeast));
    zoneRecord.getPoints().add(buildPointRecord(new LatLng(bounds.southwest.latitude, bounds.northeast.longitude)));
    zoneRecord.getPoints().add(buildPointRecord(bounds.southwest));
    zoneRecord.getPoints().add(buildPointRecord(new LatLng(bounds.northeast.latitude, bounds.southwest.longitude)));
    return zoneRecord;
  }

}

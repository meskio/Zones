package org.anhonesteffort.polygons.map;

import java.util.ArrayList;

import org.anhonesteffort.polygons.geometry.TaggedPoint;
import org.anhonesteffort.polygons.geometry.TaggedPolygon;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

public class GoogleGeometryFactory {

  public static MarkerOptions buildMarkerOptions(TaggedPoint point) {
    MarkerOptions marker = new MarkerOptions();
    LatLng position = new LatLng(point.getY(), point.getX());
    marker.position(position);
    marker.snippet(Integer.toString(point.getID()));
    return marker;
  }

  public static LatLng buildLatLng(TaggedPoint point) {
    return new LatLng(point.getY(), point.getX());
  }

  public static PolygonOptions buildPolygonOptions(TaggedPolygon<TaggedPoint> polygonRecord) {
    PolygonOptions polygonOptions = new PolygonOptions();
    for(TaggedPoint point : polygonRecord.getPoints())
      polygonOptions.add(buildLatLng(point));
    return polygonOptions;
  }

  public static TaggedPoint buildTaggedPoint(LatLng point) {
    TaggedPoint newPoint =  new TaggedPoint(-1, point.longitude, point.latitude);
    return newPoint;
  }

  public static TaggedPoint buildTaggedPoint(MarkerOptions marker) {
    TaggedPoint newPoint =  new TaggedPoint(Integer.parseInt(marker.getSnippet()),
                                                             marker.getPosition().longitude,
                                                             marker.getPosition().latitude);
    return newPoint;
  }

  public static TaggedPoint buildTaggedPoint(Marker marker) {
    TaggedPoint newPoint =  new TaggedPoint(Integer.parseInt(marker.getSnippet()),
                                                             marker.getPosition().longitude,
                                                             marker.getPosition().latitude);
    return newPoint;
  }
  
  public static TaggedPoint buildTaggedPoint(Location point) {
    TaggedPoint newPoint =  new TaggedPoint(-1, point.getLongitude(), point.getLatitude());
    return newPoint;
  }

  public static TaggedPolygon<TaggedPoint> buildTaggedPolygon(PolygonOptions polygonOptions) {
    TaggedPolygon<TaggedPoint> polygon = new TaggedPolygon<TaggedPoint>(-1, "", new ArrayList<TaggedPoint>());
    for(LatLng point : polygonOptions.getPoints()) {
      polygon.getPoints().add(buildTaggedPoint(point));
    }
    return polygon;
  }

  public static TaggedPolygon<TaggedPoint> buildTaggedPolygon(LatLngBounds bounds) {
    TaggedPolygon<TaggedPoint> polygon = new TaggedPolygon<TaggedPoint>(-1, "", new ArrayList<TaggedPoint>());
    polygon.getPoints().add(buildTaggedPoint(bounds.northeast));
    polygon.getPoints().add(buildTaggedPoint(new LatLng(bounds.southwest.latitude, bounds.northeast.longitude)));
    polygon.getPoints().add(buildTaggedPoint(bounds.southwest));
    polygon.getPoints().add(buildTaggedPoint(new LatLng(bounds.northeast.latitude, bounds.southwest.longitude)));
    return polygon;
  }

}

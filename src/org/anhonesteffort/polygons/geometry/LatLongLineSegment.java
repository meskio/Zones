package org.anhonesteffort.polygons.geometry;


public class LatLongLineSegment extends LineSegment {
  
  public LatLongLineSegment(TaggedPoint point1, TaggedPoint point2) {
    super(point1, point2);
    
    super.point1X = point1.getX() + 180;
    super.point1Y = point1.getY() + 90;
    super.point2X = point2.getX() + 180;
    super.point2Y = point2.getY() + 90;
  }

}

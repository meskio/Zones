package org.anhonesteffort.polygons.database.model;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * Programmer: rhodey
 * Date: 9/6/13
 */
public class ZoneRecord {
  private static final String TAG = "org.anhonesteffort.database.model.ZoneRecord";

  public static final int MIN_POINTS        = 3;
  public static final int MAX_POINTS        = 10;
  public static final int OK                = 0;
  public static final int TOO_FEW_POINTS    = 1;
  public static final int TOO_MANY_POINTS   = 2;
  public static final int CANNOT_MAKE_VALID = 3;

  private boolean success;
  private ZoneRecord tempZone;

  private int id;
  private String label;
  private List<PointRecord> points;

  public ZoneRecord(int id, String label) {
    this.id = id;
    this.label = label;
    points = new LinkedList<PointRecord>();
  }

  public ZoneRecord(int id, String label, List<PointRecord> points) {
    this.id = id;
    this.label = label;
    this.points = points;
  }

  public int getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public List<PointRecord> getPoints() {
    return points;
  }

  private void testAllCombinations(List<PointRecord> testPoints, int k) {
    for(int i = k; i < testPoints.size(); i++) {
      java.util.Collections.swap(testPoints, i, k);
      testAllCombinations(testPoints, k+1);

      if(success)
        return;

      java.util.Collections.swap(testPoints, k, i);
    }

    if(k == testPoints.size() -1) {
      tempZone.getPoints().addAll(testPoints);
      tempZone.getPoints().add(testPoints.get(0));

      if(tempZone.intersectsSelf())
        tempZone.getPoints().clear();
      else {
        success = true;
        points = new LinkedList<PointRecord>();
        points.addAll(testPoints);
      }
    }
  }

  // Returns true if polygon intersects itself one or more times.
  private boolean intersectsSelf() {
    LatLonLineSegment line1;
    LatLonLineSegment line2;

    // Loop through all the lines that form the polygon.
    for(int i = 0; i < (points.size() - 1); i++) {
      line1 = new LatLonLineSegment(points.get(i), points.get(i+1));

      // Check if that line segment intersects with any other line segment.
      for(int j = (i + 1); j < (points.size() - 1); j++) {
        line2 = new LatLonLineSegment(points.get(j), points.get(j+1));
        line2.shrink(0.000000000001, 0.000000000001);
        if(line1.intersects(line2))
          return true;
      }
    }
    return false;
  }

  // Attempts to create a valid polygon which does not intersect itself.
  public int makeValid() {
    tempZone = new ZoneRecord(id, label);

    // Need at least 3 points to make a polygon.
    if(points.size() < MIN_POINTS) {
      Log.d(TAG, "Too few points!");
      return TOO_FEW_POINTS;
    }

    // Permutations on 11 points requires too much memory.
    if(points.size() > MAX_POINTS) {
      Log.d(TAG, "Too many points!");
      return TOO_MANY_POINTS;
    }

    // Iterate through all possible permutations until a valid polygon is created.
    success = false;
    tempZone.getPoints().clear();
    testAllCombinations(points, 0);

    if(success)
      return OK;

    // Failed to create a valid polygon, shouldn't ever get here.
    points.clear();
    Log.e(TAG, "Cannot make valid, why?!");
    return CANNOT_MAKE_VALID;
  }

  protected class LatLonLineSegment {
    private double point1X;
    private double point1Y;
    private double point2X;
    private double point2Y;

    public LatLonLineSegment(PointRecord point1, PointRecord point2) {
      point1X = point1.getX() + 180;
      point1Y = point1.getY() + 90;
      point2X = point2.getX() + 180;
      point2Y = point2.getY() + 90;
    }

    // Shrinks the line segment by loss_x on the x-axis and loss_y on the y-axis.
    public void shrink(double loss_x, double loss_y) {
      if(point1X < point2X) {
        this.point1X = this.point1X + loss_x;
        this.point2X = this.point2X - loss_x;
      }
      else {
        this.point2X = this.point2X + loss_x;
        this.point1X = this.point1X - loss_x;
      }
      if(point1Y < point2Y) {
        this.point1Y = this.point1Y + loss_y;
        this.point2Y = this.point2Y - loss_y;
      }
      else {
        this.point2Y = this.point2Y + loss_y;
        this.point1Y = this.point1Y - loss_y;
      }
    }

    // Returns slope of the line segment.
    public double getSlope() {
      return (point1Y - point2Y) / (point1X - point2X);
    }

    // Returns true if the two line segments intersect.
    public boolean intersects(LatLonLineSegment lineSegment) {
      double y, x;
      double m1, b1;
      double m2, b2;

      // Calculate the slope and y intercept of this line segment.
      m1 = getSlope();
      b1 = this.point1Y - (m1 * this.point1X);

      // Calculate the slope and y intercept of the provided line segment.
      m2 = lineSegment.getSlope();
      b2 = lineSegment.point1Y - (m2 * lineSegment.point1X);

      // Calculate the x intercept of the two line segments.
      x = (b2 - b1) / (m1 - m2);

      // Calculate the y intercept of the two line segments.
      y = (m1 * x) + b1;

      // Check if the x intercept is within the test line segment's boundaries.
      if((x >= lineSegment.point1X && x <= lineSegment.point2X) || (x >= lineSegment.point2X && x <= lineSegment.point1X)) {
        // Check if the y intercept is within this line segment's boundaries.
        if((y >= this.point1Y && y <= this.point2Y) || (y >= this.point2Y && y <= this.point1Y))
          return true;
      }
      return false;
    }
  }

}

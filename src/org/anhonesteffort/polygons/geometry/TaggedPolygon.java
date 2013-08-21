package org.anhonesteffort.polygons.geometry;

import java.util.LinkedList;
import java.util.List;

import android.util.Log;

public class TaggedPolygon<T extends TaggedPoint> implements Comparable<TaggedPolygon<T>> {
  private static final String TAG = "org.anhonesteffort.polygons.geometry.TaggedPolygon";
  
  public static final int MIN_POINTS        = 3;
  public static final int MAX_POINTS        = 10;
  public static final int OK                = 0;
  public static final int TOO_FEW_POINTS    = 1;
  public static final int TOO_MANY_POINTS   = 2;
  public static final int CANNOT_MAKE_VALID = 3;

  private int id;
  private String label;
  private List<T> points;

  private boolean success;
  private TaggedPolygon<T> tempPolygon;

  public TaggedPolygon(int id, String label, List<T> points) {
    this.id = id;
    this.label = label;
    this.points = points;
  }

  public int getID() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public List<T> getPoints() {
    return points;
  }

  public void setID(int id) {
    this.id = id;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setPoints(List<T> points) {
    this.points = points;
  }

  // Calculates all permutations on a list of points.
  private void testAllCombinations(List<T> testPoints, int k) {
    for(int i = k; i < testPoints.size(); i++) {
      java.util.Collections.swap(testPoints, i, k);
      testAllCombinations(testPoints, k+1);

      if(success)
        return;

      java.util.Collections.swap(testPoints, k, i);
    }

    if(k == testPoints.size() -1) {
      tempPolygon.getPoints().addAll(testPoints);
      tempPolygon.getPoints().add(testPoints.get(0));

      if(tempPolygon.intersectsSelf())
        tempPolygon.getPoints().clear();
      else {
        success = true;
        points = new LinkedList<T>();
        points.addAll(testPoints);
      }
    }
  }

  // Attempts to create a valid polygon which does not intersect itself.
  public int makeValid() {
    tempPolygon = new TaggedPolygon<T>(id, label, new LinkedList<T>());

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
    tempPolygon.getPoints().clear();
    testAllCombinations(points, 0);
    if(success)
      return OK;

    // Failed to create a valid polygon, shouldn't ever get here.
    points.clear();
    Log.e(TAG, "Cannot make valid, why?!");
    return CANNOT_MAKE_VALID;
  }

  // Returns true if polygon intersects itself one or more times.
  public boolean intersectsSelf() {
    LatLongLineSegment testLine;
    LatLongLineSegment testLine2;

    // Loop through all the lines that form the polygon.
    for(int i = 0; i < (points.size() - 1); i++) {
      testLine = new LatLongLineSegment(points.get(i), points.get(i+1));

      // Check if that line segment intersects with any other line segment.
      for(int j = (i + 1); j < (points.size() - 1); j++) {
        testLine2 = new LatLongLineSegment(points.get(j), points.get(j+1));
        testLine2.shrink(0.000000000001, 0.000000000001);
        if(testLine.intersects(testLine2))
          return true;
      }
    }
    return false;
  }

  @Override
  public int compareTo(TaggedPolygon<T> another) {
    return label.compareTo(another.getLabel());
  }

}

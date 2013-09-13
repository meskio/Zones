package org.anhonesteffort.polygons.map.geometry;

import org.anhonesteffort.polygons.database.model.PointRecord;

/**
 * Programmer: rhodey
 * Date: 9/6/13
 */
public class MapPoint extends PointRecord {

  private final float defaultFillColor  = 0.0f;
  private final float selectedFillColor = 210.0f;

  private float fillColor = defaultFillColor;

  public MapPoint(PointRecord point) {
    super(point.getId(), point.getX(), point.getY());
  }

  public void setFillColor(float fillColor) {
    this.fillColor = fillColor;
  }

  public float getFillColor() {
    return fillColor;
  }

  public float getSelectedFillColor() {
    return selectedFillColor;
  }

}

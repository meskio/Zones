package org.anhonesteffort.polygons.map.geometry;

import org.anhonesteffort.polygons.database.model.PointRecord;

/**
 * Programmer: rhodey
 * Date: 9/6/13
 */
public class MapPoint extends PointRecord {

  private final float FILL_COLOR          = 0.0f;
  private final float SELECTED_FILL_COLOR = 210.0f;

  private float fillColor = FILL_COLOR;

  public MapPoint(PointRecord point) {
    super(point.getId(), point.getZoneId(), point.getX(), point.getY());
  }

  public void setFillColor(float fillColor) {
    this.fillColor = fillColor;
  }

  public float getFillColor() {
    return fillColor;
  }

  public float getSelectedFillColor() {
    return SELECTED_FILL_COLOR;
  }

}

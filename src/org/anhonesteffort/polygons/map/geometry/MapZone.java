package org.anhonesteffort.polygons.map.geometry;

import org.anhonesteffort.polygons.database.model.ZoneRecord;

/**
 * Programmer: rhodey
 * Date: 9/6/13
 */
public class MapZone extends ZoneRecord {

  private final int FILL_COLOR   = 0x5F880607;
  private final int STROKE_COLOR = 0x00;
  private final int STROKE_WIDTH = 3;

  private int fillColor   = FILL_COLOR;
  private int strokeColor = STROKE_COLOR;
  private int strokeWidth = STROKE_WIDTH;

  public MapZone(ZoneRecord zoneRecord) {
    super(zoneRecord.getId(), zoneRecord.getLabel(), zoneRecord.getPoints());
  }

  public void setFillColor(int fillColor) {
    this.fillColor = fillColor;
  }

  public void setStrokeColor(int strokeColor) {
    this.strokeColor = strokeColor;
  }

  public void setStrokeWidth(int strokeWidth) {
    this.strokeWidth = strokeWidth;
  }

  public int getFillColor() {
    return  fillColor;
  }

  public int getStrokeColor() {
    return strokeColor;
  }

  public int getStrokeWidth() {
    return strokeWidth;
  }

}

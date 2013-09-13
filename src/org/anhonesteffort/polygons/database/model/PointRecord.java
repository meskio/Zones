package org.anhonesteffort.polygons.database.model;

/**
 * Programmer: rhodey
 * Date: 9/6/13
 */
public class PointRecord {
  private int id;
  private double x;
  private double y;

  public PointRecord(int id, double x, double y) {
    this.id = id;
    this.x = x;
    this.y = y;
  }

  public int getId() {
    return id;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }
}

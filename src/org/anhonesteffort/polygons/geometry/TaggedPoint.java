package org.anhonesteffort.polygons.geometry;

public class TaggedPoint {
  protected int id;
  protected double x;
  protected double y;

  public TaggedPoint(int id, double x, double y) {
    this.id = id;
    this.x = x;
    this.y = y;
  }

  public int getID() {
    return id;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public void setID(int id) {
    this.id = id;
  }

  public void setX(double x) {
    this.x = x;
  }

  public void setY(double y) {
    this.y = y;
  }
}

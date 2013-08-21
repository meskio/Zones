package org.anhonesteffort.polygons.geometry;

public class LineSegment {
  protected double point1X;
  protected double point1Y;
  protected double point2X;
  protected double point2Y;
  
  public LineSegment(TaggedPoint point1, TaggedPoint point2) {
    point1X = point1.getX();
    point1Y = point1.getY();
    point2X = point2.getX();
    point2Y = point2.getY();
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
  public boolean intersects(LineSegment lineSegment) {
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

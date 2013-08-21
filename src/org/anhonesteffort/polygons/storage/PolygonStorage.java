package org.anhonesteffort.polygons.storage;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.anhonesteffort.polygons.geometry.TaggedPoint;
import org.anhonesteffort.polygons.geometry.TaggedPolygon;

public class PolygonStorage {
  private static final String TAG = "org.anhonesteffort.polygons.storage.PolygonStorage";
  private DatabaseHelper dbHelper;
  private ArrayList<GeometryChangeListener> listeners = new ArrayList<GeometryChangeListener>();

  public PolygonStorage(DatabaseHelper dbHelper) {
    this.dbHelper = dbHelper;
    if(isInitialized() == false) {
      initialize();
      Log.d(TAG, "Initialized the polygon tables.");
    }
  }

  // Start from scratch.
  public void initialize() {
    // Drop all tables.
    dbHelper.exec("SELECT InitSpatialMetaData()");
    dbHelper.exec("DROP TABLE IF EXISTS polygon");
    dbHelper.exec("DROP TABLE IF EXISTS point");
    dbHelper.exec("DROP TABLE IF EXISTS occupy");
    dbHelper.exec("DROP TABLE IF EXISTS selected");

    // Create the polygon table.
    dbHelper.exec("CREATE TABLE IF NOT EXISTS polygon (" +
                    "id INTEGER NOT NULL PRIMARY KEY, " +
                    "label VARCHAR(100) NOT NULL" +
                  ")");
    dbHelper.exec("SELECT AddGeometryColumn('polygon', 'geometry', 4326, 'GEOMETRY')");

    // Create the point helper table.
    dbHelper.exec("CREATE TABLE IF NOT EXISTS point (" +
                    "id INTEGER NOT NULL PRIMARY KEY, " +
                    "polygon_id INTEGER NOT NULL, " +
                    "latitude REAL NOT NULL, " +
                    "longitude REAL NOT NULL, " +
                    "FOREIGN KEY (polygon_id) REFERENCES polygon(id) ON DELETE CASCADE" +
                  ")");

    // Create the polygon occupancy table.
    dbHelper.exec("CREATE TABLE IF NOT EXISTS occupy (" +
                    "polygon_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (polygon_id), " +
                    "FOREIGN KEY (polygon_id) REFERENCES polygon(id) ON DELETE CASCADE" +
                  ")");

    // Create the polygon selected table.
    dbHelper.exec("CREATE TABLE IF NOT EXISTS selected (" +
                    "polygon_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (polygon_id), " +
                    "FOREIGN KEY (polygon_id) REFERENCES polygon(id) ON DELETE CASCADE" +
                  ")");
  }

  // Returns true if the polygon tables have already been initialized.
  public boolean isInitialized() {
    BetterStatement polygons = dbHelper.prepare("SELECT 1 FROM polygon LIMIT 1");
    if(polygons.step()) {
      polygons.close();
      return true;
    }
    polygons.close();
    return false;
  }

  public void addGeometryChangeListener(GeometryChangeListener listener) {
    listeners.add(listener);
  }

  public void removeGeometryChangeListener(GeometryChangeListener listener) {
    listeners.remove(listener);
  }

  private void geometryChange() {
    for(GeometryChangeListener listener : listeners) {
      if(listener != null)
      listener.onGeometryChange();
    }
  }
  
  public boolean isPolygonLabelAvailable(String label) {
    BetterStatement polygon = dbHelper.prepare("SELECT id FROM polygon WHERE label = '" + DatabaseHelper.escapeString(label) + "'");
    if(polygon.step()) {
      polygon.close();
      return false;
    }
    polygon.close();
    return true;
  }

  public TaggedPoint addPoint(TaggedPoint point, int polygon_id) {
    dbHelper.exec("INSERT INTO point (id, polygon_id, latitude, longitude) " +
                    "VALUES(NULL, '" + polygon_id + "', '" + point.getY() + "', '" + point.getX() + "')");
  
    BetterStatement points = dbHelper.prepare("SELECT id FROM point ORDER BY id DESC LIMIT 1");
    if(points.step())
      point.setID(points.getInt(0));
    points.close();
    
    return point;
  }
  
  public void removePoint(int point_id) {
    dbHelper.exec("DELETE FROM point WHERE id = '" + point_id + "'");
  }
  
  public TaggedPoint updatePoint(TaggedPoint point, int polygon_id) {
    dbHelper.exec("UPDATE point SET " + 
                    "polygon_id ='" + polygon_id + "', " +
                    "latitude = '" + point.getY() + "', " +
                    "longitude = '" + point.getX() + "' " +
                  "WHERE id = '" + point.getID() + "'");
    return point;
  }
  
  public TaggedPoint getPoint(int point_id) {
    TaggedPoint outPoint = null;
    BetterStatement point = dbHelper.prepare("SELECT id, longitude, latitude FROM point WHERE id = '" + point_id + "'");
    if(point.step())
      outPoint = new TaggedPoint(point.getInt(0), point.getDouble(1), point.getDouble(2));
    
    return outPoint;
  }

  // Add a polygon to storage, return a record of the new polygon.
  public TaggedPolygon<TaggedPoint> addPolygon(String label) {
    Log.d(TAG, "addPolygon(), label: " + label);
    BetterStatement polygons;
    TaggedPolygon<TaggedPoint> polygonRecord = new TaggedPolygon<TaggedPoint>(0, label, new ArrayList<TaggedPoint>());

    dbHelper.exec("INSERT INTO polygon (id, label, geometry) " +
                    "VALUES(NULL, '" + DatabaseHelper.escapeString(label) + "', " +
                    "GeomFromText('POINT(0.0 0.0)', 4326))");

    polygons = dbHelper.prepare("SELECT id FROM polygon ORDER BY id DESC LIMIT 1");
    if(polygons.step())
      polygonRecord.setID(polygons.getInt(0));
    polygons.close();
    return polygonRecord;
  }

  public void removePolygon(int polygon_id) {
    Log.d(TAG, "removePolygon(), id: " + polygon_id);
    dbHelper.exec("DELETE FROM polygon WHERE id = '" + polygon_id + "'");
  }

  // Updates a polygon and all of its points.
  public TaggedPolygon<TaggedPoint> updatePolygon(TaggedPolygon<TaggedPoint> polygon) {
    Log.d(TAG, "updatePolygon(), id: " + polygon.getID() + ", label: " + polygon.getLabel());
    BetterStatement polygons;
    String sql;

    // Remove non-polygons.
    if(polygon.getPoints().size() < TaggedPolygon.MIN_POINTS ||
        polygon.getPoints().size() > TaggedPolygon.MAX_POINTS) {
      removePolygon(polygon.getID());
      polygon.getPoints().clear();
      geometryChange();
      return polygon;
    }

    // Update the points table.
    dbHelper.exec("DELETE FROM point WHERE polygon_id = '" + polygon.getID() + "'");
    for(TaggedPoint point : polygon.getPoints())
      addPoint(point, polygon.getID());

    // Update the polygon's geometry and label.
    polygons = dbHelper.prepare("SELECT id FROM polygon WHERE id = '" + polygon.getID() + "'");
    if(polygons.step()) {
      sql = "UPDATE polygon SET geometry = GeomFromText('POLYGON((";
      for(int i = 0; i < polygon.getPoints().size(); i++) {
        if(i != 0)
          sql += ",";
        sql += polygon.getPoints().get(i).getX() + " " + polygon.getPoints().get(i).getY();
      }
      sql += "," + polygon.getPoints().get(0).getX() + " " + polygon.getPoints().get(0).getY();
      dbHelper.exec(sql + "))', 4326) WHERE id = '" + polygon.getID() + "'");
      dbHelper.exec("UPDATE polygon SET label = '" + DatabaseHelper.escapeString(polygon.getLabel()) + "' WHERE id = '" + polygon.getID() + "'");
    }
    else
      polygon.getPoints().clear();

    polygons.close();
    geometryChange();
    return polygon;
  }
  
  public TaggedPolygon<TaggedPoint> getPolygon(int polygon_id) {
    TaggedPolygon<TaggedPoint> polygonRecord = new TaggedPolygon<TaggedPoint>(0, "", new ArrayList<TaggedPoint>());

    // Find polygon in the table.
    BetterStatement polygons = dbHelper.prepare("SELECT id, label FROM polygon WHERE id = '" + polygon_id + "'");
    if(polygons.step()) {
      polygonRecord.setID(polygons.getInt(0));
      polygonRecord.setLabel(DatabaseHelper.unescapeString(polygons.getString(1)));

      // Add points to the polygon record.
      BetterStatement points = dbHelper.prepare("SELECT id, latitude, longitude FROM point WHERE " +
                                                  "polygon_id = '" + polygons.getInt(0) + "'");
      while(points.step()) {
        TaggedPoint newPoint = new TaggedPoint(points.getInt(0),
                                               points.getDouble(2),
                                               points.getDouble(1));
        polygonRecord.getPoints().add(newPoint);
      }
      points.close();
    }
    polygons.close();
    return polygonRecord;
  }
  
  public List<TaggedPolygon<TaggedPoint>> getPolygons() {
    List<TaggedPolygon<TaggedPoint>> polygonList = new LinkedList<TaggedPolygon<TaggedPoint>>();
    BetterStatement polygons = dbHelper.prepare("SELECT id FROM polygon");

    while(polygons.step())
      polygonList.add(getPolygon(polygons.getInt(0)));
    polygons.close();
    return polygonList;
  }

  public void setPolygonOccupancy(int polygon_id, boolean occupy) {
    Log.d(TAG, "setPolygonOccupancy(), id: " + polygon_id + ", occupancy: " + occupy);
    if(occupy)
      dbHelper.exec("INSERT OR IGNORE INTO occupy (polygon_id) VALUES(" + polygon_id + ")");
    else
      dbHelper.exec("DELETE FROM occupy where polygon_id = '" + polygon_id + "'");
  }
  
  // Returns a list of all polygons currently occupied.
  public List<TaggedPolygon<TaggedPoint>> getPolygonsOccupied() {
    List<TaggedPolygon<TaggedPoint>> occupiedPolygons = new ArrayList<TaggedPolygon<TaggedPoint>>();
    BetterStatement polygons = dbHelper.prepare("SELECT polygon_id FROM occupy");

    while(polygons.step())
      occupiedPolygons.add(getPolygon(polygons.getInt(0)));
    polygons.close();
    return occupiedPolygons;
  }

  public void setPolygonSelected(int polygon_id, boolean selected) {
    if(selected)
      dbHelper.exec("INSERT OR IGNORE INTO selected (polygon_id) VALUES(" + polygon_id + ")");
    else
      dbHelper.exec("DELETE FROM selected where polygon_id = '" + polygon_id + "'");
  }

  public void clearSelectedPolygons() {
    dbHelper.exec("DELETE FROM selected");
  }

  public boolean isPolygonSelected(int polygon_id) {
    BetterStatement polygon = dbHelper.prepare("SELECT polygon_id FROM selected WHERE polygon_id = '" + polygon_id + "'");
    if(polygon.step()) {
      polygon.close();
      return true;
    }
    polygon.close();
    return false;
  }

  // Returns a list of all polygons selected.
  public List<TaggedPolygon<TaggedPoint>> getPolygonsSelected() {
    List<TaggedPolygon<TaggedPoint>> selectedPolygons = new ArrayList<TaggedPolygon<TaggedPoint>>();
    BetterStatement polygons = dbHelper.prepare("SELECT polygon_id FROM selected");

    while(polygons.step())
      selectedPolygons.add(getPolygon(polygons.getInt(0)));
    polygons.close();
    return selectedPolygons;
  }

  // Returns a list of all polygons containing a point, ordered smallest to largest by area.
  public List<TaggedPolygon<TaggedPoint>> getPolygonsContainingPoint(TaggedPoint point) {
    List<TaggedPolygon<TaggedPoint>> polygonList = new ArrayList<TaggedPolygon<TaggedPoint>>();
    BetterStatement polygons = dbHelper.prepare("SELECT id FROM polygon WHERE " +
                                                 "ST_WITHIN(GeomFromText('" +
                                                   "POINT(" + point.getX() + " " + point.getY() + ")'" +
                                                   ", 4326), geometry) " +
                                                 "ORDER BY ST_AREA(geometry) ASC");
    while(polygons.step())
      polygonList.add(getPolygon(polygons.getInt(0)));
    polygons.close();
    return polygonList;
  }

  // Returns a list of all polygons intersecting or contained within the specified polygon.
  public List<TaggedPolygon<TaggedPoint>> getPolygonsIntersectingPolygon(TaggedPolygon<TaggedPoint> polygon) {
    String sql;
    BetterStatement polygons;
    List<TaggedPolygon<TaggedPoint>> polygonList = new ArrayList<TaggedPolygon<TaggedPoint>>();

    // Find all polygons intersecting polygon.
    sql = "SELECT id FROM polygon WHERE ST_Intersects(geometry, GeomFromText('POLYGON((";
    for(int i = 0; i < polygon.getPoints().size(); i++) {
      if(i != 0)
        sql += ",";
      sql += polygon.getPoints().get(i).getX() + " " + polygon.getPoints().get(i).getY();
    }
    sql += "," + polygon.getPoints().get(0).getX() + " " + polygon.getPoints().get(0).getY();
    polygons = dbHelper.prepare(sql + "))', 4326)) ORDER BY ST_AREA(geometry) DESC");

    while(polygons.step())
      polygonList.add(getPolygon(polygons.getInt(0)));
    polygons.close();
    return polygonList;
  }

  public TaggedPoint[] getPolygonBounds(int polygon_id) {
    TaggedPoint[] pointBounds = {new TaggedPoint(-1, -1, -1), new TaggedPoint(-1, -1, -1)};
    BetterStatement bounds = dbHelper.prepare("SELECT " +
                                                "MbrMinX(geometry), " +
                                                "MbrMinY(geometry), " +
                                                "MbrMaxX(geometry), " +
                                                "MbrMaxY(geometry) " +
                                                "FROM polygon WHERE id = '" + polygon_id + "'");
    if(bounds.step()) {
      pointBounds[0].setX(bounds.getDouble(0));
      pointBounds[0].setY(bounds.getDouble(1));
      pointBounds[1].setX(bounds.getDouble(2));
      pointBounds[1].setY(bounds.getDouble(3));
    }
    return pointBounds;
  }

  // For future more efficient use of the GPS.
  public double getDistanceToPolygon(TaggedPoint point, int polygon_id) {
    double distance = -1;
    BetterStatement polygons = dbHelper.prepare("SELECT " +
                          "Distance(" +
                            "GeomFromText('POINT(" + point.getX() + " " + point.getY() + ")', 4326), " +
                            "geometry, " +
                            "0" +
                          ") AS dist " +
                          "FROM polygon " +
                          "WHERE id = '" + polygon_id + "' AND " +
                          "dist > 0.0 " +
                          "ORDER BY dist ASC LIMIT 1");
    if(polygons.step())
      distance = polygons.getDouble(0);
    polygons.close();
    return distance;
  }

  // For more efficient use of the GPS.
  public double getDistanceToClosestPolygon(TaggedPoint point) {
    double distance = -1;
    BetterStatement polygons = dbHelper.prepare("SELECT " +
                                                  "Distance(" +
                                                    "GeomFromText('" +
                                                      "POINT(" +
                                                        point.getX() + " " +
                                                        point.getY() +
                                                      ")', " +
                                                    "4326), " +
                                                    "geometry, " +
                                                    "0" +
                                                  ") AS dist " +
                                                  "FROM polygon " +
                                                  "WHERE dist > 0.0 " +
                                                  "ORDER BY dist ASC LIMIT 1");
    if(polygons.step())
      distance = polygons.getDouble(0);
    polygons.close();
    return distance;
  }
}

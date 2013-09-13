package org.anhonesteffort.polygons.database;

import android.content.ContentValues;
import android.util.Log;

import org.anhonesteffort.polygons.database.model.PointRecord;
import org.anhonesteffort.polygons.database.model.ZoneRecord;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ZoneDatabase {
  private static final String TAG = "org.anhonesteffort.polygons.database.ZoneDatabase";
  private DatabaseHelper dbHelper;
  private ArrayList<GeometryChangeListener> listeners = new ArrayList<GeometryChangeListener>();

  public ZoneDatabase(DatabaseHelper dbHelper) {
    this.dbHelper = dbHelper;
    if(isInitialized() == false) {
      initialize();
      Log.d(TAG, "Initialized the zone tables.");
    }
  }

  // Start from scratch.
  public void initialize() {
    dbHelper.exec("SELECT InitSpatialMetaData()");
    dbHelper.exec("DROP TABLE IF EXISTS zone");
    dbHelper.exec("DROP TABLE IF EXISTS point");
    dbHelper.exec("DROP TABLE IF EXISTS occupy");
    dbHelper.exec("DROP TABLE IF EXISTS selected");

    dbHelper.exec("CREATE TABLE IF NOT EXISTS zone (" +
                    "id INTEGER NOT NULL PRIMARY KEY, " +
                    "label VARCHAR(100) NOT NULL" +
                  ")");
    dbHelper.exec("SELECT AddGeometryColumn('zone', 'geometry', 4326, 'GEOMETRY')");

    dbHelper.exec("CREATE TABLE IF NOT EXISTS point (" +
                    "id INTEGER NOT NULL PRIMARY KEY, " +
                    "zone_id INTEGER NOT NULL, " +
                    "latitude REAL NOT NULL, " +
                    "longitude REAL NOT NULL, " +
                    "FOREIGN KEY (zone_id) REFERENCES zone(id) ON DELETE CASCADE" +
                  ")");

    dbHelper.exec("CREATE TABLE IF NOT EXISTS occupy (" +
                    "zone_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (zone_id), " +
                    "FOREIGN KEY (zone_id) REFERENCES zone(id) ON DELETE CASCADE" +
                  ")");

    dbHelper.exec("CREATE TABLE IF NOT EXISTS selected (" +
                    "zone_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (zone_id), " +
                    "FOREIGN KEY (zone_id) REFERENCES zone(id) ON DELETE CASCADE" +
                  ")");
  }

  public boolean isInitialized() {
    BetterStatement zonesQuery = dbHelper.prepare("SELECT 1 FROM zone LIMIT 1");
    if(zonesQuery.step()) {
      zonesQuery.close();
      return true;
    }
    zonesQuery.close();

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
  
  public boolean isLabelAvailable(String label) {
    BetterStatement zonesQuery = dbHelper.prepare("SELECT id FROM zone WHERE label = '" + DatabaseHelper.escapeString(label) + "'");
    if(zonesQuery.step()) {
      zonesQuery.close();
      return false;
    }
    zonesQuery.close();
    return true;
  }

  public PointRecord addPoint(PointRecord point, int zone_id) {
    int point_id = -1;
    ContentValues values = new ContentValues();

    values.put("zone_id", zone_id);
    values.put("latitude", point.getY());
    values.put("longitude", point.getX());
    dbHelper.insert("point", values);
  
    BetterStatement pointsQuery = dbHelper.prepare("SELECT id FROM point ORDER BY id DESC LIMIT 1");
    if(pointsQuery.step())
      point_id = pointsQuery.getInt(0);
    pointsQuery.close();
    
    return new PointRecord(point_id, point.getX(), point.getY());
  }
  
  public void removePoint(int point_id) {
    dbHelper.exec("DELETE FROM point WHERE id = '" + point_id + "'");
  }
  
  public void updatePoint(PointRecord point, int zone_id) {
    dbHelper.exec("UPDATE point SET " + 
                    "zone_id ='" + zone_id + "', " +
                    "latitude = '" + point.getY() + "', " +
                    "longitude = '" + point.getX() + "' " +
                  "WHERE id = '" + point.getId() + "'");
  }
  
  public PointRecord getPoint(int point_id) {
    PointRecord outPoint = null;
    BetterStatement pointQuery = dbHelper.prepare("SELECT id, longitude, latitude FROM point WHERE id = '" + point_id + "'");
    if(pointQuery.step())
      outPoint = new PointRecord(pointQuery.getInt(0), pointQuery.getDouble(1), pointQuery.getDouble(2));
    
    return outPoint;
  }

  public ZoneRecord addZone(String label) {
    Log.d(TAG, "addZone(), label: " + label);

    ZoneRecord outZone = new ZoneRecord(-1, label);

    ContentValues values = new ContentValues();
    values.put("label", DatabaseHelper.escapeString(label));
    values.put("geometry", "ST_GeomFromText('POINT(0.0 0.0)', 4326)");
    dbHelper.insert("zone", values);

    BetterStatement zonesQuery = dbHelper.prepare("SELECT id FROM zone ORDER BY id DESC LIMIT 1");
    if(zonesQuery.step())
      outZone = new ZoneRecord(zonesQuery.getInt(0), label);
    zonesQuery.close();

    return outZone;
  }

  public void removeZone(int zone_id) {
    Log.d(TAG, "removeZone(), id: " + zone_id);
    dbHelper.exec("DELETE FROM zone WHERE id = '" + zone_id + "'");
  }

  // Updates a zone and all of its points.
  public ZoneRecord updateZone(ZoneRecord zone) {
    Log.d(TAG, "updateZone(), id: " + zone.getId() + ", label: " + zone.getLabel());
    BetterStatement zoneQuery;
    String sql;

    // Enforce point limits.
    if(zone.getPoints().size() < ZoneRecord.MIN_POINTS || zone.getPoints().size() > ZoneRecord.MAX_POINTS) {
      removeZone(zone.getId());
      geometryChange();
      return new ZoneRecord(-1, "");
    }

    // Update the points table.
    dbHelper.exec("DELETE FROM point WHERE zone_id = '" + zone.getId() + "'");
    for(PointRecord point : zone.getPoints())
      addPoint(point, zone.getId());

    // Update the zone's geometry and label.
    zoneQuery = dbHelper.prepare("SELECT id FROM zone WHERE id = '" + zone.getId() + "'");
    if(zoneQuery.step()) {
      sql = "UPDATE zone SET geometry = GeomFromText('POLYGON((";
      for(int i = 0; i < zone.getPoints().size(); i++) {
        if(i != 0)
          sql += ",";
        sql += zone.getPoints().get(i).getX() + " " + zone.getPoints().get(i).getY();
      }
      sql += "," + zone.getPoints().get(0).getX() + " " + zone.getPoints().get(0).getY();
      dbHelper.exec(sql + "))', 4326) WHERE id = '" + zone.getId() + "'");
      dbHelper.exec("UPDATE zone SET label = '" + DatabaseHelper.escapeString(zone.getLabel()) +
                    "' WHERE id = '" + zone.getId() + "'");
    }
    else
      zone = new ZoneRecord(-1, "");

    zoneQuery.close();
    geometryChange();
    return zone;
  }
  
  public ZoneRecord getZone(int zone_id) {
    ZoneRecord zone = new ZoneRecord(-1, "");

    // Find zone in the table.
    BetterStatement zoneQuery = dbHelper.prepare("SELECT id, label FROM zone WHERE id = '" + zone_id + "'");
    if(zoneQuery.step()) {
      zone = new ZoneRecord(zoneQuery.getInt(0), DatabaseHelper.unescapeString(zoneQuery.getString(1)));

      // Add points to the zone record.
      BetterStatement pointsQuery = dbHelper.prepare("SELECT id, latitude, longitude FROM point WHERE " +
                                                  "zone_id = '" + zoneQuery.getInt(0) + "'");
      while(pointsQuery.step())
        zone.getPoints().add(getPoint(pointsQuery.getInt(0)));
      pointsQuery.close();
    }
    zoneQuery.close();

    return zone;
  }
  
  public List<ZoneRecord> getZones() {
    List<ZoneRecord> zoneList = new LinkedList<ZoneRecord>();
    BetterStatement zonesQuery = dbHelper.prepare("SELECT id FROM zone");

    while(zonesQuery.step())
      zoneList.add(getZone(zonesQuery.getInt(0)));
    zonesQuery.close();

    return zoneList;
  }

  public void setZoneOccupancy(int zone_id, boolean occupy) {
    Log.d(TAG, "setZoneOccupancy(), id: " + zone_id + ", occupancy: " + occupy);

    if(occupy) {
      ContentValues values = new ContentValues();
      values.put("zone_id", zone_id);
      dbHelper.insert("occupy", values);
    }
    else
      dbHelper.exec("DELETE FROM occupy where zone_id = '" + zone_id + "'");
  }

  public List<ZoneRecord> getZonesOccupied() {
    List<ZoneRecord> zoneList = new ArrayList<ZoneRecord>();
    BetterStatement zonesQuery = dbHelper.prepare("SELECT zone_id FROM occupy");

    while(zonesQuery.step())
      zoneList.add(getZone(zonesQuery.getInt(0)));
    zonesQuery.close();

    return zoneList;
  }

  public void setZoneSelected(int zone_id, boolean selected) {
    if(selected) {
      ContentValues values = new ContentValues();
      values.put("zone_id", zone_id);
      dbHelper.insert("selected", values);
    }
    else
      dbHelper.exec("DELETE FROM selected where zone_id = '" + zone_id + "'");
  }

  public void clearSelectedZones() {
    dbHelper.exec("DELETE FROM selected");
  }

  public boolean isZoneSelected(int zone_id) {
    BetterStatement zoneQuery = dbHelper.prepare("SELECT zone_id FROM selected WHERE zone_id = '" + zone_id + "'");

    if(zoneQuery.step()) {
      zoneQuery.close();
      return true;
    }
    zoneQuery.close();

    return false;
  }

  public List<ZoneRecord> getZonesSelected() {
    List<ZoneRecord> zoneList = new ArrayList<ZoneRecord>();
    BetterStatement zonesQuery = dbHelper.prepare("SELECT zone_id FROM selected");

    while(zonesQuery.step())
      zoneList.add(getZone(zonesQuery.getInt(0)));
    zonesQuery.close();

    return zoneList;
  }

  public List<ZoneRecord> getZonesContainingPoint(PointRecord point) {
    List<ZoneRecord> zoneList = new ArrayList<ZoneRecord>();
    BetterStatement zonesQuery = dbHelper.prepare("SELECT id FROM zone WHERE " +
                                                    "ST_WITHIN(GeomFromText('" +
                                                      "POINT(" + point.getX() + " " + point.getY() + ")'" +
                                                      ", 4326), geometry) " +
                                                  "ORDER BY ST_AREA(geometry) ASC");
    while(zonesQuery.step())
      zoneList.add(getZone(zonesQuery.getInt(0)));
    zonesQuery.close();

    return zoneList;
  }

  public List<ZoneRecord> getZonesIntersecting(ZoneRecord zone) {
    String sql;
    BetterStatement zonesQuery;
    List<ZoneRecord> zoneList = new ArrayList<ZoneRecord>();

    // Find all zones intersecting the provided zone.
    sql = "SELECT id FROM zone WHERE ST_Intersects(geometry, GeomFromText('POLYGON((";
    for(int i = 0; i < zone.getPoints().size(); i++) {
      if(i != 0)
        sql += ",";
      sql += zone.getPoints().get(i).getX() + " " + zone.getPoints().get(i).getY();
    }
    sql += "," + zone.getPoints().get(0).getX() + " " + zone.getPoints().get(0).getY();
    zonesQuery = dbHelper.prepare(sql + "))', 4326)) ORDER BY ST_AREA(geometry) DESC");

    while(zonesQuery.step())
      zoneList.add(getZone(zonesQuery.getInt(0)));
    zonesQuery.close();

    return zoneList;
  }

  public PointRecord[] getZoneBounds(int zone_id) {
    PointRecord[] pointBounds = {new PointRecord(-1, -1, -1), new PointRecord(-1, -1, -1)};
    BetterStatement zoneQuery = dbHelper.prepare("SELECT " +
                                                   "MbrMinX(geometry), " +
                                                   "MbrMinY(geometry), " +
                                                   "MbrMaxX(geometry), " +
                                                   "MbrMaxY(geometry) " +
                                                 "FROM zone WHERE id = '" + zone_id + "'");
    if(zoneQuery.step()) {
      pointBounds[0] = new PointRecord(-1, zoneQuery.getDouble(0), zoneQuery.getDouble(1));
      pointBounds[1] = new PointRecord(-1, zoneQuery.getDouble(2), zoneQuery.getDouble(3));
    }

    return pointBounds;
  }

  public double distanceBetween(PointRecord point, int zone_id) {
    double distance = -1;
    BetterStatement zoneQuery = dbHelper.prepare("SELECT " +
                                                   "Distance(" +
                                                   "GeomFromText('POINT(" + point.getX() + " " + point.getY() + ")', 4326), " +
                                                     "geometry, " +
                                                     "0" +
                                                   ") AS dist " +
                                                 "FROM zone " +
                                                   "WHERE id = '" + zone_id + "' AND dist > 0.0 " +
                                                 "ORDER BY dist ASC LIMIT 1");
    if(zoneQuery.step())
      distance = zoneQuery.getDouble(0);
    zoneQuery.close();

    return distance;
  }

  public double distanceToClosestZone(PointRecord point) {
    double distance = -1;
    BetterStatement zoneQuery = dbHelper.prepare("SELECT " +
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
                                                 "FROM zone " +
                                                 "WHERE dist > 0.0 " +
                                                 "ORDER BY dist ASC LIMIT 1");
    if(zoneQuery.step())
      distance = zoneQuery.getDouble(0);
    zoneQuery.close();

    return distance;
  }
}

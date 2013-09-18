package org.anhonesteffort.polygons.database;

import android.content.ContentValues;
import android.database.SQLException;
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

  private boolean isInitialized() {
    try {

      SpatialCursor zonesQuery = dbHelper.prepare("SELECT 1 FROM zone LIMIT 1");
      if(zonesQuery.getCount() > 0) {
        zonesQuery.close();
        return true;
      }
      zonesQuery.close();

    } catch (SQLException e) {
      dbHelper.displayException(e);
    }

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
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT id FROM zone WHERE label = '" +
                                                    DatabaseHelper.escapeString(label) + "'");

    if(zoneRecords.getCount() > 0) {
      zoneRecords.close();
      return false;
    }

    zoneRecords.close();
    return true;
  }

  public PointRecord addPoint(PointRecord point, int zone_id) {
    int point_id = -1;
    ContentValues values = new ContentValues();

    values.put("zone_id", zone_id);
    values.put("latitude", point.getY());
    values.put("longitude", point.getX());
    dbHelper.insert("point", values);

    SpatialCursor pointRecords = dbHelper.prepare("SELECT id FROM point ORDER BY id DESC LIMIT 1");
    if(pointRecords.moveToNext())
      point_id = pointRecords.getInt(0);

    pointRecords.close();
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
    SpatialCursor pointRecords = dbHelper.prepare("SELECT id, longitude, latitude FROM point WHERE id = '" +
                                                                                               point_id + "'");

    if(pointRecords.moveToNext())
      outPoint = new PointRecord(pointRecords.getInt(0), pointRecords.getDouble(1), pointRecords.getDouble(2));

    return outPoint;
  }

  public ZoneRecord addZone(String label) {
    Log.d(TAG, "addZone(), label: " + label);

    ZoneRecord outZone = new ZoneRecord(-1, label);

    ContentValues values = new ContentValues();
    values.put("label", DatabaseHelper.escapeString(label));
    values.put("geometry", "ST_GeomFromText('POINT(0.0 0.0)', 4326)");
    dbHelper.insert("zone", values);

    SpatialCursor zoneRecords = dbHelper.prepare("SELECT id FROM zone ORDER BY id DESC LIMIT 1");
    if(zoneRecords.moveToNext())
      outZone = new ZoneRecord(zoneRecords.getInt(0), label);

    zoneRecords.close();
    return outZone;
  }

  public void deleteZone(int zone_id) {
    Log.d(TAG, "deleteZone(), id: " + zone_id);
    dbHelper.exec("DELETE FROM zone WHERE id = '" + zone_id + "'");
  }

  public ZoneRecord updateZone(ZoneRecord zone) {
    Log.d(TAG, "updateZone(), id: " + zone.getId() + ", label: " + zone.getLabel());
    String sql;

    // Enforce point limits.
    if(zone.getPoints().size() < ZoneRecord.MIN_POINTS || zone.getPoints().size() > ZoneRecord.MAX_POINTS) {
      deleteZone(zone.getId());
      geometryChange();
      return new ZoneRecord(-1, "");
    }

    // Update the points table.
    dbHelper.exec("DELETE FROM point WHERE zone_id = '" + zone.getId() + "'");
    for(PointRecord point : zone.getPoints())
      addPoint(point, zone.getId());

    // Update the zone's geometry and label.
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT id FROM zone WHERE id = '" + zone.getId() + "'");
    if(zoneRecords.moveToNext()) {
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

    zoneRecords.close();
    geometryChange();
    return zone;
  }

  public ZoneRecord getZone(int zone_id) {
    ZoneRecord zone = new ZoneRecord(-1, "");
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT id, label FROM zone WHERE id = '" + zone_id + "'");

    if(zoneRecords.moveToNext()) {
      zone = new ZoneRecord(zoneRecords.getInt(0), DatabaseHelper.unescapeString(zoneRecords.getString(1)));
      SpatialCursor pointRecords = dbHelper.prepare("SELECT id, latitude, longitude FROM point WHERE " +
                                                      "zone_id = '" + zoneRecords.getInt(0) + "'");

      while(pointRecords.moveToNext())
        zone.getPoints().add(getPoint(pointRecords.getInt(0)));

      pointRecords.close();
    }

    zoneRecords.close();
    return zone;
  }

  public List<ZoneRecord> getZones() {
    List<ZoneRecord> zoneList = new LinkedList<ZoneRecord>();
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT id FROM zone");

    while(zoneRecords.moveToNext())
      zoneList.add(getZone(zoneRecords.getInt(0)));

    zoneRecords.close();
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
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT zone_id FROM occupy");

    while(zoneRecords.moveToNext())
      zoneList.add(getZone(zoneRecords.getInt(0)));

    zoneRecords.close();
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
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT zone_id FROM selected WHERE zone_id = '" + zone_id + "'");

    if(zoneRecords.getCount() > 0) {
      zoneRecords.close();
      return true;
    }

    zoneRecords.close();
    return false;
  }

  public List<ZoneRecord> getZonesSelected() {
    List<ZoneRecord> zoneList = new ArrayList<ZoneRecord>();
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT zone_id FROM selected");

    while(zoneRecords.moveToNext())
      zoneList.add(getZone(zoneRecords.getInt(0)));

    zoneRecords.close();
    return zoneList;
  }

  public List<ZoneRecord> getZonesContainingPoint(PointRecord point) {
    List<ZoneRecord> zoneList = new ArrayList<ZoneRecord>();
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT id FROM zone WHERE " +
                                                   "ST_WITHIN(GeomFromText('" +
                                                     "POINT(" + point.getX() + " " + point.getY() + ")'" +
                                                     ", 4326), geometry) " +
                                                 "ORDER BY ST_AREA(geometry) ASC");
    while(zoneRecords.moveToNext())
      zoneList.add(getZone(zoneRecords.getInt(0)));

    zoneRecords.close();
    return zoneList;
  }

  public List<ZoneRecord> getZonesIntersecting(ZoneRecord zone) {
    String sql;
    List<ZoneRecord> zoneList = new ArrayList<ZoneRecord>();

    sql = "SELECT id FROM zone WHERE ST_Intersects(geometry, GeomFromText('POLYGON((";
    for(int i = 0; i < zone.getPoints().size(); i++) {
      if(i != 0)
        sql += ",";
      sql += zone.getPoints().get(i).getX() + " " + zone.getPoints().get(i).getY();
    }
    sql += "," + zone.getPoints().get(0).getX() + " " + zone.getPoints().get(0).getY();

    SpatialCursor zoneRecords = dbHelper.prepare(sql + "))', 4326)) ORDER BY ST_AREA(geometry) DESC");
    while(zoneRecords.moveToNext())
      zoneList.add(getZone(zoneRecords.getInt(0)));

    zoneRecords.close();
    return zoneList;
  }

  public PointRecord[] getZoneBounds(int zone_id) {
    PointRecord[] pointBounds = {new PointRecord(-1, -1, -1), new PointRecord(-1, -1, -1)};
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT " +
                                                   "MbrMinX(geometry), " +
                                                   "MbrMinY(geometry), " +
                                                   "MbrMaxX(geometry), " +
                                                   "MbrMaxY(geometry) " +
                                                 "FROM zone WHERE id = '" + zone_id + "'");
    if(zoneRecords.moveToNext()) {
      pointBounds[0] = new PointRecord(-1, zoneRecords.getDouble(0), zoneRecords.getDouble(1));
      pointBounds[1] = new PointRecord(-1, zoneRecords.getDouble(2), zoneRecords.getDouble(3));
    }

    zoneRecords.close();
    return pointBounds;
  }

  public double distanceBetween(PointRecord point, int zone_id) {
    double distance = -1;
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT " +
                                                   "Distance(" +
                                                   "GeomFromText('POINT(" + point.getX() + " " + point.getY() + ")', 4326), " +
                                                     "geometry, " +
                                                     "0" +
                                                   ") AS dist " +
                                                 "FROM zone " +
                                                   "WHERE id = '" + zone_id + "' AND dist > 0.0 " +
                                                 "ORDER BY dist ASC LIMIT 1");
    if(zoneRecords.moveToNext())
      distance = zoneRecords.getDouble(0);

    zoneRecords.close();
    return distance;
  }

  public double distanceToClosestZone(PointRecord point) {
    double distance = -1;
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT " +
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
    if(zoneRecords.moveToNext())
      distance = zoneRecords.getDouble(0);

    zoneRecords.close();
    return distance;
  }
}

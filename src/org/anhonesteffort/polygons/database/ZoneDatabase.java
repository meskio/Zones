package org.anhonesteffort.polygons.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import org.anhonesteffort.polygons.database.model.PointRecord;
import org.anhonesteffort.polygons.database.model.ZoneRecord;

import java.util.ArrayList;
import java.util.List;

public class ZoneDatabase {

  private static final String TAG = "ZoneDatabase";

  private DatabaseHelper dbHelper;
  private ArrayList<GeometryChangeListener> listeners = new ArrayList<GeometryChangeListener>();

  protected ZoneDatabase(DatabaseHelper dbHelper) {
    this.dbHelper = dbHelper;
    if(isInitialized() == false) {
      initialize();
      Log.d(TAG, "Initialized the zone tables.");
    }
  }

  private void initialize() {
    dbHelper.exec("SELECT InitSpatialMetaData()");
    dbHelper.exec("DROP TABLE IF EXISTS zone");
    dbHelper.exec("DROP TABLE IF EXISTS point");
    dbHelper.exec("DROP TABLE IF EXISTS occupy");
    dbHelper.exec("DROP TABLE IF EXISTS selected");

    dbHelper.exec("CREATE TABLE IF NOT EXISTS zone (" +
                    "_id INTEGER NOT NULL PRIMARY KEY, " +
                    "label VARCHAR(100) NOT NULL" +
                  ")");
    dbHelper.exec("SELECT AddGeometryColumn('zone', 'geometry', 4326, 'GEOMETRY')");

    dbHelper.exec("CREATE TABLE IF NOT EXISTS point (" +
                    "_id INTEGER NOT NULL PRIMARY KEY, " +
                    "zone_id INTEGER NOT NULL, " +
                    "latitude REAL NOT NULL, " +
                    "longitude REAL NOT NULL, " +
                    "FOREIGN KEY (zone_id) REFERENCES zone(_id) ON DELETE CASCADE" +
                  ")");

    dbHelper.exec("CREATE TABLE IF NOT EXISTS occupy (" +
                    "zone_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (zone_id), " +
                    "FOREIGN KEY (zone_id) REFERENCES zone(_id) ON DELETE CASCADE" +
                  ")");

    dbHelper.exec("CREATE TABLE IF NOT EXISTS selected (" +
                    "zone_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (zone_id), " +
                    "FOREIGN KEY (zone_id) REFERENCES zone(_id) ON DELETE CASCADE" +
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

  public boolean isLabelAvailable(String label) {
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT _id FROM zone WHERE label = '" +
                                                    DatabaseHelper.escapeString(label) + "'");

    if(zoneRecords.getCount() > 0) {
      zoneRecords.close();
      return false;
    }

    zoneRecords.close();
    return true;
  }

  private void geometryChange() {
    for(GeometryChangeListener listener : listeners) {
      if(listener != null)
        listener.onGeometryChange();
    }
  }

  private PointRecord addPoint(PointRecord point, int zone_id) {
    int point_id = -1;
    ContentValues values = new ContentValues();

    values.put("zone_id", zone_id);
    values.put("latitude", point.getY());
    values.put("longitude", point.getX());
    dbHelper.insert("point", values);

    SpatialCursor pointRecords = dbHelper.prepare("SELECT _id FROM point ORDER BY _id DESC LIMIT 1");
    if(pointRecords.moveToNext())
      point_id = pointRecords.getInt(0);

    pointRecords.close();
    return new PointRecord(point_id, zone_id, point.getX(), point.getY());
  }

  public PointRecord getPoint(int point_id) {
    PointRecord outPoint = null;
    SpatialCursor pointRecords = dbHelper.prepare("SELECT _id, zone_id, longitude, latitude FROM point WHERE _id = '" +
                                                                                                         point_id + "'");

    if(pointRecords.moveToNext())
      outPoint = new PointRecord(pointRecords.getInt(0),
                                 pointRecords.getInt(1),
                                 pointRecords.getDouble(2),
                                 pointRecords.getDouble(3));

    return outPoint;
  }

  private boolean zoneExists(int zone_id) {
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT _id FROM zone WHERE _id = '" + zone_id + "'");
    if(zoneRecords.moveToNext()) {
      zoneRecords.close();
      return true;
    }

    zoneRecords.close();
    return false;
  }

  public ZoneRecord addZone(String label) {
    Log.d(TAG, "addZone()");
    ZoneRecord outZone = null;

    ContentValues values = new ContentValues();
    values.put("label", DatabaseHelper.escapeString(label));
    values.put("geometry", "ST_GeomFromText('POINT(0.0 0.0)', 4326)");
    dbHelper.insert("zone", values);

    SpatialCursor zoneRecords = dbHelper.prepare("SELECT _id FROM zone ORDER BY _id DESC LIMIT 1");
    if(zoneRecords.moveToNext())
      outZone = new ZoneRecord(zoneRecords.getInt(0), label);

    zoneRecords.close();
    dbHelper.getActionDatabase().initZoneActions(outZone.getId());
    return outZone;
  }

  public void deleteZone(int zone_id) {
    Log.d(TAG, "deleteZone(), id: " + zone_id);
    dbHelper.exec("DELETE FROM zone WHERE _id = '" + zone_id + "'");
  }

  public ZoneRecord updateZone(ZoneRecord zone) {
    Log.d(TAG, "updateZone(), id: " + zone.getId());

    if(zoneExists(zone.getId())) {
      String sql = "UPDATE zone SET geometry = GeomFromText('POLYGON((";

      for(int i = 0; i < zone.getPoints().size(); i++) {
        if(i != 0)
          sql += ",";
        sql += zone.getPoints().get(i).getX() + " " + zone.getPoints().get(i).getY();
      }
      sql += "," + zone.getPoints().get(0).getX() + " " + zone.getPoints().get(0).getY();

      dbHelper.exec(sql + "))', 4326) WHERE _id = '" + zone.getId() + "'");
      dbHelper.exec("UPDATE zone SET label = '" + DatabaseHelper.escapeString(zone.getLabel()) + "' " +
                    "WHERE _id = '" + zone.getId() + "'");

      dbHelper.exec("DELETE FROM point WHERE zone_id = '" + zone.getId() + "'");
      for(PointRecord point : zone.getPoints())
        addPoint(point, zone.getId());
    }
    else
      zone = null;

    geometryChange();
    return zone;
  }

  public ZoneRecord getZone(int zone_id) {
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT _id, label FROM zone WHERE _id = '" + zone_id + "'");
    Reader zoneReader = new Reader(dbHelper, zoneRecords);

    ZoneRecord zone = zoneReader.getNext();

    zoneReader.close();
    return zone;
  }

  public Cursor getZones() {
    return dbHelper.prepare("SELECT _id, label FROM zone");
  }

  public List<ZoneRecord> getZonesContainingPoint(PointRecord point) {
    List<ZoneRecord> zoneList = new ArrayList<ZoneRecord>();
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT _id FROM zone WHERE " +
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
    List<ZoneRecord> zoneList = new ArrayList<ZoneRecord>();
    String sql = "SELECT _id FROM zone WHERE ST_Intersects(geometry, GeomFromText('POLYGON((";

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
    PointRecord[] pointBounds = {null, null};
    SpatialCursor zoneRecords = dbHelper.prepare("SELECT " +
                                                   "MbrMinX(geometry), " +
                                                   "MbrMinY(geometry), " +
                                                   "MbrMaxX(geometry), " +
                                                   "MbrMaxY(geometry) " +
                                                 "FROM zone WHERE _id = '" + zone_id + "'");
    if(zoneRecords.moveToNext()) {
      pointBounds[0] = new PointRecord(-1, -1, zoneRecords.getDouble(0), zoneRecords.getDouble(1));
      pointBounds[1] = new PointRecord(-1, -1, zoneRecords.getDouble(2), zoneRecords.getDouble(3));
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
                                                   "WHERE _id = '" + zone_id + "' AND dist > 0.0 " +
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

  public Cursor getZonesSelected() {
    return dbHelper.prepare("SELECT zone_id, label FROM selected JOIN zone ON zone_id = _id");
  }

  public static class Reader {

    private DatabaseHelper dbHelper;
    private Cursor cursor;

    public Reader(DatabaseHelper dbHelper, Cursor cursor) {
      this.dbHelper = dbHelper;
      this.cursor = cursor;
    }

    public ZoneRecord getNext() {
      if (cursor == null || !cursor.moveToNext())
        return null;

      return getCurrent();
    }

    public ZoneRecord getCurrent() {
      return getZoneRecord(cursor);
    }

    private ZoneRecord getZoneRecord(Cursor cursor) {
      ZoneRecord currentZone = new ZoneRecord(cursor.getInt(0), cursor.getString(1));
      SpatialCursor zonePoints = dbHelper.prepare("SELECT _id FROM point " +
                                                    "WHERE zone_id = '" + currentZone.getId() + "'");

      while(zonePoints.moveToNext())
        currentZone.getPoints().add(dbHelper.getZoneDatabase().getPoint(zonePoints.getInt(0)));

      zonePoints.close();
      return currentZone;
    }

    public void close() {
      cursor.close();
    }
  }

}

package org.anhonesteffort.polygons.database;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import jsqlite.Database;
import jsqlite.Exception;

import java.io.File;
import java.util.Map;

public class DatabaseHelper {

  private static final String TAG = "DatabaseHelper";

  private static DatabaseHelper instance;
  private Context context;
  private Database database;

  private ActionDatabase actionDatabase;
  private ZoneDatabase zoneDatabase;

  public synchronized static DatabaseHelper getInstance(Context context) {
    if(instance == null)
      instance = new DatabaseHelper(new File(context.getFilesDir(), "ZonesDatabase.sqlite"), context);
    return instance;
  }

  private DatabaseHelper(File spatialDbFile, Context context) {
    Log.d(TAG, "private DatabaseHelper()");

    this.context = context;
    database = new jsqlite.Database();

    try {
      database.open(spatialDbFile.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE | jsqlite.Constants.SQLITE_OPEN_CREATE);
      zoneDatabase = new ZoneDatabase(this);
      actionDatabase = new ActionDatabase(this);
    } catch (Exception e) {
      displayException(e);
    }
  }

  public ZoneDatabase getZoneDatabase() {
    return zoneDatabase;
  }

  public ActionDatabase getActionDatabase() {
    return actionDatabase;
  }

  protected static String escapeString(String input) {
    return input.replace("'", "\'");
  }
  
  protected static String unescapeString(String input) {
    return input.replace("\'", "'");
  }

  protected String getStringResource(int resource_id) {
    return context.getString(resource_id);
  }

  protected Database getDatabase() {
    return database;
  }

  protected SpatialCursor prepare(String sql) {
    return new SpatialCursor(this, sql);
  }

  protected void exec(String sql) {
    try {
      database.exec("PRAGMA foreign_keys=ON; " + sql, null);
    }
    catch (Exception e) {
      displayException(e);
    }
  }

  private ContentValues prepareValues(ContentValues values) {
    ContentValues out = new ContentValues();

    // WHY CAN I NOT CALL values.keySet()?!?!
    //Set<String> test = values.keySet();

    for(Map.Entry<String, Object> entry : values.valueSet()) {
      if(entry.getValue().toString().startsWith("ST_"))
        out.put(entry.getKey(), entry.getValue().toString());
      else
        out.put(entry.getKey(), "'" + entry.getValue() + "'");
    }

    return out;
  }

  protected void insert(String table, ContentValues contentValues) {
    if(contentValues.size() == 0 || table == null)
      return;

    int i = 1;
    ContentValues insertValues = prepareValues(contentValues);

    String sql = "INSERT INTO " + table + " (";
    for(Map.Entry<String, Object> entry : insertValues.valueSet()) {
      if(i == insertValues.size())
        sql += entry.getKey() + ")";
      else
        sql += entry.getKey() + ", ";

      i++;
    }

    i = 1;
    sql += " VALUES(";
    for(Map.Entry<String, Object> entry : insertValues.valueSet()) {
      if(i == insertValues.size())
        sql += entry.getValue() + ")";
      else
        sql += entry.getValue() + ", ";

      i++;
    }

    // Log.w(TAG, sql);
    exec(sql);
  }

  protected void displayException(java.lang.Exception e) {
    Log.e(TAG, "displayException(): " + e.toString());
  }

  public void close() {
    try {
      database.close();
    } catch (Exception e) {
      displayException(e);
    }
  }

}

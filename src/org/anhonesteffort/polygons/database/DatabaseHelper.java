package org.anhonesteffort.polygons.database;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import jsqlite.Database;
import jsqlite.Exception;

import java.io.File;
import java.util.Map;

public class DatabaseHelper {
  private static final String TAG = "org.anhonesteffort.zoneDb.database.DatabaseHelper";
  private Context applicationContext;
  private Database db;
  public ActionDatabase actionDb;
  public ZoneDatabase zoneDb;
  private static DatabaseHelper instance;

  public synchronized static DatabaseHelper getInstance(Context context) {
    if(instance == null)
      instance = new DatabaseHelper(new File(context.getFilesDir(), "zoneDb.sqlite"), context);
    return instance;
  }

  private DatabaseHelper(File spatialDbFile, Context context) {
    Log.d(TAG, "private DatabaseHelper()");

    applicationContext = context;
    db = new jsqlite.Database();

    try {
      db.open(spatialDbFile.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE | jsqlite.Constants.SQLITE_OPEN_CREATE);
      zoneDb = new ZoneDatabase(this);
      actionDb = new ActionDatabase(this);
    } catch (Exception e) {
      throwError(e.toString());
    }
  }
  
  protected static String escapeString(String input) {
    return input.replace("'", "\'");
  }
  
  protected static String unescapeString(String input) {
    return input.replace("\'", "'");
  }

  public void throwError(String error) {
    Log.e(TAG, "throwError(), error: " + error);
  }

  public String getStringResource(int resource_id) {
    return applicationContext.getString(resource_id);
  }

  protected Database getDB() {
    return db;
  }

  protected void exec(String sql) {
    try {
      db.exec("PRAGMA foreign_keys=ON; " + sql, null);
    }
    catch (Exception e) {
      throwError(e.toString());
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

    Log.w(TAG, sql);
    exec(sql);
  }

  protected BetterStatement prepare(String sql) {
    return new BetterStatement(this, sql);
  }

  public void close() {
    try {
      db.close();
    } catch (Exception e) {
      throwError(e.toString());
    }
  }
}

package org.anhonesteffort.polygons.storage;

import java.io.File;

import jsqlite.Database;
import jsqlite.Exception;
import android.content.Context;
import android.util.Log;

public class DatabaseHelper {
  private static final String TAG = "org.anhonesteffort.polygons.storage.DatabaseHelper";
  private Context applicationContext;
  private Database db;
  public ActionStorage actions;
  public PolygonStorage polygons;
  private static DatabaseHelper instance;

  public synchronized static DatabaseHelper getInstance(Context context) {
    if(instance == null)
      instance = new DatabaseHelper(new File(context.getFilesDir(), "polygons.sqlite"), context);
    return instance;
  }

  private DatabaseHelper(File spatialDbFile, Context context) {
    Log.d(TAG, "private DatabaseHelper()");
    applicationContext = context;
    db = new jsqlite.Database();

    try {
      db.open(spatialDbFile.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE | jsqlite.Constants.SQLITE_OPEN_CREATE);
      polygons = new PolygonStorage(this);
      actions = new ActionStorage(this);
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

  public void exec(String sql) {
    try {
      db.exec("PRAGMA foreign_keys=ON; " + sql, null);
    }
    catch (Exception e) {
      throwError(e.toString());
    }
  }

  public BetterStatement prepare(String sql) {
    return new BetterStatement(this, sql);
  }

  // Probably doesn't need to exist...
  public void close() {
    try {
      db.close();
    } catch (Exception e) {
      throwError(e.toString());
    }
  }
}

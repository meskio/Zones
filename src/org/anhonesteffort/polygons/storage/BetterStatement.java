package org.anhonesteffort.polygons.storage;

import jsqlite.Exception;
import jsqlite.Stmt;

public class BetterStatement {
  private DatabaseHelper dbHelper;
  private Stmt statement;

  public BetterStatement(DatabaseHelper dbHelper, String sql) {
    this.dbHelper = dbHelper;
    try {
      statement = dbHelper.getDB().prepare(sql);
    } catch (Exception e) {
      dbHelper.throwError(e.toString());
    }
  }

  public boolean step() {
    try {
      if(statement != null && statement.step())
        return true;
    } catch (Exception e) {
      dbHelper.throwError(e.toString());
    }
    return false;
  }
  
  public void reset() {
    try {
      if(statement != null)
        statement.reset();
    } catch (Exception e) {
      dbHelper.throwError(e.toString());
    }
  }

  public void close() {
    try {
      if(statement != null)
        statement.close();
    } catch (Exception e) {
      dbHelper.throwError(e.toString());
    }
  }

  public String getString(int column) {
    try {
      if(statement != null)
        return statement.column_string(column);
    } catch (Exception e) {
      dbHelper.throwError(e.toString());
    }
    return null;
  }

  public int getInt(int column) {
    try {
      if(statement != null)
        return statement.column_int(column);
    } catch (Exception e) {
      dbHelper.throwError(e.toString());
    }
    return -1;
  }

  public double getDouble(int column) {
    try {
      if(statement != null)
        return statement.column_double(column);
    } catch (Exception e) {
      dbHelper.throwError(e.toString());
    }
    return -1;
  }
}

package org.anhonesteffort.polygons.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.database.model.ActionRecord;

import java.util.LinkedList;
import java.util.List;

public class ActionDatabase {

  private static final String TAG = "ActionDatabase";

  private static final String ACTION_SELECTION = "SELECT _id, name, description, " +
                                                        "zone_id, enter, exit " +
                                                  "FROM action JOIN zone_action " +
                                                  "ON _id = action_id ";
  private static final String ACTION_GROUP_BY  = " GROUP BY _id";

  private DatabaseHelper dbHelper;
  private List<LocationSubscriberChangeListener> locationSubscriberListeners;

  protected ActionDatabase(DatabaseHelper dbHelper) {
    this.dbHelper = dbHelper;
    locationSubscriberListeners = new LinkedList<LocationSubscriberChangeListener>();
    
    if(isInitialized() == false) {
      initialize();
      Log.d(TAG, "Initialized the action tables.");
    }
  }

  private void initialize() {
    // Drop all tables.
    dbHelper.exec("DROP TABLE IF EXISTS action");
    dbHelper.exec("DROP TABLE IF EXISTS zone_action");
    dbHelper.exec("DROP TABLE IF EXISTS location_update_receiver");

    // Create the action table.
    dbHelper.exec("CREATE TABLE IF NOT EXISTS action (" +
                    "_id INTEGER NOT NULL PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "description VARCHAR(100) NOT NULL" +
                  ")");

    // Create the action broadcast table.
    dbHelper.exec("CREATE TABLE IF NOT EXISTS zone_action (" +
                    "action_id INTEGER NOT NULL, " +
                    "zone_id INTEGER NOT NULL, " +
                    "enter INTEGER NOT NULL, " +
                    "exit INTEGER NOT NULL, " +
                    "PRIMARY KEY (action_id, zone_id), " +
                    "FOREIGN KEY (action_id) REFERENCES action(_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (zone_id) REFERENCES zone(_id) ON DELETE CASCADE" +
                  ")");

    // Create the location update table.
    dbHelper.exec("CREATE TABLE IF NOT EXISTS location_update_receiver (" +
                    "phone_number VARCHAR(100) NOT NULL, " +
                    "PRIMARY KEY (phone_number)" +
                  ")");

    // Populate the action table.
    dbHelper.exec("INSERT INTO action (_id, name, description) VALUES(" +
                    "'" + R.integer.action_audio_alarm + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_audio_alarm) + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_audio_alarm_description) + "'" +
                  ")");
    dbHelper.exec("INSERT INTO action (_id, name, description) VALUES(" +
                    "'" + R.integer.action_sms_alert + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_sms_alert) + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_sms_alert_description) + "'" +
                  ")");
    dbHelper.exec("INSERT INTO action (_id, name, description) VALUES(" +
                    "'" + R.integer.action_super_lock + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_super_lock) + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_super_lock_description) + "'" +
                  ")");  
    dbHelper.exec("INSERT INTO action (_id, name, description) VALUES(" +
                    "'" + R.integer.action_super_unlock + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_super_unlock) + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_super_unlock_description) + "'" +
                  ")");
    dbHelper.exec("INSERT INTO action (_id, name, description) VALUES(" +
                    "'" + R.integer.action_clear_call_history + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_clear_call_history) + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_clear_call_history_description) + "'" +
                  ")");
    dbHelper.exec("INSERT INTO action (_id, name, description) VALUES(" +
                    "'" + R.integer.action_clear_contacts + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_clear_contacts) + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_clear_contacts_description) + "'" +
                  ")");
    dbHelper.exec("INSERT INTO action (_id, name, description) VALUES(" +
                    "'" + R.integer.action_factory_reset + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_factory_reset) + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_factory_reset_description) + "'" +
                  ")");
  }

  private boolean isInitialized() {
    try {

      SpatialCursor actionRecords = dbHelper.prepare("SELECT 1 FROM action LIMIT 1");
      if(actionRecords.getCount() > 0) {
        actionRecords.close();
        return true;
      }
      actionRecords.close();

    } catch (SQLException e) {
      dbHelper.displayException(e);
    }

    return false;
  }
  
  public void addLocationSubscriberChangeListener(LocationSubscriberChangeListener listener) {
    locationSubscriberListeners.add(listener);
  }

  public void removeLocationSubscriberChangeListener(LocationSubscriberChangeListener listener) {
    locationSubscriberListeners.remove(listener);
  }
  
  private void subscriberChange() {
    for(LocationSubscriberChangeListener listener : locationSubscriberListeners) {
      if(listener != null)
        listener.onSubscriberChange();
    }
  }
  
  public List<String> getLocationSubscribers() {
    List<String> subscriberNumbers = new LinkedList<String>();
    SpatialCursor subscriberRecords = dbHelper.prepare("SELECT phone_number FROM location_update_receiver");

    while(subscriberRecords.moveToNext())
      subscriberNumbers.add(subscriberRecords.getString(0));

    subscriberRecords.close();
    return subscriberNumbers;
  }
  
  public void addLocationSubscriber(String phone_number) {
    if(phone_number != null) {
      dbHelper.exec("INSERT OR IGNORE INTO location_update_receiver (phone_number) VALUES('" + phone_number + "')");
      subscriberChange();
    }
  }
  
  public void removeLocationSubscriber(String phone_number) {
    if(phone_number != null) {
      dbHelper.exec("DELETE FROM location_update_receiver where phone_number = '" + phone_number + "'");
      subscriberChange();
    }
  }

  protected void initZoneActions(int zone_id) {
    ContentValues values;

    values = new ContentValues();
    values.put("action_id", R.integer.action_audio_alarm);
    values.put("zone_id", zone_id);
    values.put("enter", 0);
    values.put("exit", 0);
    dbHelper.insert("zone_action", values);

    values = new ContentValues();
    values.put("action_id", R.integer.action_sms_alert);
    values.put("zone_id", zone_id);
    values.put("enter", 0);
    values.put("exit", 0);
    dbHelper.insert("zone_action", values);

    values = new ContentValues();
    values.put("action_id", R.integer.action_super_lock);
    values.put("zone_id", zone_id);
    values.put("enter", 0);
    values.put("exit", 0);
    dbHelper.insert("zone_action", values);

    values = new ContentValues();
    values.put("action_id", R.integer.action_super_unlock);
    values.put("zone_id", zone_id);
    values.put("enter", 0);
    values.put("exit", 0);
    dbHelper.insert("zone_action", values);

    values = new ContentValues();
    values.put("action_id", R.integer.action_clear_call_history);
    values.put("zone_id", zone_id);
    values.put("enter", 0);
    values.put("exit", 0);
    dbHelper.insert("zone_action", values);

    values = new ContentValues();
    values.put("action_id", R.integer.action_clear_contacts);
    values.put("zone_id", zone_id);
    values.put("enter", 0);
    values.put("exit", 0);
    dbHelper.insert("zone_action", values);

    values = new ContentValues();
    values.put("action_id", R.integer.action_factory_reset);
    values.put("zone_id", zone_id);
    values.put("enter", 0);
    values.put("exit", 0);
    dbHelper.insert("zone_action", values);
  }

  public Cursor getActions(int zone_id) {
    return dbHelper.prepare(ACTION_SELECTION + "WHERE zone_action.zone_id = '" + zone_id + "'" + ACTION_GROUP_BY);
  }

  public void updateZoneAction(ActionRecord action) {
    Log.d(TAG, "updateZoneAction(), action_id: " + action.getID() + ", zone_id: " + action.getZoneId());

    dbHelper.exec("DELETE FROM zone_action " +
                  "WHERE action_id = '" + action.getID() + "' " +
                  "AND zone_id = '" + action.getZoneId() + "'");

    ContentValues values = new ContentValues();
    values.put("action_id", action.getID());
    values.put("zone_id", action.getZoneId());
    values.put("enter", (action.runOnEnter()? 1 : 0));
    values.put("exit", (action.runOnExit()? 1 : 0));
    dbHelper.insert("zone_action", values);
  }

  public static class Reader {

    private Cursor cursor;

    public Reader(Cursor cursor) {
      this.cursor = cursor;
    }

    public ActionRecord getNext() {
      if (cursor == null || !cursor.moveToNext())
        return null;

      return getCurrent();
    }

    public ActionRecord getCurrent() {
      return getActionRecord(cursor);
    }

    private ActionRecord getActionRecord(Cursor cursor) {
      return new ActionRecord(cursor.getInt(0),
                              cursor.getString(1),
                              cursor.getString(2),
                              cursor.getInt(3),
                              (cursor.getInt(4) != 0),
                              (cursor.getInt(5) != 0));
    }

    public void close() {
      cursor.close();
    }
  }

}
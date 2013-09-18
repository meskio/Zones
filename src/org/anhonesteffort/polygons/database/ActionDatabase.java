package org.anhonesteffort.polygons.database;

import android.content.ContentValues;
import android.database.SQLException;
import android.util.Log;
import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.database.model.ActionRecord;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ActionDatabase {

  private static final String TAG = "ActionDatabase";

  private DatabaseHelper dbHelper;
  private ArrayList<LocationSubscriberChangeListener> locationSubscriberListeners = new ArrayList<LocationSubscriberChangeListener>();

  protected ActionDatabase(DatabaseHelper dbHelper) {
    this.dbHelper = dbHelper;
    
    if(isInitialized() == false) {
      initialize();
      Log.d(TAG, "Initialized the action tables.");
    }
  }

  private void initialize() {
    // Drop all tables.
    dbHelper.exec("DROP TABLE IF EXISTS location_update_receiver");
    dbHelper.exec("DROP TABLE IF EXISTS action");
    dbHelper.exec("DROP TABLE IF EXISTS action_broadcast");
    
    // Create the location update table.
    dbHelper.exec("CREATE TABLE IF NOT EXISTS location_update_receiver (" +
                    "phone_number VARCHAR(100) NOT NULL, " +
                    "PRIMARY KEY (phone_number)" +
                  ")");

    // Create the action table.
    dbHelper.exec("CREATE TABLE IF NOT EXISTS action (" +
                    "id INTEGER NOT NULL PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "description VARCHAR(100) NOT NULL" +
                  ")");

    // Create the action broadcast table.
    dbHelper.exec("CREATE TABLE IF NOT EXISTS action_broadcast (" +
                    "action_id INTEGER NOT NULL, " +
                    "zone_id INTEGER NOT NULL, " +
                    "enter INTEGER NOT NULL, " +
                    "exit INTEGER NOT NULL, " +
                    "PRIMARY KEY (action_id, zone_id), " +
                    "FOREIGN KEY (action_id) REFERENCES action(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (zone_id) REFERENCES zone(id) ON DELETE CASCADE" +
                  ")");

    // Populate the action table.
    dbHelper.exec("INSERT INTO action (id, name, description) VALUES(" +
                    "'" + R.integer.action_audio_alarm + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_audio_alarm) + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_audio_alarm_description) + "'" +
                  ")");
    dbHelper.exec("INSERT INTO action (id, name, description) VALUES(" +
                    "'" + R.integer.action_sms_alert + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_sms_alert) + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_sms_alert_description) + "'" +
                  ")");
    dbHelper.exec("INSERT INTO action (id, name, description) VALUES(" +
                    "'" + R.integer.action_super_lock + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_super_lock) + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_super_lock_description) + "'" +
                  ")");  
    dbHelper.exec("INSERT INTO action (id, name, description) VALUES(" +
                    "'" + R.integer.action_super_unlock + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_super_unlock) + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_super_unlock_description) + "'" +
                  ")");
    dbHelper.exec("INSERT INTO action (id, name, description) VALUES(" +
                    "'" + R.integer.action_clear_call_history + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_clear_call_history) + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_clear_call_history_description) + "'" +
                  ")");
    dbHelper.exec("INSERT INTO action (id, name, description) VALUES(" +
                    "'" + R.integer.action_clear_contacts + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_clear_contacts) + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_clear_contacts_description) + "'" +
                  ")");
    dbHelper.exec("INSERT INTO action (id, name, description) VALUES(" +
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

  public List<ActionRecord> getActions() {
    List<ActionRecord> actionNames = new LinkedList<ActionRecord>();
    SpatialCursor actionRecords = dbHelper.prepare("SELECT id, name, description FROM action");

    while(actionRecords.moveToNext())
      actionNames.add(new ActionRecord(actionRecords.getInt(0),
                                       actionRecords.getString(1),
                                       actionRecords.getString(2), -1));

    actionRecords.close();
    return actionNames;
  }

  public List<ActionRecord> getActions(int zone_id) {
    List<ActionRecord> actions = getActions();
    List<ActionRecord> orderedActions = new LinkedList<ActionRecord>();
    SpatialCursor zoneActionRecords = dbHelper.prepare("SELECT action_id, zone_id, enter, exit " +
                                                       "FROM action_broadcast " +
                                                       "WHERE zone_id = '" + zone_id + "'");

    for(ActionRecord action : actions) {
      boolean found = false;
      while(zoneActionRecords.moveToNext()) {
        if(action.getID() == zoneActionRecords.getInt(0)) {
          found = true;
          orderedActions.add(new ActionRecord(
                                   action.getID(),
                                   action.getName(),
                                   action.getDescription(),
                                   zoneActionRecords.getInt(1),
                                   (zoneActionRecords.getInt(2) != 0),
                                   (zoneActionRecords.getInt(3) != 0)));
        }
      }
      if(found == false) {
        action.setZoneId(zone_id);
        orderedActions.add(action);
      }
    }
    zoneActionRecords.close();
    return orderedActions;
  }

  public void updateActionBroadcast(ActionRecord action) {
    Log.d(TAG, "updateActionBroadcast(), action_id: " + action.getID() + ", zone_id: " + action.getZoneId());

    dbHelper.exec("DELETE FROM action_broadcast " +
                  "WHERE action_id = '" + action.getID() + "' " +
                  "AND zone_id = '" + action.getZoneId() + "'");

    ContentValues values = new ContentValues();
    values.put("action_id", action.getID());
    values.put("zone_id", action.getZoneId());
    values.put("enter", (action.runOnEnter()? 1 : 0));
    values.put("exit", (action.runOnExit()? 1 : 0));
    dbHelper.insert("action_broadcast", values);
  }
  
}
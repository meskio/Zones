package org.anhonesteffort.polygons.database;

import android.content.ContentValues;
import android.util.Log;
import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.database.model.ActionRecord;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ActionDatabase {
  private static final String TAG = "org.anhonesteffort.zoneDb.database.ActionDatabase";
  private DatabaseHelper dbHelper;
  private ArrayList<LocationSubscriberChangeListener> listeners = new ArrayList<LocationSubscriberChangeListener>();

  public ActionDatabase(DatabaseHelper dbHelper) {
    this.dbHelper = dbHelper;
    
    if(isInitialized() == false) {
      initialize();
      Log.d(TAG, "Initialized the action tables.");
    }
  }

  public void initialize() {
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

  // Returns true if the action tables have already been initialized.
  public boolean isInitialized() {
    BetterStatement actions = dbHelper.prepare("SELECT 1 FROM action LIMIT 1");
    if(actions.step()) {
      actions.close();
      return true;
    }
    actions.close();
    return false;
  }
  
  public void addLocationSubscriberChangeListener(LocationSubscriberChangeListener listener) {
    listeners.add(listener);
  }

  public void removeLocationSubscriberChangeListener(LocationSubscriberChangeListener listener) {
    listeners.remove(listener);
  }
  
  private void subscriberChange() {
    for(LocationSubscriberChangeListener listener : listeners) {
      if(listener != null)
        listener.onSubscriberChange();
    }
  }
  
  // Returns a list of all phone numbers subscribed to location updates.
  public List<String> getLocationSubscribers() {
    List<String> subscriberNumbers = new LinkedList<String>();
    BetterStatement subscribers = dbHelper.prepare("SELECT phone_number FROM location_update_receiver");

    while(subscribers.step())
      subscriberNumbers.add(subscribers.getString(0));
    subscribers.close();
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

  // Returns a list of all available actionDb.
  public List<ActionRecord> getActions() {
    List<ActionRecord> actionNames = new LinkedList<ActionRecord>();
    BetterStatement actions = dbHelper.prepare("SELECT id, name, description FROM action");

    while(actions.step())
      actionNames.add(new ActionRecord(actions.getInt(0), actions.getString(1), actions.getString(2), -1));
    actions.close();
    return actionNames;
  }

  // Returns a list of all available actionDb with exit & enter appropriately set for the specified zone.
  public List<ActionRecord> getActions(int zone_id) {
    List<ActionRecord> actions = getActions();
    List<ActionRecord> orderedActions = new LinkedList<ActionRecord>();
    BetterStatement zoneActions = dbHelper.prepare("SELECT action_id, zone_id, enter, exit " +
                                                        "FROM action_broadcast " +
                                                        "WHERE zone_id = '" + zone_id + "'");

    for(ActionRecord action : actions) {
      boolean found = false;
      while(zoneActions.step()) {
        if(action.getID() == zoneActions.getInt(0)) {
          found = true;
          orderedActions.add(new ActionRecord(
                                   action.getID(),
                                   action.getName(),
                                   action.getDescription(),
                                   zoneActions.getInt(1),
                                   (zoneActions.getInt(2) != 0),
                                   (zoneActions.getInt(3) != 0)));
        }
      }
      if(found == false) {
        action.setZoneId(zone_id);
        orderedActions.add(action);
      }
    }
    zoneActions.close();
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
package org.anhonesteffort.polygons.storage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.anhonesteffort.polygons.R;

import android.util.Log;

public class ActionStorage {
  private static final String TAG = "org.anhonesteffort.polygons.storage.ActionStorage";
  private DatabaseHelper dbHelper;
  private ArrayList<LocationSubscriberChangeListener> listeners = new ArrayList<LocationSubscriberChangeListener>();

  public ActionStorage(DatabaseHelper dbHelper) {
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
                    "polygon_id INTEGER NOT NULL, " +
                    "enter INTEGER NOT NULL, " +
                    "exit INTEGER NOT NULL, " +
                    "PRIMARY KEY (action_id, polygon_id), " +
                    "FOREIGN KEY (action_id) REFERENCES action(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (polygon_id) REFERENCES polygon(id) ON DELETE CASCADE" +
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
                    "'" + R.integer.action_email_alert + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_email_alert) + "', " +
                    "'" + dbHelper.getStringResource(R.string.action_email_alert_description) + "'" +
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

  // Returns true if the polygon tables have already been initialized.
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

  // Returns a list of all available actions.
  public List<ActionBroadcastRecord> getActions() {
    List<ActionBroadcastRecord> actionNames = new LinkedList<ActionBroadcastRecord>();
    BetterStatement actions = dbHelper.prepare("SELECT id, name, description FROM action");

    while(actions.step())
      actionNames.add(new ActionBroadcastRecord(actions.getInt(0), actions.getString(1), actions.getString(2), 0, false, false));
    actions.close();
    return actionNames;
  }

  // Returns a list of all available actions with exit & enter appropriately set for the specified polygon.
  public List<ActionBroadcastRecord> getPolygonActions(int polygon_id) {
    List<ActionBroadcastRecord> actions = getActions();
    List<ActionBroadcastRecord> orderedActions = new LinkedList<ActionBroadcastRecord>();
    BetterStatement polygonActions = dbHelper.prepare("SELECT action_id, polygon_id, enter, exit " +
                                                        "FROM action_broadcast " +
                                                        "WHERE polygon_id = '" + polygon_id + "'");

    for(ActionBroadcastRecord action : actions) {
      boolean found = false;
      while(polygonActions.step()) {
        if(action.getID() == polygonActions.getInt(0)) {
          found = true;
          orderedActions.add(new ActionBroadcastRecord(
                                   action.getID(),
                                   action.getName(),
                                   action.getDescription(),
                                   polygonActions.getInt(1),
                                   (polygonActions.getInt(2) != 0),
                                   (polygonActions.getInt(3) != 0)));
        }
      }
      if(found == false) {
        action.setPolygonID(polygon_id);
        orderedActions.add(action);
      }
    }
    polygonActions.close();
    return orderedActions;
  }
  
  public void updatePolygonAction(ActionBroadcastRecord action) {
    Log.d(TAG, "updatePolygonAction(), action_id: " + action.getID() + ", polygon_id: " + action.getPolygonID());
    dbHelper.exec("DELETE FROM action_broadcast " +
                    "WHERE action_id = '" + action.getID() + "' " +
                    "AND polygon_id = '" + action.getPolygonID() + "'");

    dbHelper.exec("INSERT INTO action_broadcast (action_id, polygon_id, enter, exit) " +
                    "VALUES(" +
                      "'" + action.getID() + "', " +
                      "'" + action.getPolygonID() + "', " +
                      "'" + (action.runOnEnter()? 1 : 0) + "', " +
                      "'" + (action.runOnExit()? 1 : 0) + "')");
  }
  
}
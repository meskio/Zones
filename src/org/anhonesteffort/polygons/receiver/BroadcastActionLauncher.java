package org.anhonesteffort.polygons.receiver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import org.anhonesteffort.polygons.PreferencesActivity;
import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.ZoneService;
import org.anhonesteffort.polygons.action.AudioAlarmer;
import org.anhonesteffort.polygons.action.CallHistoryCleaner;
import org.anhonesteffort.polygons.action.ContactCleaner;
import org.anhonesteffort.polygons.action.LocationReporter;
import org.anhonesteffort.polygons.database.DatabaseHelper;
import org.anhonesteffort.polygons.database.model.ActionRecord;
import org.anhonesteffort.polygons.map.ZoneMapActivity;

import java.util.List;

public class BroadcastActionLauncher extends BroadcastReceiver {

  private static final String TAG = "BroadcastActionLauncher";

  private SharedPreferences sharedPreferences;
  private DevicePolicyManager policyManager;
  private ComponentName adminReceiver;

  private void showNotification(Context context, boolean enter, String title) {
    Log.d(TAG, "showNotification()");

    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                                                .setContentTitle(title)
                                                .setContentText(title);
    if(enter)
      mBuilder.setSmallIcon(R.drawable.polygon_enter);
    else
      mBuilder.setSmallIcon(R.drawable.polygon_exit);

    // Start the ZoneMapActivity if the user clicks on the notification.
    Intent resultIntent = new Intent(context, ZoneMapActivity.class);
    TaskStackBuilder stackBuilder = TaskStackBuilder.from(context);
    stackBuilder.addParentStack(ZoneMapActivity.class);
    stackBuilder.addNextIntent(resultIntent);
    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    mBuilder.setContentIntent(resultPendingIntent);

    // Display the notification.
    NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    mBuilder.setAutoCancel(true);
    mNotificationManager.notify(1020, mBuilder.getNotification());
  }

  private void launchAction(Context context, Bundle zoneData, ActionRecord action, String enter_exit) {
    Log.d(TAG, "launchAction() action_id: " + action.getID());

    Intent actionIntent;

    switch(action.getID()) {
      case R.integer.action_audio_alarm:
        actionIntent = new Intent(context, AudioAlarmer.class);
        context.startService(actionIntent);
        break;
      
      case R.integer.action_sms_alert:
        actionIntent = new Intent(context, LocationReporter.class);
        zoneData.putString(LocationReporter.ENTER_EXIT, enter_exit);
        zoneData.putBoolean(LocationReporter.SMS_REPORT, true);
        actionIntent.putExtras(zoneData);
        context.startService(actionIntent);
        break;
        
      case R.integer.action_super_lock:
        sharedPreferences.edit().putBoolean(PreferencesActivity.PREF_SUPER_LOCK, true).commit();
        if(policyManager.isAdminActive(adminReceiver))
          policyManager.lockNow();
        break;
        
      case R.integer.action_super_unlock:
        sharedPreferences.edit().putBoolean(PreferencesActivity.PREF_SUPER_LOCK, false).commit();
        break;
        
      case R.integer.action_clear_call_history:
        actionIntent = new Intent(context, CallHistoryCleaner.class);
        context.startService(actionIntent);
        break;
        
      case R.integer.action_clear_contacts:
        actionIntent = new Intent(context, ContactCleaner.class);
        context.startService(actionIntent);
        break;
        
      case R.integer.action_factory_reset:
        if(policyManager.isAdminActive(adminReceiver))
          policyManager.wipeData(0);
        break;
        
      default:
        break;
    }
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(TAG, "onReceive()");

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    policyManager = (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    adminReceiver = new ComponentName(context, AdminReceiver.class);

    DatabaseHelper applicationStorage = DatabaseHelper.getInstance(context);

    // Zone exit & enter related broadcasts.
    if(intent.getAction().equals(ZoneService.ZONE_ENTER) || intent.getAction().equals(ZoneService.ZONE_EXIT)) {
      List<ActionRecord> actions = applicationStorage.actionDb.getActions(intent.getExtras().getInt(ZoneService.ZONE_ID));
    
      // Launch zone enter & exit actions.
      for(ActionRecord action : actions) {
        if(intent.getAction().equals(ZoneService.ZONE_ENTER) && action.runOnEnter())
          launchAction(context, intent.getExtras(), action, intent.getAction());
        else if(intent.getAction().equals(ZoneService.ZONE_EXIT) && action.runOnExit())
          launchAction(context, intent.getExtras(), action, intent.getAction());
      }
      
      // Show status bar notifications.
      if(sharedPreferences.getBoolean(PreferencesActivity.PREF_NOTIFICATIONS, false)) {
        if(intent.getAction().equals(ZoneService.ZONE_ENTER))
          showNotification(context, true, context.getString(R.string.entered_zone) + " " + intent.getExtras().getString(ZoneService.ZONE_LABEL));
        else if(intent.getAction().equals(ZoneService.ZONE_EXIT))
          showNotification(context, false, context.getString(R.string.exited_zone) + " " + intent.getExtras().getString(ZoneService.ZONE_LABEL));
      }
      
      // Vibrate on zone enter & exit.
      if(sharedPreferences.getBoolean(PreferencesActivity.PREF_VIBRATE, false)) {
        Vibrator vibrate = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 200, 200, 200, 200, 200};
        vibrate.vibrate(pattern, -1);
      }
    }
    
    // Enforce the super lock.
    else if(sharedPreferences.getBoolean(PreferencesActivity.PREF_SUPER_LOCK, false) && intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
      if(policyManager.isAdminActive(adminReceiver)) {
        Log.d(TAG, "policyManager.lockNow()");
        policyManager.lockNow();
      }
    }
  }

}

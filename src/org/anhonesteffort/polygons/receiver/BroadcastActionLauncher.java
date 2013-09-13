package org.anhonesteffort.polygons.receiver;

import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.PreferencesActivity;
import org.anhonesteffort.polygons.ZoneService;
import org.anhonesteffort.polygons.action.*;
import org.anhonesteffort.polygons.database.model.ActionRecord;
import org.anhonesteffort.polygons.map.ZoneMapActivity;
import org.anhonesteffort.polygons.database.DatabaseHelper;

import java.util.List;

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

public class BroadcastActionLauncher extends BroadcastReceiver {
  private static final String TAG = "org.anhonesteffort.polygons.receiver.BroadcastActionLauncher";
  private SharedPreferences settings;

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

  private void launchAction(Context context, Bundle polygonData, ActionRecord action, String enter_exit) {
    Log.d(TAG, "launchAction() action_id: " + action.getID());
    Intent actionIntent;
    DevicePolicyManager policyManager;
    ComponentName adminReceiver;
    
    switch(action.getID()) {
      case R.integer.action_audio_alarm:
        actionIntent = new Intent(context, AudioAlarmer.class);
        context.startService(actionIntent);
        break;
      
      case R.integer.action_sms_alert:
        actionIntent = new Intent(context, LocationReporter.class);
        polygonData.putString(LocationReporter.ENTER_EXIT, enter_exit);
        polygonData.putBoolean(LocationReporter.SMS_REPORT, true);
        actionIntent.putExtras(polygonData);
        context.startService(actionIntent);
        break;
        
      case R.integer.action_email_alert:
        actionIntent = new Intent(context, LocationReporter.class);
        polygonData.putString(LocationReporter.ENTER_EXIT, enter_exit);
        polygonData.putBoolean(LocationReporter.EMAIL_REPORT, true);
        actionIntent.putExtras(polygonData);
        context.startService(actionIntent);
        break;
        
      case R.integer.action_super_lock:
        settings.edit().putBoolean(PreferencesActivity.PREF_SUPER_LOCK, true).commit();
        policyManager = (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminReceiver = new ComponentName(context, AdminReceiver.class);
        if(policyManager.isAdminActive(adminReceiver))
          policyManager.lockNow();
        break;
        
      case R.integer.action_super_unlock:
        settings.edit().putBoolean(PreferencesActivity.PREF_SUPER_LOCK, false).commit();
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
        policyManager = (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminReceiver = new ComponentName(context, AdminReceiver.class);
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
    DatabaseHelper applicationStorage = DatabaseHelper.getInstance(context);
    settings = PreferenceManager.getDefaultSharedPreferences(context);
    
    // Polygon exit & enter related broadcasts.
    if(intent.getAction().equals(ZoneService.POLYGON_ENTER) || intent.getAction().equals(ZoneService.POLYGON_EXIT)) {
      List<ActionRecord> actions = applicationStorage.actionDb.getActions(intent.getExtras().getInt(ZoneService.POLYGON_ID));
    
      // Launch polygon enter & exit actionDb.
      for(ActionRecord action : actions) {
        if(intent.getAction().equals(ZoneService.POLYGON_ENTER) && action.runOnEnter())
          launchAction(context, intent.getExtras(), action, intent.getAction());
        else if(intent.getAction().equals(ZoneService.POLYGON_EXIT) && action.runOnExit())
          launchAction(context, intent.getExtras(), action, intent.getAction());
      }
      
      // Show status bar notifications.
      if(settings.getBoolean(PreferencesActivity.PREF_NOTIFICATIONS, false)) {
        if(intent.getAction().equals(ZoneService.POLYGON_ENTER))
          showNotification(context, true, context.getString(R.string.entered_polygon) + " " + intent.getExtras().getString(ZoneService.POLYGON_LABEL));
        else if(intent.getAction().equals(ZoneService.POLYGON_EXIT))
          showNotification(context, false, context.getString(R.string.exited_polygon) + " " + intent.getExtras().getString(ZoneService.POLYGON_LABEL));
      }
      
      // Vibrate on polygon enter & exit.
      if(settings.getBoolean(PreferencesActivity.PREF_VIBRATE, false)) {
        Vibrator vibrate = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 200, 200, 200, 200, 200};
        vibrate.vibrate(pattern, -1);
      }
    }
    
    // Enforce the super lock.
    else if(settings.getBoolean(PreferencesActivity.PREF_SUPER_LOCK, false) && intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
      DevicePolicyManager policyManager = (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
      ComponentName adminReceiver = new ComponentName(context, AdminReceiver.class);
      if(policyManager.isAdminActive(adminReceiver)) {
        Log.d(TAG, "policyManager.lockNow()");
        policyManager.lockNow();
      }
    }
  }
}

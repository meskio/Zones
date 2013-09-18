package org.anhonesteffort.polygons.receiver;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;

import org.anhonesteffort.polygons.PreferencesActivity;
import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.ZoneService;
import org.anhonesteffort.polygons.action.AudioAlarmer;
import org.anhonesteffort.polygons.action.CallHistoryCleaner;
import org.anhonesteffort.polygons.action.ContactCleaner;
import org.anhonesteffort.polygons.database.DatabaseHelper;
import org.anhonesteffort.polygons.transport.sms.SMSSender;

import java.util.Locale;

public class SMSActionLauncher extends BroadcastReceiver {

  private static final String TAG = "SMSActionLauncher";

  private boolean messageIsAuthorized(String message, String password) {
    message = message.trim();
    int j = 0;
    
    if(password.length() == 0)
      return true;

    for(int i = (message.length() - password.length()); i < message.length(); i++) {
      if(i < 0 || message.charAt(i) != password.charAt(j++))
        return false;
    }

    if(j == password.length())
      return true;

    return false;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(TAG, "onReceive()");

    Object[] pdus = (Object[])intent.getExtras().get("pdus");
    SmsMessage shortMessage = SmsMessage.createFromPdu((byte[]) pdus[0]);

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    DatabaseHelper applicationStorage = DatabaseHelper.getInstance(context);
    Intent resultIntent;
    boolean match = false;

    // Only process commands if allowed in application preferences.
    if(settings.getBoolean(PreferencesActivity.PREF_SMS_COMMANDS, true)) {
      Log.d(TAG, "SMS message sender: " + shortMessage.getOriginatingAddress());
      Log.d(TAG, "SMS message text: " + shortMessage.getDisplayMessageBody());

      String message = shortMessage.getDisplayMessageBody();
      String commandPassword = settings.getString(PreferencesActivity.PREF_SMS_COMMAND_PASSWORD, "");
      
      // Only run authorized commands.
      if(messageIsAuthorized(message, commandPassword)) {
        Log.d(TAG, "SMS message is authorized.");

        // Command: start location updates.
        if(message.toLowerCase(Locale.ENGLISH).startsWith(context.getString(R.string.command_start_location_updates))) {
          applicationStorage.actionDatabase.addLocationSubscriber(shortMessage.getOriginatingAddress());
          SMSSender.sendTextMessage(shortMessage.getOriginatingAddress(), context.getString(R.string.command_start_location_updates_response));
          match = true;
        }
        
        // Command: stop location updates.
        else if(message.toLowerCase(Locale.ENGLISH).startsWith(context.getString(R.string.command_stop_location_updates))) {
          applicationStorage.actionDatabase.removeLocationSubscriber(shortMessage.getOriginatingAddress());
          SMSSender.sendTextMessage(shortMessage.getOriginatingAddress(), context.getString(R.string.command_stop_location_updates_response));
          match = true;
        }
        
        // Command: battery level.
        else if(message.toLowerCase(Locale.ENGLISH).startsWith(context.getString(R.string.command_battery_level))) {
          IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
          Intent batteryStatus = context.getApplicationContext().registerReceiver(null, ifilter);
          int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
          int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
          SMSSender.sendTextMessage(shortMessage.getOriginatingAddress(), context.getString(R.string.command_battery_level_response) + " " + level + "/" + scale);
          match = true;
        }
        
        // Command: audio alarm.
        else if(message.toLowerCase(Locale.ENGLISH).startsWith(context.getString(R.string.command_audio_alarm))) {
          resultIntent = new Intent(context, AudioAlarmer.class);
          context.startService(resultIntent);
          SMSSender.sendTextMessage(shortMessage.getOriginatingAddress(), context.getString(R.string.command_audio_alarm_response));
          match = true;
        }
        
        // Command: super lock.
        else if(message.toLowerCase(Locale.ENGLISH).startsWith(context.getString(R.string.command_super_lock))) {
          settings.edit().putBoolean(PreferencesActivity.PREF_SUPER_LOCK, true).commit();
          DevicePolicyManager policyManager = (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
          ComponentName adminReceiver = new ComponentName(context, AdminReceiver.class);
          
          if(policyManager.isAdminActive(adminReceiver)) {
            policyManager.lockNow();
            SMSSender.sendTextMessage(shortMessage.getOriginatingAddress(), context.getString(R.string.command_super_lock_response));
          }
          else
            SMSSender.sendTextMessage(shortMessage.getOriginatingAddress(), context.getString(R.string.error_device_admin));
          
          match = true;
        }
        
        // Command: super unlock.
        else if(message.toLowerCase(Locale.ENGLISH).startsWith(context.getString(R.string.command_super_unlock))) {
          settings.edit().putBoolean(PreferencesActivity.PREF_SUPER_LOCK, false).commit();
          SMSSender.sendTextMessage(shortMessage.getOriginatingAddress(), context.getString(R.string.command_super_unlock_response));
          match = true;
        }
        
        // Command: clear call history.
        else if(message.toLowerCase(Locale.ENGLISH).startsWith(context.getString(R.string.command_clear_call_history))) {
          resultIntent = new Intent(context, CallHistoryCleaner.class);
          context.startService(resultIntent);
          SMSSender.sendTextMessage(shortMessage.getOriginatingAddress(), context.getString(R.string.command_clear_call_history_response));
          match = true;
        }
        
        // Command: clear contacts.
        else if(message.toLowerCase(Locale.ENGLISH).startsWith(context.getString(R.string.command_clear_contacts))) {
          resultIntent = new Intent(context, ContactCleaner.class);
          context.startService(resultIntent);
          SMSSender.sendTextMessage(shortMessage.getOriginatingAddress(), context.getString(R.string.command_clear_contacts_response));
          match = true;
        }
        
        // Command: factory reset.
        else if(message.toLowerCase(Locale.ENGLISH).startsWith(context.getString(R.string.command_factory_reset))) {
          DevicePolicyManager policyManager = (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
          ComponentName adminReceiver = new ComponentName(context, AdminReceiver.class);

          if(policyManager.isAdminActive(adminReceiver)) {
            policyManager.wipeData(0);
            SMSSender.sendTextMessage(shortMessage.getOriginatingAddress(), context.getString(R.string.command_factory_reset_response));
          }
          else
            SMSSender.sendTextMessage(shortMessage.getOriginatingAddress(), context.getString(R.string.error_device_admin));

          match = true;
        }
        
        // Start the background service if not already running.
        if(match) {
          resultIntent = new Intent(context.getApplicationContext(), ZoneService.class);
          context.getApplicationContext().startService(resultIntent);
        }
      }
    }
  }

}

package org.anhonesteffort.polygons;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import org.anhonesteffort.polygons.ZoneService.ZoneServiceBinder;
import org.anhonesteffort.polygons.receiver.AdminReceiver;

@SuppressWarnings("deprecation")
public class PreferencesActivity extends SherlockPreferenceActivity {

  private static final String TAG = "PreferencesActivity";

  public static final String PREF_GEOFENCING            = "pref_allow_geofencing";
  public static final String PREF_DEVICE_ADMIN          = "pref_allow_device_admin";
  public static final String PREF_AUDIO_ALARM_LENGTH    = "pref_audio_alarm_length";
  public static final String PREF_NOTIFICATIONS         = "pref_allow_notifications";
  public static final String PREF_VIBRATE               = "pref_allow_vibrate";
  public static final String PREF_SUPER_LOCK            = "pref_super_lock";

  public static final String PREF_SMS_DEFAULT_RECEIVER  = "pref_sms_default_receiver";
  public static final String PREF_SMS_COMMANDS          = "pref_allow_sms_commands";
  public static final String PREF_SMS_COMMAND_PASSWORD  = "pref_sms_command_password";

  private ZoneService zoneService;
  private boolean zoneServiceBound = false;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate()");

    this.addPreferencesFromResource(R.xml.preferences);
    this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    this.getSupportActionBar().setSubtitle(R.string.menu_title_settings);

    Intent locationWatchIntent = new Intent(this, ZoneService.class);
    startService(locationWatchIntent);
    bindService(locationWatchIntent, mConnection, Context.BIND_AUTO_CREATE);

    updateAdminPreferenceCheckBox();
    this.findPreference(PREF_GEOFENCING).setOnPreferenceChangeListener(new GeofencingClickListener());
    this.findPreference(PREF_DEVICE_ADMIN).setOnPreferenceChangeListener(new DeviceAdminClickListener());
  }

  @Override
  public void onActivityResult(int reqCode, int resultCode, Intent data) {
    super.onActivityResult(reqCode, resultCode, data);
    updateAdminPreferenceCheckBox();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        break;
    }
    return true;
  }
  
  @Override
  protected void onStop() {
    super.onStop();
    Log.d(TAG, "onStop()");

    if(zoneServiceBound) {
      unbindService(mConnection);
      zoneServiceBound = false;
    }
  }

  private ServiceConnection mConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      Log.d(TAG, "onServiceConnected()");
      ZoneServiceBinder binder = (ZoneServiceBinder) service;
      zoneService = binder.getService();
      zoneServiceBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
      Log.d(TAG, "onServiceDisconnected()");
      zoneServiceBound = false;
    }

  };

  private void updateAdminPreferenceCheckBox() {
    DevicePolicyManager policyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
    ComponentName adminReceiver = new ComponentName(PreferencesActivity.this, AdminReceiver.class);
    CheckBoxPreference allowAdmin = (CheckBoxPreference) this.findPreference(PREF_DEVICE_ADMIN);

    if(policyManager.isAdminActive(adminReceiver) == false)
      allowAdmin.setChecked(false);
    else
      allowAdmin.setChecked(true);
  }

  private class GeofencingClickListener implements OnPreferenceChangeListener {

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      CheckBoxPreference enableGeofencing = (CheckBoxPreference) preference;

      if((Boolean) newValue) {
        if(zoneServiceBound)
          zoneService.onGeofencingPreferenceChange(true);
        enableGeofencing.setChecked(true);
      }
      else {
        if(zoneServiceBound)
          zoneService.onGeofencingPreferenceChange(false);
        enableGeofencing.setChecked(false);
      }

      return false;
    }
  }

  private class DeviceAdminClickListener implements OnPreferenceChangeListener {
    DevicePolicyManager policyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
    ComponentName adminReceiver = new ComponentName(PreferencesActivity.this, AdminReceiver.class);

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      CheckBoxPreference deviceAdminCheckBox = (CheckBoxPreference) preference;

      if((Boolean) newValue) {
        deviceAdminCheckBox.setChecked(true);
        ComponentName mDeviceAdmin = new ComponentName(PreferencesActivity.this, AdminReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, PreferencesActivity.this.getString(R.string.device_admin_description));
        PreferencesActivity.this.startActivityForResult(intent, 1);
      }
      else {
        policyManager.removeActiveAdmin(adminReceiver);
        deviceAdminCheckBox.setChecked(false);
      }

      return false;
    }
  }

}

package org.anhonesteffort.polygons;

import java.util.LinkedList;

import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.ZoneService.ZoneServiceBinder;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

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
import android.preference.PreferenceCategory;
import android.util.Log;

@SuppressWarnings("deprecation")
public class PreferencesActivity extends SherlockPreferenceActivity {
  public static final String PREF_GEOFENCING            = "pref_allow_geofencing";
  public static final String PREF_DEVICE_ADMIN          = "pref_allow_device_admin";
  public static final String PREF_AUDIO_ALARM_LENGTH    = "pref_audio_alarm_length";
  public static final String PREF_NOTIFICATIONS         = "pref_allow_notifications";
  public static final String PREF_VIBRATE               = "pref_allow_vibrate";
  public static final String PREF_SUPER_LOCK            = "pref_super_lock";

  public static final String PREF_SMS_DEFAULT_RECEIVER  = "pref_sms_default_receiver";
  public static final String PREF_SMS_COMMANDS          = "pref_allow_sms_commands";
  public static final String PREF_SMS_COMMAND_PASSWORD  = "pref_sms_command_password";

  public static final String CATEGORY_EMAIL             = "category_email";
  public static final String PREF_EMAIL                 = "pref_allow_email";
  public static final String PREF_EMAIL_RECEIVER        = "pref_email_receiver";
  public static final String PREF_EMAIL_USERNAME        = "pref_smtp_username";
  public static final String PREF_EMAIL_PASSWORD        = "pref_smtp_password";
  public static final String PREF_EMAIL_SERVER          = "pref_smtp_server";
  public static final String PREF_EMAIL_PORT            = "pref_smtp_ssl_port";

  private static final String TAG = "org.anhonesteffort.polygons.PreferencesActivity";
  private LinkedList<Preference> preferences = new LinkedList<Preference>();
  private ZoneService zoneService;
  private boolean zoneServiceBound = false;
  
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
  
  private void setAdminPreference() {
    DevicePolicyManager policyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
    ComponentName adminReceiver = new ComponentName(PreferencesActivity.this, AdminReceiver.class);
    CheckBoxPreference allowAdmin = (CheckBoxPreference) this.findPreference(PREF_DEVICE_ADMIN);

    if(policyManager.isAdminActive(adminReceiver) == false)
      allowAdmin.setChecked(false);
    else
      allowAdmin.setChecked(true);
  }

  private void hideEmailPreferences() {
    PreferenceCategory emailPreferences = (PreferenceCategory) PreferencesActivity.this.findPreference(CATEGORY_EMAIL);
    emailPreferences.removePreference(this.findPreference(PREF_EMAIL_RECEIVER));
    emailPreferences.removePreference(this.findPreference(PREF_EMAIL_USERNAME));
    emailPreferences.removePreference(this.findPreference(PREF_EMAIL_PASSWORD));
    emailPreferences.removePreference(this.findPreference(PREF_EMAIL_SERVER));
    emailPreferences.removePreference(this.findPreference(PREF_EMAIL_PORT));
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
      CheckBoxPreference allowAdmin = (CheckBoxPreference) preference;

      if((Boolean) newValue) {
        allowAdmin.setChecked(true);
        ComponentName mDeviceAdmin = new ComponentName(PreferencesActivity.this, AdminReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, PreferencesActivity.this.getString(R.string.device_admin_description));
        PreferencesActivity.this.startActivityForResult(intent, 1);
      }
      else {
        policyManager.removeActiveAdmin(adminReceiver);
        allowAdmin.setChecked(false);
      }
      
      return false;
    }
  }

  private class EmailPreferenceChangeListener implements OnPreferenceChangeListener {
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      CheckBoxPreference allowEmail = (CheckBoxPreference) preference;
      if((Boolean) newValue) {
        PreferenceCategory emailPreferences = (PreferenceCategory) PreferencesActivity.this.findPreference(CATEGORY_EMAIL);
        for(Preference emailPreference : preferences)
          emailPreferences.addPreference(emailPreference);
        allowEmail.setChecked(true);
      }
      else {
        hideEmailPreferences();
        allowEmail.setChecked(false);
      }
      
      return false;
    }
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate()");
    
    super.onCreate(savedInstanceState);
    this.addPreferencesFromResource(R.xml.application_preferences);
    this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    this.getSupportActionBar().setSubtitle(R.string.menu_settings);
    
    // Bind to the ZoneService background service.
    Intent locationWatchIntent = new Intent(this, ZoneService.class);
    startService(locationWatchIntent);
    bindService(locationWatchIntent, mConnection, Context.BIND_AUTO_CREATE);

    preferences.add(this.findPreference(PREF_EMAIL_RECEIVER));
    preferences.add(this.findPreference(PREF_EMAIL_USERNAME));
    preferences.add(this.findPreference(PREF_EMAIL_PASSWORD));
    preferences.add(this.findPreference(PREF_EMAIL_SERVER));
    preferences.add(this.findPreference(PREF_EMAIL_PORT));

    setAdminPreference();
    this.findPreference(PREF_GEOFENCING).setOnPreferenceChangeListener(new GeofencingClickListener());
    this.findPreference(PREF_DEVICE_ADMIN).setOnPreferenceChangeListener(new DeviceAdminClickListener());

    CheckBoxPreference allowEmail = (CheckBoxPreference) this.findPreference(PREF_EMAIL);
    allowEmail.setOnPreferenceChangeListener(new EmailPreferenceChangeListener());
    if(allowEmail.isChecked() == false)
      hideEmailPreferences();
  }

  @Override
  public void onActivityResult(int reqCode, int resultCode, Intent data) {
    super.onActivityResult(reqCode, resultCode, data);
    setAdminPreference();
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
    Log.d(TAG, "onStop()");
    super.onStop();
    
    if(zoneServiceBound) {
      unbindService(mConnection);
      zoneServiceBound = false;
    }
  }
}

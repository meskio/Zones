package org.anhonesteffort.polygons.action;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.anhonesteffort.polygons.PreferencesActivity;
import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.ZoneService;
import org.anhonesteffort.polygons.transport.sms.SMSSender;

public class LocationReporter extends IntentService {

  private static final String TAG         = "LocationReporter";

  public static final String ENTER_EXIT   = "org.anhonesteffort.polygons.action.LocationReporter.ENTER_EXIT";
  public static final String SMS_REPORT   = "org.anhonesteffort.polygons.action.LocationReporter.SMS";

  private Bundle eventData;
  private SharedPreferences settings;

  public LocationReporter() {
    super("LocationReporter");
  }

  private String createMessage() {
    String message = "";

    if(eventData.getString(ENTER_EXIT).equals(ZoneService.ZONE_ENTER))
      message = this.getString(R.string.entered_zone);
    else
      message = this.getString(R.string.exited_zone);

    message += " " + eventData.getString(ZoneService.ZONE_LABEL) + ". ";

    double[] phone_location = eventData.getDoubleArray(ZoneService.DEVICE_LOCATION);
    message += this.getString(R.string.device_location) + " (" + Double.toString(phone_location[0]) + ", " + Double.toString(phone_location[1]) + ")";
    return message;
  }

  private void sendZoneSMS() {
    Log.d(TAG, "sendZoneSMS()");

    String receiver = settings.getString(PreferencesActivity.PREF_SMS_DEFAULT_RECEIVER, null);
    if(receiver != null)
      SMSSender.sendTextMessage(receiver, createMessage());
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.d(TAG, "onHandleIntent()");

    eventData = intent.getExtras();
    settings = PreferenceManager.getDefaultSharedPreferences(this);

    if(eventData.containsKey(SMS_REPORT))
      sendZoneSMS();
  }
}

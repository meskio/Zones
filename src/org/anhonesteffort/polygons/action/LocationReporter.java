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
import org.anhonesteffort.polygons.transport.smtp.SMTPClient;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

public class LocationReporter extends IntentService {
  private static final String TAG         = "org.anhonesteffort.polygons.action.LocationReporter";
  public static final String ENTER_EXIT   = "org.anhonesteffort.polygons.action.LocationReporter.ENTER_EXIT";
  public static final String SMS_REPORT   = "org.anhonesteffort.polygons.action.LocationReporter.SMS";
  public static final String EMAIL_REPORT = "org.anhonesteffort.polygons.action.LocationReporter.EMAIL";

  private Bundle eventData;
  private SharedPreferences settings;

  public LocationReporter() {
    super("LocationReporter");
  }

  private String createMessage() {
    String message = "";
    double[] phone_location;

    if(eventData.getString(ENTER_EXIT).equals(ZoneService.POLYGON_ENTER))
      message = this.getString(R.string.entered_polygon);
    else
      message = this.getString(R.string.exited_polygon);
    message += " " + eventData.getString(ZoneService.POLYGON_LABEL) + ". ";

    phone_location = eventData.getDoubleArray(ZoneService.PHONE_LOCATION);
    message += this.getString(R.string.device_location) + " (" + Double.toString(phone_location[0]) + ", " + Double.toString(phone_location[1]) + ")";
    return message;
  }

  private void sendPolygonSMS() {
    Log.d(TAG, "sendPolygonSMS()");
    String receiver = settings.getString(PreferencesActivity.PREF_SMS_DEFAULT_RECEIVER, null);
    if(receiver != null)
      SMSSender.sendTextMessage(receiver, createMessage());
  }

  private void sendPolygonEmail() {
    Log.d(TAG, "sendPolygonEmail()");
    if(settings.getBoolean(PreferencesActivity.PREF_EMAIL, false)) {
      try {
        SMTPClient email = new SMTPClient(settings.getString(PreferencesActivity.PREF_EMAIL_USERNAME, ""),
                                settings.getString(PreferencesActivity.PREF_EMAIL_PASSWORD, ""),
                                settings.getString(PreferencesActivity.PREF_EMAIL_SERVER, ""),
                                SMTPClient.AuthType.SSL,
                                Integer.parseInt(settings.getString(PreferencesActivity.PREF_EMAIL_PORT, "465")));
        Thread.currentThread().setContextClassLoader(email.getClass().getClassLoader());
        email.sendMessage(settings.getString(PreferencesActivity.PREF_EMAIL_RECEIVER, ""),
                            this.getString(R.string.email_subject),
                            createMessage());

      } catch (AddressException e) {
        e.printStackTrace();
      } catch (MessagingException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.d(TAG, "onHandleIntent()");
    eventData = intent.getExtras();
    settings = PreferenceManager.getDefaultSharedPreferences(this);

    if(eventData.containsKey(SMS_REPORT))
      sendPolygonSMS();

    if(eventData.containsKey(EMAIL_REPORT))
      sendPolygonEmail();
  }
}

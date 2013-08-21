package org.anhonesteffort.polygons.communication;

import android.telephony.SmsManager;
import android.util.Log;

public class SMSSender {
  private static final String TAG = "org.anhonesteffort.polygons.communication.SMSSender";
  
  public static boolean sendTextMessage(String recipient, String message) {
    if(message != null && recipient != null) {
      if(recipient.contains("+") == false)
        recipient = "+" + recipient;
      
      Log.d(TAG, "sendTextMessage(), recipient: " + recipient + ", message: " + message);
      
      SmsManager smsManager = SmsManager.getDefault();
      smsManager.sendTextMessage(recipient, null, message, null, null);
      return true;
    }
    
    return false;
  }
  
}

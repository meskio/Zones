package org.anhonesteffort.polygons.transport.sms;

import android.telephony.SmsManager;

public class SMSSender {
  
  public static boolean sendTextMessage(String recipient, String message) {
    if(message != null && recipient != null) {
      if(recipient.contains("+") == false)
        recipient = "+" + recipient;
      
      SmsManager smsManager = SmsManager.getDefault();
      smsManager.sendTextMessage(recipient, null, message, null, null);
      return true;
    }
    
    return false;
  }
  
}

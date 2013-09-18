package org.anhonesteffort.polygons.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AdminReceiver extends DeviceAdminReceiver {

  private static final String TAG = "AdminReceiver";

  @Override
  public void onEnabled(Context context, Intent intent) {
    Log.d(TAG, "onEndabled()");    
  }

  @Override
  public void onDisabled(Context context, Intent intent) {
    Log.d(TAG, "onDisabled()"); 
  }
}

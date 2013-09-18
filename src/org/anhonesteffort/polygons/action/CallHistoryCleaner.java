package org.anhonesteffort.polygons.action;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class CallHistoryCleaner extends IntentService {

  private static final String TAG = "CallHistoryCleaner";
  
  public CallHistoryCleaner() {
    super("CallHistoryCleaner");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.d(TAG, "onHandleIntent()");
    this.getContentResolver().delete(android.provider.CallLog.Calls.CONTENT_URI, null, null);
  }
}

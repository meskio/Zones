package org.anhonesteffort.polygons.action;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

public class ContactCleaner extends IntentService {
  private static final String TAG = "org.anhonesteffort.polygons.action.ContactCleaner";

  public ContactCleaner() {
    super("ContactCleaner");

  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.d(TAG, "onHandleIntent()");
    ContentResolver contentResolver = this.getContentResolver();
    Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
    
    while (cursor.moveToNext()) {
      String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
      Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
      contentResolver.delete(uri, null, null);
    }
  }
}

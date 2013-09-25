package org.anhonesteffort.polygons;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import org.anhonesteffort.polygons.database.DatabaseHelper;
import org.anhonesteffort.polygons.database.model.ZoneRecord;

public class ActionListActivity extends SherlockActivity {

  private static final String TAG    = "ActionListActivity";
  public static final String ZONE_ID = "org.anhonesteffort.polygons.ActionListActivity.ZONE_ID";

  private DatabaseHelper databaseHelper;
  private ZoneRecord selectedZone;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate()");

    databaseHelper = DatabaseHelper.getInstance(this);
    selectedZone = databaseHelper.getZoneDatabase().getZone(getIntent().getExtras().getInt(ZONE_ID));

    setContentView(R.layout.action_list_layout);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(this.getString(R.string.title_edit_actions));
    getSupportActionBar().setSubtitle(selectedZone.getLabel());
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume()");

    new InitializeListTask().execute();
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

  private class InitializeListTask extends AsyncTask<Void, Void, Integer> {

    private Cursor zoneActions;

    protected Integer doInBackground(Void... params) {
      zoneActions = databaseHelper.getActionDatabase().getActions(selectedZone.getId());
      return 0;
    }

    protected void onProgressUpdate(Void... progress) {
      // Nothing to do.
    }

    protected void onPostExecute(Integer result) {
      CursorAdapter actionAdapter = new ActionCursorAdapter(ActionListActivity.this, zoneActions);
      ListView actionList = (ListView) findViewById(R.id.list);

      actionList.setAdapter(actionAdapter);
    }
  }

}

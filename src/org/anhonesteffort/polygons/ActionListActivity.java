package org.anhonesteffort.polygons;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import org.anhonesteffort.polygons.database.DatabaseHelper;
import org.anhonesteffort.polygons.database.model.ActionRecord;
import org.anhonesteffort.polygons.database.model.ZoneRecord;

import java.util.List;

public class ActionListActivity extends SherlockActivity {

  private static final String TAG    = "ActionListActivity";
  public static final String ZONE_ID = "org.anhonesteffort.polygons.ActionListActivity.ZONE_ID";

  private DatabaseHelper applicationStorage;
  private ZoneRecord selectedZone;
  private ListView actionList;

  private void initializeList() {
    List<ActionRecord> zoneActions = applicationStorage.getActionDatabase().getActions(selectedZone.getId());
    ArrayAdapter<ActionRecord> actionArrayAdapter = new ActionArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, zoneActions);

    actionList = (ListView) findViewById(R.id.list);
    actionList.setAdapter(actionArrayAdapter);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate()");

    applicationStorage = DatabaseHelper.getInstance(this.getBaseContext());
    selectedZone = applicationStorage.getZoneDatabase().getZone(getIntent().getExtras().getInt(ZONE_ID));

    setContentView(R.layout.action_list_layout);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(this.getString(R.string.title_edit_actions));
    getSupportActionBar().setSubtitle(selectedZone.getLabel());
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume()");

    initializeList();
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
}

package org.anhonesteffort.polygons;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import org.anhonesteffort.polygons.action.ActionArrayAdapter;
import org.anhonesteffort.polygons.database.DatabaseHelper;
import org.anhonesteffort.polygons.database.model.ActionRecord;
import org.anhonesteffort.polygons.database.model.ZoneRecord;

import java.util.List;

public class ActionListActivity extends SherlockActivity {
  private static final String TAG          = "org.anhonesteffort.polygons.ActionListActivity";
  public static final String ZONE_ID       = "org.anhonesteffort.polygons.action.PID";
  public static final String ENTER_ACTIONS = "org.anhonesteffort.polygons.action.ENTER";
  public static final String EXIT_ACTIONS  = "org.anhonesteffort.polygons.action.EXIT";

  private ListView actionList;

  private void initializeList() {
    DatabaseHelper applicationStorage = DatabaseHelper.getInstance(this.getBaseContext());
    List<ActionRecord> polygonActions = applicationStorage.actionDb.getActions(getIntent().getExtras().getInt(ZONE_ID));
    ArrayAdapter<ActionRecord> adapter = new ActionArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, polygonActions);

    actionList = (ListView) findViewById(R.id.list);
    actionList.setAdapter(adapter);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate()");
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.action_list_layout);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(this.getString(R.string.title_edit_actions));

    DatabaseHelper applicationStorage = DatabaseHelper.getInstance(this.getBaseContext());
    ZoneRecord selectedZone = applicationStorage.zoneDb.getZone(getIntent().getExtras().getInt(ZONE_ID));
    getSupportActionBar().setSubtitle(selectedZone.getLabel());
  }

  @Override
  public void onResume() {
    Log.d(TAG, "onResume()");
    super.onResume();
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

package org.anhonesteffort.polygons.action;

import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.geometry.TaggedPoint;
import org.anhonesteffort.polygons.geometry.TaggedPolygon;
import org.anhonesteffort.polygons.storage.ActionBroadcastRecord;
import org.anhonesteffort.polygons.storage.DatabaseHelper;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ActionListActivity extends SherlockActivity {
  private static final String TAG           = "org.anhonesteffort.polygons.action.ActionListActivity";
  public static final String POLYGON_ID     = "org.anhonesteffort.maptest.prompt.PID";
  public static final String ENTER_ACTIONS  = "org.anhonesteffort.maptest.prompt.ENTER";
  public static final String EXIT_ACTIONS   = "org.anhonesteffort.maptest.prompt.EXIT";

  private ListView actionList;

  private void initializeList() {
    DatabaseHelper applicationStorage = DatabaseHelper.getInstance(this.getBaseContext());
    List<ActionBroadcastRecord> polygonActions = applicationStorage.actions.getPolygonActions(getIntent().getExtras().getInt(POLYGON_ID));
    ArrayAdapter<ActionBroadcastRecord> adapter = new ActionArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, polygonActions);

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
    TaggedPolygon<TaggedPoint> selectedPolygon = applicationStorage.polygons.getPolygon(getIntent().getExtras().getInt(POLYGON_ID));
    getSupportActionBar().setSubtitle(selectedPolygon.getLabel());
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

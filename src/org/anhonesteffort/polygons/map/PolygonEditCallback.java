package org.anhonesteffort.polygons.map;

import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.action.ActionListActivity;
import org.anhonesteffort.polygons.storage.DatabaseHelper;

import android.content.Intent;
import android.util.Log;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class PolygonEditCallback implements ActionMode.Callback {
  private static final String TAG = "org.anhonesteffort.polygons.map.PolygonEditCallback";
  private PolygonMapActivity mapActivity;
  private GooglePolygonMap polygonMap;
  private DatabaseHelper applicationStorage;

  public PolygonEditCallback(PolygonMapActivity mapActivity, GooglePolygonMap polygonMap) {
    this.polygonMap = polygonMap;
    this.mapActivity = mapActivity;
    applicationStorage = DatabaseHelper.getInstance(mapActivity.getApplicationContext());
  }

  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    Log.d(TAG, "onCreateActionMode()");
    MenuInflater inflater = mode.getMenuInflater();
    inflater.inflate(R.menu.edit_polygon_menu, menu);
    mode.setTitle(mapActivity.getString(R.string.title_edit_zone));
    mode.setSubtitle(mapActivity.getSelectedPolygon().getLabel());
    return true;
  }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    mode.setSubtitle(mapActivity.getSelectedPolygon().getLabel());
    return true;
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    switch (item.getItemId()) {
      case R.id.polygon_action_button:
        Intent intent = new Intent();
        intent.setClass(mapActivity.getApplicationContext(), ActionListActivity.class);
        intent.putExtra(ActionListActivity.POLYGON_ID, mapActivity.getSelectedPolygon().getID());
        mapActivity.startActivityForResult(intent, 0);
        return true;

      case R.id.delete_polygon_button:
        mapActivity.getSelectedPolygon().getPoints().clear();
        applicationStorage.polygons.removePolygon(mapActivity.getSelectedPolygon().getID());
        polygonMap.removePolygon(mapActivity.getSelectedPolygon().getID());
        mode.finish();
        return true;
    }
    return false;
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
    Log.d(TAG, "onDestroyActionMode()");
    if(mapActivity.getSelectedPoint() == null)
      mapActivity.setState(PolygonMapActivity.DrawState.NAVIGATE);
  }
}

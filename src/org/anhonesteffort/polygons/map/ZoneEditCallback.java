package org.anhonesteffort.polygons.map;

import android.content.Intent;
import android.util.Log;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.anhonesteffort.polygons.ActionListActivity;
import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.database.DatabaseHelper;

public class ZoneEditCallback implements ActionMode.Callback {
  private static final String TAG = "org.anhonesteffort.polygons.map.ZoneEditCallback";
  private ZoneMapActivity mapActivity;
  private GoogleZoneMap zoneMap;
  private DatabaseHelper applicationStorage;

  public ZoneEditCallback(ZoneMapActivity mapActivity, GoogleZoneMap zoneMap) {
    this.zoneMap = zoneMap;
    this.mapActivity = mapActivity;
    applicationStorage = DatabaseHelper.getInstance(mapActivity.getApplicationContext());
  }

  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    Log.d(TAG, "onCreateActionMode()");
    MenuInflater inflater = mode.getMenuInflater();
    inflater.inflate(R.menu.edit_polygon_menu, menu);
    mode.setTitle(mapActivity.getString(R.string.title_edit_zone));
    mode.setSubtitle(mapActivity.getSelectedZone().getLabel());
    return true;
  }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    mode.setSubtitle(mapActivity.getSelectedZone().getLabel());
    return true;
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    switch (item.getItemId()) {
      case R.id.polygon_action_button:
        Intent intent = new Intent();
        intent.setClass(mapActivity.getApplicationContext(), ActionListActivity.class);
        intent.putExtra(ActionListActivity.ZONE_ID, mapActivity.getSelectedZone().getId());
        mapActivity.startActivityForResult(intent, 0);
        return true;

      case R.id.delete_zone_button:
        mapActivity.getSelectedZone().getPoints().clear();
        applicationStorage.zoneDb.deleteZone(mapActivity.getSelectedZone().getId());
        zoneMap.removePolygon(mapActivity.getSelectedZone().getId());
        mode.finish();
        return true;
    }
    return false;
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
    Log.d(TAG, "onDestroyActionMode()");
    if(mapActivity.getSelectedPoint() == null)
      mapActivity.setState(ZoneMapActivity.DrawState.NAVIGATE);
  }
}

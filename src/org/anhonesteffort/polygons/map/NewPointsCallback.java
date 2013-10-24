package org.anhonesteffort.polygons.map;

import android.util.Log;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.map.ZoneMapActivity.DrawState;

public class NewPointsCallback implements ActionMode.Callback {
  private static final String TAG = "org.anhonesteffort.polygons.map.NewPointsCallback";
  private ZoneMapActivity mapActivity;

  public NewPointsCallback(ZoneMapActivity mapActivity) {
    this.mapActivity = mapActivity;
  }
  
  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    Log.d(TAG, "onCreateActionMode()");
    MenuInflater inflater = mode.getMenuInflater();
    inflater.inflate(R.menu.new_points_menu, menu);
    mode.setTitle(mapActivity.getString(R.string.title_new_polygon_points));
    mode.setSubtitle(mapActivity.getSelectedZone().getLabel());
    return true;
  }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    return false;
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    return false;
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
    Log.d(TAG, "onDestroyActionMode()");
    mapActivity.updateSelectedZone();
    mapActivity.setState(DrawState.NAVIGATE);
  }
}

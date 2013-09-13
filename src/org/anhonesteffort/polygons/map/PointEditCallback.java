package org.anhonesteffort.polygons.map;

import android.util.Log;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.database.model.PointRecord;
import org.anhonesteffort.polygons.map.ZoneMapActivity.DrawState;

public class PointEditCallback implements ActionMode.Callback {
  private static final String TAG = "org.anhonesteffort.polygons.map.PointEditCallback";
  private ZoneMapActivity mapActivity;
  
  public PointEditCallback(ZoneMapActivity mapActivity) {
    this.mapActivity = mapActivity;
  }
  
  private void removeSelectedPoint() {
    Log.d(TAG, "removeSelectedPoint()");
    PointRecord removePoint = null;

    for(PointRecord point : mapActivity.getSelectedZone().getPoints()) {
      if(point.getId() == mapActivity.getSelectedPoint().getId())
        removePoint = point;
    }
    if(removePoint != null)
      mapActivity.getSelectedZone().getPoints().remove(removePoint);
    
    mapActivity.updateSelectedZone();
    mapActivity.setState(DrawState.EDIT_ZONE);
  }
  
  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    Log.d(TAG, "onCreateActionMode()");
    MenuInflater inflater = mode.getMenuInflater();
    inflater.inflate(R.menu.edit_point_menu, menu);
    mode.setTitle(mapActivity.getString(R.string.title_edit_point));
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
      case R.id.delete_point_button:
        removeSelectedPoint();
        return true;
    }
    return false;
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
    Log.d(TAG, "onDestroyActionMode()");
    if(mapActivity.getState() != DrawState.NEW_POINTS)
      mapActivity.setState(DrawState.NAVIGATE);
  }
}
